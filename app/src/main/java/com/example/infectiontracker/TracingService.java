package com.example.infectiontracker;

import android.app.Service;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.UUID;

public class TracingService extends Service {
    private Looper serviceLooper;
    private Handler serviceHandler;

    private UUID currentUUID;
    private static final int UUID_VALID_TIME = 1000 * 60 * 60; //ms * sec * min = 1h

    private Runnable regenerateUUID = () -> {
        //TODO store in DB
        currentUUID = UUID.randomUUID();

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


        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanCallback leScanCallback = new ScanCallback() {
            public void onScanResult(int callbackType, ScanResult result) {
                Log.e("BLE", "onScanResult");
                String deviceAddress = result.getDevice().getAddress();
                String deviceRSSI = Integer.toString(result.getRssi());

                //TOOD
                Log.i("BLET")
                devices.put(deviceAddress, deviceRSSI);
                final StringBuilder builder = new StringBuilder();

                for(String address : devices.keySet()) {
                    builder.append(address).append(" ").append(devices.get(address)).append("\n");
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(builder);
                    }
                });
            }
        };
        scanner.startScan(leScanCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
