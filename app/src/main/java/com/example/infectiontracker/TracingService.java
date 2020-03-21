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
import android.util.Log;

import com.example.infectiontracker.database.OwnUUID;
import com.example.infectiontracker.repository.BroadcastRepository;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class TracingService extends Service {
    private static final int UUID_VALID_TIME = 1000 * 60 * 60; //ms * sec * min = 1h
    private static final String LOG_TAG = "TracingService";
    private static final int BLUETOOTH_SIG = 2220;
    private static final int BROADCAST_LENGTH = 27;

    private Looper serviceLooper;
    private Handler serviceHandler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;

    private UUID currentUUID;
    private byte[] broadcastData;

    private BroadcastRepository mBroadcastRepository;

    private Runnable regenerateUUID = () -> {
        currentUUID = UUID.randomUUID();
        long time = System.currentTimeMillis();
        mBroadcastRepository.insert(new OwnUUID(currentUUID, new Date(time)));

        // Put the UUID and the current time together into one buffer and
        ByteBuffer inputBuffer = ByteBuffer.wrap(new byte[/*Long.BYTES*/ 8 * 3]);
        inputBuffer.putLong(0, currentUUID.getMostSignificantBits());
        inputBuffer.putLong(4, currentUUID.getLeastSignificantBits());
        inputBuffer.putLong(8, time);

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

                //TODO store
                Log.i(LOG_TAG, "onScanResult");
                Log.d(LOG_TAG, Arrays.toString(receivedHash) + ":" + deviceRSSI);
            }
        };
        bluetoothLeScanner.startScan(leScanCallback);
    }

    private void advertise() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
