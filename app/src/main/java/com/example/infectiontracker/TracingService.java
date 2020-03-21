package com.example.infectiontracker;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.infectiontracker.database.Beacon;
import com.example.infectiontracker.database.OwnUUID;
import com.example.infectiontracker.repository.BroadcastRepository;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import androidx.core.app.NotificationCompat;

public class TracingService extends Service {
    private static final int UUID_VALID_TIME = 1000 * 60 * 60; //ms * sec * min = 1h
    private static final String LOG_TAG = "TracingService";
    private static final int BLUETOOTH_SIG = 2220;
    private static final int BROADCAST_LENGTH = 27;
    private static final String DEFAULT_NOTIFICATION_CHANNEL = "ContactTracing";
    private static final int NOTIFICATION_ID = 1;

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_ERROR = 2;

    private Looper serviceLooper;
    private Handler serviceHandler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private int status = STATUS_DISABLED;

    private UUID currentUUID;
    private byte[] broadcastData;

    private BroadcastRepository mBroadcastRepository;

    private Runnable regenerateUUID = () -> {
        currentUUID = UUID.randomUUID();
        long time = System.currentTimeMillis();
        mBroadcastRepository.insertOwnUUID(new OwnUUID(currentUUID, new Date(time)));

        // Convert the UUID to its SHA-256 hash
        ByteBuffer inputBuffer = ByteBuffer.wrap(new byte[/*Long.BYTES*/ 8 * 2]);
        inputBuffer.putLong(0, currentUUID.getMostSignificantBits());
        inputBuffer.putLong(4, currentUUID.getLeastSignificantBits());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            broadcastData = digest.digest(inputBuffer.array());
            broadcastData = Arrays.copyOf(broadcastData, BROADCAST_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(LOG_TAG, "Algorithm not found", e);
        }

        serviceHandler.postDelayed(this.regenerateUUID, UUID_VALID_TIME);
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcastRepository = new BroadcastRepository(this.getApplication());
        HandlerThread thread = new HandlerThread("TrackerHandler", Thread.NORM_PRIORITY);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new Handler(serviceLooper);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;

        bluetoothAdapter = bluetoothManager.getAdapter();
        if(!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.w(LOG_TAG, "BLE not supported!");
            // TODO handle LE not available in UI
            status = STATUS_ERROR;
        }

        if(!bluetoothAdapter.isEnabled()) {
            Log.i(LOG_TAG, "Bluetooth is disabled");
            // TODO is it possible to restart the service as soon as Bluetooth gets enabled?
            status = STATUS_ERROR;
            return;
        }

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        regenerateUUID.run();
        advertise();
        scan();
    }

    private void scan() {
        ScanCallback leScanCallback = new ScanCallback() {
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord record = result.getScanRecord();

                // if there is no record, discard this packet
                if (record == null) {
                    return;
                }

                byte[] receivedHash = record.getManufacturerSpecificData(BLUETOOTH_SIG);

                // if there is no data, discard
                if (receivedHash == null) {
                    return;
                }

                int deviceRSSI = result.getRssi();

                Log.i(LOG_TAG, "onScanResult");
                Log.d(LOG_TAG, Arrays.toString(receivedHash) + ":" + deviceRSSI);
                mBroadcastRepository.insertBeacon(new Beacon(
                        receivedHash,
                        currentUUID,
                        new Date(System.currentTimeMillis()),
                        //TODO calculate Risk
                        0
                ));
            }
        };
        bluetoothLeScanner.startScan(leScanCallback);
    }

    private void advertise() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(180000)
                .build();


        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addManufacturerData(BLUETOOTH_SIG, broadcastData)
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.i("BLE", "Advertising onStartSuccess");

                // when the timeout expires, restart advertising
                serviceHandler.postDelayed(()-> {
                    bluetoothLeAdvertiser.stopAdvertising(this);
                    serviceHandler.post(TracingService.this::advertise);
                }, settingsInEffect.getTimeout());
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e("BLE", "Advertising onStartFailure: " + errorCode);

                // TODO
            }
        };
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertisingCallback);
    }

    @TargetApi(26)
    private void createChannel(NotificationManager notificationManager) {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel mChannel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL, DEFAULT_NOTIFICATION_CHANNEL, importance);
        mChannel.setDescription(getText(R.string.notification_channel).toString());
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
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
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        runAsForgroundService();
        return START_STICKY;
    }

    /*
    Don't do anything here, because the service doesn't have to communicate to other apps
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
