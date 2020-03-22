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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.infectiontracker.database.Beacon;
import com.example.infectiontracker.database.OwnUUID;
import com.example.infectiontracker.repository.BroadcastRepository;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

public class TracingService extends Service {
    private static final int UUID_VALID_TIME = 1000 * 60 * 60; //ms * sec * min = 1h
    private static final String LOG_TAG = "TracingService";
    private static final int BLUETOOTH_SIG = 2220;
    private static final int HASH_LENGTH = 26;
    private static final int BROADCAST_LENGTH = HASH_LENGTH + 1;
    private static final String DEFAULT_NOTIFICATION_CHANNEL = "ContactTracing";
    private static final int NOTIFICATION_ID = 1;

    private Looper serviceLooper;
    private Handler serviceHandler;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;

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
            broadcastData[HASH_LENGTH] = getTransmitPower();
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(LOG_TAG, "Algorithm not found", e);
        }

        serviceHandler.postDelayed(this.regenerateUUID, UUID_VALID_TIME);
    };

    private byte getTransmitPower() {
        // TODO look up transmit power for current device
        return (byte) -65;
    }

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
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        regenerateUUID.run();
        advertise();
        scan();
    }

    private void scan() {
        ScanCallback leScanCallback = new ScanCallback() {
            public void onScanResult(int callbackType, ScanResult result) {

                Log.d(LOG_TAG, "onScanResult");

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

                byte txPower = receivedHash[HASH_LENGTH];
                receivedHash = Arrays.copyOf(receivedHash, HASH_LENGTH);

                int rssi = result.getRssi();

                // TODO take antenna attenuation into account
                double distance = Math.pow(10d, ((double) txPower - rssi) / (10 * 2));

                Log.d(LOG_TAG, Arrays.toString(receivedHash) + ":" + distance);
                mBroadcastRepository.insertBeacon(new Beacon(
                        receivedHash,
                        currentUUID,
                        new Date(System.currentTimeMillis()),
                        distance
                ));
            }
        };

        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settingsBuilder.setLegacy(true);
        }

        byte[] manufacturerDataMask = new byte[BROADCAST_LENGTH];

        ScanFilter filter = new ScanFilter.Builder()
                .setManufacturerData(BLUETOOTH_SIG, manufacturerDataMask, manufacturerDataMask)
                .build();

        bluetoothLeScanner.startScan(Collections.singletonList(filter), settingsBuilder.build(), leScanCallback);
    }

    private void advertise() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
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
                Log.i(LOG_TAG, "Advertising onStartSuccess");

                // when the timeout expires, restart advertising
                if (settingsInEffect.getTimeout() > 0)
                    serviceHandler.postDelayed(() -> {
                        bluetoothLeAdvertiser.stopAdvertising(this);
                        serviceHandler.post(TracingService.this::advertise);
                    }, settingsInEffect.getTimeout());
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(LOG_TAG, "Advertising onStartFailure: " + errorCode);
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
