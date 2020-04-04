package org.itoapp.strict.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;

import static org.itoapp.strict.service.TracingService.BLUETOOTH_COMPANY_ID;
import static org.itoapp.strict.service.TracingService.BROADCAST_LENGTH;
import static org.itoapp.strict.service.TracingService.HASH_LENGTH;

public class BleScanner {
    private static final String LOG_TAG = "BleScanner";

    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback bluetoothScanCallback;
    private BeaconCache beaconCache;

    public BleScanner(BluetoothAdapter bluetoothAdapter, BeaconCache beaconCache) {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.beaconCache = beaconCache;
    }

    public void startScanning() {
        Log.d(LOG_TAG, "Starting scan");
        bluetoothScanCallback = new ScanCallback() {
            public void onScanResult(int callbackType, ScanResult result) {

                Log.d(LOG_TAG, "onScanResult");

                ScanRecord record = result.getScanRecord();

                // if there is no record, discard this packet
                if (record == null) {
                    return;
                }

                byte[] receivedHash = record.getManufacturerSpecificData(BLUETOOTH_COMPANY_ID);

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

                beaconCache.addReceivedBroadcast(receivedHash, distance);
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
                .setManufacturerData(BLUETOOTH_COMPANY_ID, manufacturerDataMask, manufacturerDataMask)
                .build();

        bluetoothLeScanner.startScan(Collections.singletonList(filter), settingsBuilder.build(), bluetoothScanCallback);
    }

    public void stopScanning() {
        Log.d(LOG_TAG, "Stopping scanning");
        if(bluetoothScanCallback != null) {
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
            bluetoothScanCallback = null;
        }
    }
}
