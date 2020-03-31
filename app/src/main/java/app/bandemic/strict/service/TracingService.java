package app.bandemic.strict.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import app.bandemic.R;
import app.bandemic.strict.database.OwnUUID;
import app.bandemic.strict.repository.BroadcastRepository;
import app.bandemic.ui.MainActivity;

public class TracingService extends Service {
    private static final String LOG_TAG = "TracingService";
    private static final String DEFAULT_NOTIFICATION_CHANNEL = "ContactTracing";
    private static final int NOTIFICATION_ID = 1;

    public static final int BLUETOOTH_SIG = 2220;
    public static final int HASH_LENGTH = 26;
    public static final int BROADCAST_LENGTH = HASH_LENGTH + 1;
    private static final int UUID_VALID_TIME = 1000 * 60 * 30; //ms * sec * 30 min

    private Looper serviceLooper;
    private Handler serviceHandler;
    private BleScanner bleScanner;
    private BleAdvertiser bleAdvertiser;
    private BeaconCache beaconCache;

    private UUID currentUUID;

    private BroadcastRepository broadcastRepository;

    private final IBinder mBinder = new TracingServiceBinder();

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_STARTING = 1;
    public static final int STATUS_BLUETOOTH_NOT_ENABLED = 2;
    public static final int STATUS_LOCATION_NOT_ENABLED = 3;

    private int serviceStatus = STATUS_STARTING;

    public class TracingServiceBinder extends Binder {
        public int getServiceStatus() {
            return serviceStatus;
        }

        public void addServiceStatusListener(ServiceStatusListener listener) {
            serviceStatusListeners.add(listener);
        }

        public void removeServiceStatusListener(ServiceStatusListener listener) {
            serviceStatusListeners.remove(listener);
        }

        public double[] getNearbyDevices() {
            return beaconCache.getNearbyDevices();
        }

        public void addNearbyDevicesListener(BeaconCache.NearbyDevicesListener listener) {
            beaconCache.nearbyDevicesListeners.add(listener);
        }

        public void removeNearbyDevicesListener(BeaconCache.NearbyDevicesListener listener) {
            beaconCache.nearbyDevicesListeners.remove(listener);
        }
    }

    List<ServiceStatusListener> serviceStatusListeners = new ArrayList<>();
    public interface ServiceStatusListener {
        void serviceStatusChanged(int serviceStatus);
    }

    private void setServiceStatus(int serviceStatus) {
        this.serviceStatus = serviceStatus;
        for (ServiceStatusListener listener : serviceStatusListeners) {
            listener.serviceStatusChanged(serviceStatus);
        }
    }

    private Runnable regenerateUUID = () -> {
        Log.i(LOG_TAG, "Regenerating UUID");

        currentUUID = UUID.randomUUID();
        long time = System.currentTimeMillis();
        broadcastRepository.insertOwnUUID(new OwnUUID(currentUUID, new Date(time)));

        // Convert the UUID to its SHA-256 hash
        ByteBuffer inputBuffer = ByteBuffer.wrap(new byte[/*Long.BYTES*/ 8 * 2]);
        inputBuffer.putLong(0, currentUUID.getMostSignificantBits());
        inputBuffer.putLong(4, currentUUID.getLeastSignificantBits());

        byte[] broadcastData;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            broadcastData = digest.digest(inputBuffer.array());
            broadcastData = Arrays.copyOf(broadcastData, BROADCAST_LENGTH);
            broadcastData[HASH_LENGTH] = getTransmitPower();
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(LOG_TAG, "Algorithm not found", e);
            throw new RuntimeException(e);
        }

        bleAdvertiser.setBroadcastData(broadcastData);

        serviceHandler.postDelayed(this.regenerateUUID, UUID_VALID_TIME);
    };

    private byte getTransmitPower() {
        // TODO look up transmit power for current device
        return (byte) -65;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        broadcastRepository = new BroadcastRepository(this.getApplication());
        HandlerThread thread = new HandlerThread("TrackerHandler", Thread.NORM_PRIORITY);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new Handler(serviceLooper);
        beaconCache = new BeaconCache(broadcastRepository, serviceHandler);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        registerReceiver(stateReceiver, filter);
    }

    @TargetApi(26)
    private void createChannel(NotificationManager notificationManager) {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel mChannel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL, DEFAULT_NOTIFICATION_CHANNEL, importance);
        mChannel.setDescription(getText(R.string.notification_channel).toString());
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        mChannel.setImportance(NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(mChannel);
    }

    private void runAsForgroundService() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(notificationManager);

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this,
                DEFAULT_NOTIFICATION_CHANNEL)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setVibrate(null)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    void tryStartingBluetooth() {
        Log.i(LOG_TAG, "Try starting bluetooth advertisement + scanning");
        if (serviceStatus == STATUS_RUNNING) {
            Log.i(LOG_TAG, "Bluetooth is already running");
            return;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Log.i(LOG_TAG, "Bluetooth not enabled");
            setServiceStatus(STATUS_BLUETOOTH_NOT_ENABLED);
            return;
        }

        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //I have read this is not actually required on all devices, but I have not found a way
            //to check if it is required.
            //If location is not enabled the BLE scan fails silently (scan callback is never called)
            if (!locationManager.isLocationEnabled()) {
                Log.i(LOG_TAG, "Location not enabled (API>=P check)");
                setServiceStatus(STATUS_LOCATION_NOT_ENABLED);
                return;
            }
        } else {
            //Not sure if this is the correct check, gps is not really required, but passive provider
            //does not seem to be enough
            if (!locationManager.getProviders(true).contains(LocationManager.GPS_PROVIDER)) {
                Log.i(LOG_TAG, "Location not enabled (API<P check)");
                setServiceStatus(STATUS_LOCATION_NOT_ENABLED);
                return;
            }
        }




        bleScanner = new BleScanner(bluetoothAdapter, beaconCache, this);
        bleAdvertiser = new BleAdvertiser(bluetoothManager, this);

        regenerateUUID.run();
        bleAdvertiser.startAdvertising();
        bleScanner.startScanning();
        setServiceStatus(STATUS_RUNNING);
    }

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (bluetoothState == BluetoothAdapter.STATE_ON) {
                    tryStartingBluetooth();
                }
            }

            if (action.equals(LocationManager.MODE_CHANGED_ACTION)) {
                tryStartingBluetooth();
            }
        }
    };

    @Override
    public void onDestroy() {
        if (bleAdvertiser != null) {
            bleAdvertiser.stopAdvertising();
        }
        if (bleScanner != null) {
            bleScanner.stopScanning();
        }
        beaconCache.flush();
        unregisterReceiver(stateReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        runAsForgroundService();
        tryStartingBluetooth();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
