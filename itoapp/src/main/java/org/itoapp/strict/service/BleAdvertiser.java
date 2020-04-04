package org.itoapp.strict.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Handler;
import android.util.Log;

import static org.itoapp.strict.service.TracingService.BLUETOOTH_COMPANY_ID;

public class BleAdvertiser {
    private static final String LOG_TAG = "BleAdvertiser";
    private final Handler serviceHandler;

    private byte[] broadcastData;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback bluetoothAdvertiseCallback;

    public BleAdvertiser(BluetoothAdapter bluetoothAdapter, Handler serviceHandler) {
        this.bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        this.serviceHandler = serviceHandler;
    }

    public void setBroadcastData(byte[] broadcastData) {
        this.broadcastData = broadcastData;
        if(bluetoothAdvertiseCallback != null) {
            restartAdvertising();
        }
    }

    private void restartAdvertising() {
        stopAdvertising();
        startAdvertising();
    }

    public void startAdvertising() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
                .build();


        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addManufacturerData(BLUETOOTH_COMPANY_ID, broadcastData)
                .build();

        bluetoothAdvertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.i(LOG_TAG, "Advertising onStartSuccess");

                // when the timeout expires, restart advertising
                if (settingsInEffect.getTimeout() > 0)
                    serviceHandler.postDelayed(() -> restartAdvertising(),
                            settingsInEffect.getTimeout());
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(LOG_TAG, "Advertising onStartFailure: " + errorCode);
                // TODO
            }
        };

        // TODO: check if null when launching with Bluetooth disabled
        bluetoothLeAdvertiser.startAdvertising(settings, data, bluetoothAdvertiseCallback);
    }

    public void stopAdvertising() {
        Log.d(LOG_TAG, "Stopping advertising");
        if (bluetoothAdvertiseCallback != null) {
            bluetoothLeAdvertiser.stopAdvertising(bluetoothAdvertiseCallback);
            bluetoothAdvertiseCallback = null;
        }
    }
}
