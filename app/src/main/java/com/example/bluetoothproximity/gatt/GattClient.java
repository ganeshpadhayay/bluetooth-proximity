package com.example.bluetoothproximity.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.example.bluetoothproximity.MyApplication;
import com.example.bluetoothproximity.beans.BluetoothModel;
import com.example.bluetoothproximity.util.Constants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.content.Context.BLUETOOTH_SERVICE;

public class GattClient {
    private String TAG = "BluetoothScanningService";

    private BluetoothGatt mBluetoothGatt;

    //the client device from where we would get the data about the client
    private BluetoothDevice mDevice;
    //parameter of the client device
    private String txPower = "";
    private int rssi;
    private String txPowerLevel = "";
    private String uniqueId = "";
    //list of characteristics we would be receiving from the client
    private List<BluetoothGattCharacteristic> chars = new ArrayList<>();

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                stopClient();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(Constants.SERVICE_UUID));
                if (service != null) {
                    BluetoothGattCharacteristic coronaSeverityCharacteristics = service.getCharacteristic(UUID.fromString(Constants.CORONA_SEVERITY_LEVEL_UUID));
                    if (coronaSeverityCharacteristics != null) {
                        chars.add(coronaSeverityCharacteristics);
                    }
                }
                requestCharacteristics(gatt);
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        public void requestCharacteristics(BluetoothGatt gatt) {
            if (!chars.isEmpty()) {
                gatt.readCharacteristic(chars.get(chars.size() - 1));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (UUID.fromString(Constants.CORONA_SEVERITY_LEVEL_UUID).equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                String coronaLevel = new String(data, StandardCharsets.UTF_8);
                BluetoothModel bluetoothModel = new BluetoothModel(uniqueId, coronaLevel, rssi, txPower, txPowerLevel, null, null);
                storeDetectedUserDeviceInDB(bluetoothModel);
            }
            chars.remove(chars.get(chars.size() - 1));
            if (chars.size() > 0) {
                requestCharacteristics(gatt);
            } else {
                gatt.disconnect();
            }
        }
    };

    private void storeDetectedUserDeviceInDB(BluetoothModel bluetoothModel) {
        Location loc = MyApplication.lastKnownLocation;
        if (loc != null) {
            bluetoothModel.setLongitude(loc.getLongitude());
            bluetoothModel.setLattitude(loc.getLatitude());
        }
        Log.d(TAG, "Bluetooth Data in Gatt Client : " + bluetoothModel.toString());
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startClient();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopClient();
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
    };

    public void onCreate(Context context, ScanResult result) throws RuntimeException {
        mContext = context;
        rssi = result.getRssi();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            txPower = String.valueOf(result.getTxPower());
        }
        if (result.getScanRecord() != null) {
            txPowerLevel = String.valueOf(result.getScanRecord().getTxPowerLevel());
        }
        if (result.getDevice() != null && result.getDevice().getName() != null) {
            uniqueId = result.getDevice().getName();
        }
        mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            // Register for system Bluetooth events
            registerReceiver();
            configureClient(result);
        }
    }

    private void configureClient(ScanResult result) {
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        } else {
            mDevice = mBluetoothAdapter.getRemoteDevice(result.getDevice().getAddress());
            startClient();
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothReceiver, filter);
    }

    private void startClient() {
        if (mDevice != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "in startClient of GattClient, connecting to gatt server");
                mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback, TRANSPORT_LE);
            } else {
                Log.d(TAG, "in startClient of GattClient, connecting to gatt server");
                mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
            }
        }
    }

    private void stopClient() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public void onDestroy() {
        if (mContext != null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter;
            if (mBluetoothManager != null) {
                bluetoothAdapter = mBluetoothManager.getAdapter();
                if (bluetoothAdapter.isEnabled()) {
                    stopClient();
                }
            }
        }
    }
}
