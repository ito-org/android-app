package app.bandemic.strict.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

public class BleScanner {
    private static final String LOG_TAG = "BleScanner";

    private BluetoothAdapter bluetoothAdapter;
    private BeaconCache beaconCache;
    private BluetoothAdapter.LeScanCallback callback;
    private Context context;

    public BleScanner(BluetoothAdapter bluetoothAdapter, BeaconCache beaconCache, Context context) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.beaconCache = beaconCache;
        this.context = context;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void startScanning() {
        Log.d(LOG_TAG, "Starting scan");

        UUID[] uuids = {BandemicProfile.BANDEMIC_SERVICE};
        callback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.i(LOG_TAG, "Found device with rssi " + rssi);
                int txPower = 0; //TODO
                double distance = Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
                BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        Log.i(LOG_TAG, "State changed to " + newState);
                        if (newState == BluetoothGatt.STATE_CONNECTED) {
                            Log.i(LOG_TAG, "Connected, read characterisic ...");
                            gatt.discoverServices();

                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        Log.i(LOG_TAG, "Read characteristic: " + bytesToHex(characteristic.getValue()));
                        beaconCache.addReceivedBroadcast(characteristic.getValue(), distance);
                        gatt.close();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        BluetoothGattCharacteristic characteristic = gatt.getService(BandemicProfile.BANDEMIC_SERVICE).getCharacteristic(BandemicProfile.HASH_OF_UUID);
                        if (characteristic != null) {
                            gatt.readCharacteristic(characteristic);
                        } else {
                            Log.e(LOG_TAG, "Did not find expected characteristic");
                            gatt.close();
                        }
                    }
                };
                device.connectGatt(context, false, gattCallback);
            }
        };
        bluetoothAdapter.startLeScan(uuids, callback);
    }

    public void stopScanning() {
        Log.d(LOG_TAG, "Stopping scanning");

        if (callback != null) {
            bluetoothAdapter.stopLeScan(callback);
        }
    }
}
