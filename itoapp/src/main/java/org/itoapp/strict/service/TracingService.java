package org.itoapp.strict.service;

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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.itoapp.DistanceCallback;
import org.itoapp.PublishUUIDsCallback;
import org.itoapp.TracingServiceInterface;
import org.itoapp.strict.Constants;
import org.itoapp.strict.Helper;
import org.itoapp.strict.Preconditions;
import org.itoapp.strict.database.ItoDBHelper;

import java.security.SecureRandom;

public class TracingService extends Service {
    private static final String LOG_TAG = "TracingService";
    private static final String DEFAULT_NOTIFICATION_CHANNEL = "ContactTracing";
    private static final int NOTIFICATION_ID = 1;
    private SecureRandom uuidGenerator;
    private Looper serviceLooper;
    private Handler serviceHandler;
    private BleScanner bleScanner;
    private BleAdvertiser bleAdvertiser;
    private ContactCache contactCache;
    private ItoDBHelper dbHelper;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, android.content.Intent intent) {
            if (!Preconditions.canScanBluetooth(context) && isBluetoothRunning()) {
                stopBluetooth();
            }
            if(Preconditions.canScanBluetooth(context) && !isBluetoothRunning()) {
                startBluetooth();
            }
        }
    };

    private TracingServiceInterface.Stub binder = new TracingServiceInterface.Stub() {
        @Override
        public void setDistanceCallback(DistanceCallback distanceCallback) {
            contactCache.setDistanceCallback(distanceCallback);
        }

        @Override
        public void publishBeaconUUIDs(long from, long to, PublishUUIDsCallback callback) {
            new PublishBeaconsTask(dbHelper, from, to, callback).execute();
        }

        @Override
        public boolean isPossiblyInfected() {
            //TODO do async
            return dbHelper.selectInfectedContacts().size() > 0;
        }
    };

    private Runnable regenerateUUID = () -> {
        Log.i(LOG_TAG, "Regenerating UUID");

        byte[] uuid = new byte[Constants.UUID_LENGTH];
        uuidGenerator.nextBytes(uuid);
        byte[] hashedUUID = Helper.calculateTruncatedSHA256(uuid);

        dbHelper.insertBeacon(uuid);

        byte[] broadcastData = new byte[Constants.BROADCAST_LENGTH];
        broadcastData[Constants.BROADCAST_LENGTH - 1] = getTransmitPower();
        System.arraycopy(hashedUUID, 0, broadcastData, 0, Constants.HASH_LENGTH);

        bleAdvertiser.setBroadcastData(broadcastData);

        serviceHandler.postDelayed(this.regenerateUUID, Constants.UUID_VALID_INTERVAL);
    };
    //TODO move this to some alarmManager governed section.
    // Also ideally check the server when connected to WIFI and charger
    private Runnable checkServer = () -> {
        new CheckServerTask(dbHelper).execute();
        serviceHandler.postDelayed(this.checkServer, Constants.CHECK_SERVER_INTERVAL);
    };

    private byte getTransmitPower() {
        // TODO look up transmit power for current device
        return (byte) -65;
    }

    private boolean isBluetoothRunning() {
        return bleScanner != null;
    }

    private void stopBluetooth() {
        contactCache.flush();
        bleScanner.stopScanning();
        bleAdvertiser.stopAdvertising();

        serviceHandler.removeCallbacks(regenerateUUID);

        contactCache = null;
        bleScanner = null;
        bleAdvertiser = null;
    }

    private void startBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        contactCache = new ContactCache(dbHelper, serviceHandler);
        bleScanner = new BleScanner(bluetoothAdapter, contactCache);
        bleAdvertiser = new BleAdvertiser(bluetoothAdapter, serviceHandler);

        regenerateUUID.run();
        bleAdvertiser.startAdvertising();
        bleScanner.startScanning();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        uuidGenerator = new SecureRandom();
        dbHelper = new ItoDBHelper(this);
        HandlerThread thread = new HandlerThread("TrackerHandler", Thread.NORM_PRIORITY);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new Handler(serviceLooper);
        serviceHandler.post(this.checkServer);

        if(Preconditions.canScanBluetooth(this)) {
            startBluetooth();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        registerReceiver(broadcastReceiver, filter);
    }

    @TargetApi(26)
    private void createNotificationChannel(NotificationManager notificationManager) {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel mChannel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL, DEFAULT_NOTIFICATION_CHANNEL, importance);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        mChannel.setImportance(NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(mChannel);
    }

    private void runAsForgroundService() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel(notificationManager);

        Intent notificationIntent = new Intent();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this,
                DEFAULT_NOTIFICATION_CHANNEL)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setVibrate(null)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        bleAdvertiser.stopAdvertising();
        bleScanner.stopScanning();
        contactCache.flush();
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
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
        return binder;
    }
}
