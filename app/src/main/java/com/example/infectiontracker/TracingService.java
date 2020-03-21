package com.example.infectiontracker;

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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class TracingService extends Service {
    private static final int UUID_VALID_TIME = 1000 * 60 * 60; //ms * sec * min = 1h
    private static final String LOG_TAG = "TracingService";

    private Looper serviceLooper;
    private Handler serviceHandler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;

    private ByteBuffer inputBuffer = ByteBuffer.wrap(new byte[/*Long.BYTES*/ 8 * 3]);
    private UUID currentUUID;
    private byte[] broadcastData;

    private Runnable regenerateUUID = () -> {
        //TODO store in DB
        currentUUID = UUID.randomUUID();
        long time = System.currentTimeMillis();

        //TODO test
        inputBuffer.putLong(0, currentUUID.getMostSignificantBits());
        inputBuffer.putLong(4, currentUUID.getLeastSignificantBits());
        inputBuffer.putLong(8, time);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-224");
            broadcastData = digest.digest(inputBuffer.array());
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(LOG_TAG, "Algorithm not found", e);
        }

        serviceHandler.postDelayed(this.regenerateUUID, UUID_VALID_TIME);
    };

    /*
    Don't do anything here, because the service doesn't have to communicate to other apps
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("TrackerHandler", Thread.NORM_PRIORITY);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new Handler(serviceLooper);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;

        bluetoothAdapter = bluetoothManager.getAdapter();
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
                ByteBuffer transferredData = ByteBuffer.wrap(new byte[29]);
                try {
                    Map<ParcelUuid, byte[]> serviceData = record.getServiceData();
                    ParcelUuid key = serviceData.keySet().iterator().next();
                    UUID uuid = key.getUuid();
                    transferredData.putLong(uuid.getMostSignificantBits());
                    transferredData.putLong(uuid.getLeastSignificantBits());
                    transferredData.put(serviceData.get(key));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "unsupported");
                    return;
                }

                Log.e(LOG_TAG, Arrays.toString(transferredData.array()));

                int deviceRSSI = result.getRssi();

                //TODO store
                Log.i(LOG_TAG, "onScanResult");
                //Log.d(LOG_TAG, Arrays.toString(receivedID));
                Log.d(LOG_TAG, "" + deviceRSSI);
            }
        };
        bluetoothLeScanner.startScan(leScanCallback);
    }

    private void advertise() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(false)
                .build();

        Log.e(LOG_TAG, Arrays.toString(this.broadcastData));
        ByteBuffer broadcastData = ByteBuffer.allocate(29);
        broadcastData.put((byte) 42);
        broadcastData.put(this.broadcastData);
        broadcastData.flip();
        UUID serviceUUID = new UUID(broadcastData.getLong(), broadcastData.getLong());
        ParcelUuid parcelUuid = new ParcelUuid(serviceUUID);
        byte[] serviceData = new byte[13];
        broadcastData.get(serviceData);

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addServiceData(parcelUuid, serviceData)
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.i("BLE", "Advertising onStartSuccess");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e("BLE", "Advertising onStartFailure: " + errorCode);
            }
        };
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertisingCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
