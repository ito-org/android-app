package org.itoapp.strict.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.itoapp.DistanceCallback;
import org.itoapp.PublishUUIDsCallback;
import org.itoapp.TracingServiceInterface;
import org.itoapp.strict.Helper;
import org.itoapp.strict.database.ItoDBHelper;
import org.itoapp.strict.network.NetworkHelper;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

public class TracingService extends Service {
    public static final int BLUETOOTH_COMPANY_ID = 65535; // TODO get a real company ID!
    public static final int UUID_LENGTH = 16;
    public static final int HASH_LENGTH = 26;
    public static final int BROADCAST_LENGTH = HASH_LENGTH + 1;
    private static final String LOG_TAG = "TracingService";
    private static final String DEFAULT_NOTIFICATION_CHANNEL = "ContactTracing";
    private static final int NOTIFICATION_ID = 1;
    private static final int UUID_VALID_TIME = 1000 * 60 * 30; //ms * sec * 30 min
    private static final int CHECK_SERVER_TIME = 1000 * 60 * 5; //ms * sec * 5 min
    private SecureRandom uuidGenerator;
    private Looper serviceLooper;
    private Handler serviceHandler;
    private BleScanner bleScanner;
    private BleAdvertiser bleAdvertiser;
    private ContactCache contactCache;
    private ItoDBHelper dbHelper;
    private TracingServiceInterface.Stub binder = new TracingServiceInterface.Stub() {
        @Override
        public void setDistanceCallback(DistanceCallback distanceCallback) {
            contactCache.setDistanceCallback(distanceCallback);
        }

        @Override
        public void publishBeaconUUIDs(long from, long to, PublishUUIDsCallback callback) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        NetworkHelper.publishUUIDs(dbHelper.selectBeacons(from, to));
                        try {
                            callback.onSuccess();
                        } catch (RemoteException e) {
                            Log.e(LOG_TAG, "._.", e);
                        }
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Could not publish UUIDs!", e);
                        try {
                            callback.onFailure();
                        } catch (RemoteException ex) {
                            Log.e(LOG_TAG, "._.", e);
                        }
                    }
                    return null;
                }
            }.execute();
        }

        @Override
        public boolean isPossiblyInfected() {
            //TODO do async
            return dbHelper.selectInfectedContacts().size() > 0;
        }
    };
    private Runnable regenerateUUID = () -> {
        Log.i(LOG_TAG, "Regenerating UUID");

        byte[] uuid = new byte[UUID_LENGTH];
        uuidGenerator.nextBytes(uuid);
        byte[] hashedUUID = Helper.calculateTruncatedSHA256(uuid);

        dbHelper.insertBeacon(uuid);

        byte[] broadcastData = new byte[BROADCAST_LENGTH];
        broadcastData[BROADCAST_LENGTH - 1] = getTransmitPower();
        System.arraycopy(hashedUUID, 0, broadcastData, 0, HASH_LENGTH);

        bleAdvertiser.setBroadcastData(broadcastData);

        serviceHandler.postDelayed(this.regenerateUUID, UUID_VALID_TIME);
    };
    //TODO move this to some alarmManager governed section.
    // Also ideally check the server when connected to WIFI and charger
    private Runnable checkServer = () -> {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                NetworkHelper.refreshInfectedUUIDs(dbHelper);
                List<ItoDBHelper.ContactResult> contactResults = dbHelper.selectInfectedContacts();
                if (!contactResults.isEmpty()) {
                    Log.w(LOG_TAG, "Possibly encountered UUIDs: " + contactResults.size());
                }
                serviceHandler.postDelayed(checkServer, CHECK_SERVER_TIME);
                return null;
            }
        }.execute();
    };

    private byte getTransmitPower() {
        // TODO look up transmit power for current device
        return (byte) -65;
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

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        contactCache = new ContactCache(dbHelper, serviceHandler);
        bleScanner = new BleScanner(bluetoothAdapter, contactCache);
        bleAdvertiser = new BleAdvertiser(bluetoothAdapter, serviceHandler);

        regenerateUUID.run();
        bleAdvertiser.startAdvertising();
        bleScanner.startScanning();
        serviceHandler.post(this.checkServer);
    }

    @TargetApi(26)
    private void createChannel(NotificationManager notificationManager) {
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
            createChannel(notificationManager);

        //TODO
        Class<?> activityClass;
        try {
            activityClass = Class.forName("app.bandemic.ui.MainActivity");
        } catch (ClassNotFoundException e) {
            try {
                activityClass = Class.forName("com.reactnativeapp.MainActivity");
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                return;
            }
        }
        Intent notificationIntent = new Intent(this, activityClass);

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
