package com.example.bluetoothproximity.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.bluetoothproximity.background.BluetoothServiceUtility;
import com.example.bluetoothproximity.util.Constants;
import com.example.bluetoothproximity.util.SharedPreferenceHelper;

import java.util.UUID;

public class GattServer {
    private String TAG = "BluetoothScanningService";
    private Context mContext;

    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothManager mBluetoothManager;

    private AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
        }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //do nothing
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //do nothing
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (UUID.fromString(Constants.CORONA_SEVERITY_LEVEL_UUID).equals(characteristic.getUuid())) {
                //here we would send the corona severity level of the user
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
            } else {
                // Invalid characteristic, send null
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }
    };


    public void onCreate(Context context) throws RuntimeException {
        mContext = context;
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public void advertise(int advertisementMode) {
        try {
            BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
            if (defaultAdapter == null) {
                return;
            }
            SharedPreferences sharedPref = mContext.getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
            String uniqueId = sharedPref.getString(Constants.BLUETOOTH_NAME_UNIQUE_ID, null);
            if (uniqueId == null || !uniqueId.equalsIgnoreCase(defaultAdapter.getName())) {
                stopAdvertising();
            }
            defaultAdapter.setName(uniqueId);

            advertiser = defaultAdapter.getBluetoothLeAdvertiser();

            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(advertisementMode)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                    .setConnectable(true);

            ParcelUuid pUuid = new ParcelUuid(UUID.fromString(Constants.SERVICE_UUID));

            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .addServiceUuid(pUuid)
                    .setIncludeTxPowerLevel(false).build();

            if (advertiser != null) {
                try {
                    startAdvertising(settingsBuilder, data, true);
                } catch (Exception e) {
                    // Adding common exception just to retry this if anything goes wrong in the first time
                    // (Chinese devices facing some legacy data issue)
                    //Some OEM shows Advertising data too large exception,so not sending txPowerLevel
                    startAdvertising(settingsBuilder, data, false);
                }
            }
        } catch (Exception ex) {
            //Reporting exception on Crashlytics if advertisement fails for other reason in devices and take corrective actions
        }
    }

    private void startAdvertising(AdvertiseSettings.Builder settingsBuilder, AdvertiseData data, boolean isConnectable) {
        settingsBuilder.setConnectable(isConnectable);
        if (BluetoothServiceUtility.INSTANCE.isBluetoothAvailable() && advertiser != null && advertisingCallback != null) {
            advertiser.startAdvertising(settingsBuilder.build(), data, advertisingCallback);
        }
    }

    /***
     * adds a GATT service to our server
     */
    public void addGattService() {
        if (BluetoothServiceUtility.INSTANCE.isBluetoothAvailable() && isServerStarted()) {
            try {
                mBluetoothGattServer.addService(createGattService());
            } catch (Exception ex) {
                //Android version 7.0 (Redmi Note 4 & Huawei MediaPad T3 & Nova2Plus device issue) Android BLE characterstic add issue  https://github.com/iDevicesInc/SweetBlue/issues/394
            }
        }
    }

    /***
     * We will be creating a service which will have some Characteristics which will have the data bytes that are to be
     * transmitted
     * @return a service
     */
    private BluetoothGattService createGattService() {
        SharedPreferences sharedPreferences = SharedPreferenceHelper.INSTANCE.getSharedPreferenceHelper();

        //create a service
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(Constants.SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //create  characteristics
        BluetoothGattCharacteristic coronaSeverityLevelCharacteristics = new BluetoothGattCharacteristic(UUID.fromString(Constants.CORONA_SEVERITY_LEVEL_UUID), BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        String coronaSeverityLevel = sharedPreferences.getString(Constants.CORONA_SEVERITY_LEVEL, null);
        String uniqueUserId = sharedPreferences.getString(Constants.BLUETOOTH_NAME_UNIQUE_ID, null);
        coronaSeverityLevelCharacteristics.setValue(uniqueUserId + "&" + coronaSeverityLevel);

        //add characteristic to the service
        service.addCharacteristic(coronaSeverityLevelCharacteristics);

        return service;
    }

    public void onDestroy() {
        Log.d(TAG, "in onDestroy of GattServer");
        if (mContext != null) {
            if (BluetoothServiceUtility.INSTANCE.isBluetoothAvailable()) {
                stopServer();
                stopAdvertising();
            }
        }
    }

    public void stopServer() {
        try {
            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.clearServices();
                mBluetoothGattServer.close();
            }
        } catch (Exception e) {
            //Handle Bluetooth Gatt close internal bug
            Log.e(TAG, "GATT server can't be closed elegantly" + e.getMessage());
        }
    }

    private boolean isServerStarted() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.clearServices();
            return true;
        } else {
            return false;
        }
    }

    public void stopAdvertising() {
        try {
            if (advertiser != null) {
                advertiser.stopAdvertising(advertisingCallback);
            }
        } catch (Exception ex) {
            //Handle StopAdvertisingSet Android Internal bug (Redmi Note 7 Pro Android 9)
        }
    }
}
