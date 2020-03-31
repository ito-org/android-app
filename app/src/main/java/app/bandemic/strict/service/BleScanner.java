package app.bandemic.strict.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;
import android.util.LruCache;

import java.util.Collections;

public class BleScanner {
    private static final String LOG_TAG = "BleScanner";
    private final BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    private final BeaconCache beaconCache;
    private final Context context;

    private ScanCallback bluetoothScanCallback;

    // Cache the hash of uuid returned by the device so we don't have to connect again every time
    private LruCache<String, byte[]> macAddressCache = new LruCache<>(100);

    // Remember start time of connections so we don't start multiple connections per device
    private LruCache<String, Long> connStartedTimeMap = new LruCache<>(100);

    public BleScanner(BluetoothAdapter bluetoothAdapter, BeaconCache beaconCache, Context context) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.beaconCache = beaconCache;
        this.context = context;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
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

        bluetoothScanCallback = new ScanCallback() {
            public void onScanResult(int callbackType, ScanResult result) {

                Log.d(LOG_TAG, "onScanResult");

                ScanRecord record = result.getScanRecord();

                // if there is no record, discard this packet
                if (record == null) {
                    return;
                }

                //TODO The values here seem wrong
                int txPower = -65;//record.getTxPowerLevel();
                int rssi = result.getRssi();

                BluetoothDevice device = result.getDevice();

                String deviceAddress = device.getAddress();

                Log.i(LOG_TAG, "Found device with rssi=" + rssi + " txPower="+txPower);


                double distance = Math.pow(10d, ((double) txPower - rssi) / (10 * 2));

                //Cache UUID results for mac addresses we have already connected to
                // so we don't have to build up connections again.
                //Only send updated distance to BeaconCache
                byte[] hashOfUUIDCached = macAddressCache.get(deviceAddress);
                if (hashOfUUIDCached != null) {
                    Log.i(LOG_TAG, "Address seen already: " + deviceAddress + " New distance: " + distance);
                    beaconCache.addReceivedBroadcast(hashOfUUIDCached, distance);
                } else {

                    //Only start connection for the same device max every 5 sec so that we don't
                    // have multiple connections for the same device running
                    Long connStartedTime = connStartedTimeMap.get(deviceAddress);
                    if (connStartedTime == null || (SystemClock.elapsedRealtime() - connStartedTime) > 5000) {
                        connStartedTimeMap.put(deviceAddress, SystemClock.elapsedRealtime());

                        Log.i(LOG_TAG, "Address not seen yet: " + deviceAddress + " Distance: " + distance);
                        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
                            @Override
                            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                Log.i(LOG_TAG, "State changed to " + newState);
                                if (newState == BluetoothGatt.STATE_CONNECTED) {
                                    gatt.discoverServices();

                                }
                            }

                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                Log.i(LOG_TAG, "Characteristic read " + characteristic.getUuid());
                                if (characteristic.getUuid().compareTo(BandemicProfile.HASH_OF_UUID) == 0) {
                                    byte[] hashOfUUID = characteristic.getValue();
                                    Log.i(LOG_TAG, "Read hash of uuid characteristic: " + bytesToHex(hashOfUUID));
                                    macAddressCache.put(deviceAddress, hashOfUUID);
                                    beaconCache.addReceivedBroadcast(hashOfUUID, distance);
                                    gatt.close();
                                }
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                Log.i(LOG_TAG, "Services Discovered");
                                BluetoothGattCharacteristic characteristic = gatt.getService(BandemicProfile.BANDEMIC_SERVICE).getCharacteristic(BandemicProfile.HASH_OF_UUID);
                                if (characteristic != null) {
                                    Log.i(LOG_TAG, "Read characteristic...");
                                    gatt.readCharacteristic(characteristic);
                                } else {
                                    Log.i(LOG_TAG, "=============================");
                                    Log.e(LOG_TAG, "Did not find expected characteristic");
                                    Log.i(LOG_TAG, "Found these instead:");
                                    for (BluetoothGattService service : gatt.getServices()) {
                                        Log.i(LOG_TAG, "Service: " + service.getUuid());
                                        for (BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics()) {
                                            Log.i(LOG_TAG, "    Characteristic: " + gattCharacteristic.getUuid());
                                        }
                                    }
                                    Log.i(LOG_TAG, "=============================");
                                    gatt.close();
                                }
                            }
                        };
                        device.connectGatt(context, false, gattCallback);
                    }
                }

            }
        };

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(BandemicProfile.BANDEMIC_SERVICE))
                .build();

        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(0);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settingsBuilder.setLegacy(true);
        }

        bluetoothLeScanner.startScan(Collections.singletonList(filter), settingsBuilder.build(), bluetoothScanCallback);
    }

    public void stopScanning() {
        Log.d(LOG_TAG, "Stopping scanning");

        if (bluetoothScanCallback != null && bluetoothAdapter.isEnabled()) {
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
        }

    }
}
