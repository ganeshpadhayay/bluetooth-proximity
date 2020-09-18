package com.example.bluetoothproximity.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.bluetoothproximity.MainActivity;
import com.example.bluetoothproximity.MyApplication;
import com.example.bluetoothproximity.R;
import com.example.bluetoothproximity.beans.BluetoothData;
import com.example.bluetoothproximity.beans.BluetoothModel;
import com.example.bluetoothproximity.gatt.GattServer;
import com.example.bluetoothproximity.util.Constants;
import com.example.bluetoothproximity.util.GTMController;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothScanningService extends Service implements AdaptiveScanHelper.AdaptiveModeListener {

    private RetrieveLocationService retrieveLocationService;

    private BluetoothLeScanner mBluetoothLeScanner;

    private AdaptiveScanHelper mAdaptiveScanHelper;

    private List<String> mData = new ArrayList<>();

    private static final int FIVE_MINUTES = 5 * 60 * 1000;

    private long searchTimestamp;

    private final GattServer mGattServer = new GattServer();

    private static final int NOTIFICATION_ID = 1973;

    private String TAG = "BluetoothScanningService";

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult : Scanning : " + result.getDevice().getName());
            if (BluetoothServiceUtility.INSTANCE.isBluetoothPermissionAvailable(MyApplication.context)) {
                if (result.getDevice() == null || result.getDevice().getName() == null)
                    return;
                String deviceName = result.getDevice().getName();
                clearList();
                mAdaptiveScanHelper.addScanResult(result);
                if (mData.contains(deviceName)) {
                    return;
                }
                String txPower = Constants.EMPTY;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    txPower = String.valueOf(result.getTxPower());
                }
                String txPowerLevel = "";
                if (result.getScanRecord() != null) {
                    txPowerLevel = String.valueOf(result.getScanRecord().getTxPowerLevel());
                }
                BluetoothModel bluetoothModel = new BluetoothModel(result.getDevice().getName(), deviceName, result.getRssi(), txPower, txPowerLevel);
                mData.add(deviceName);
                storeDetectedUserDeviceInDB(bluetoothModel);
                Log.d(TAG, "onScanResult : Information Updated, Device : " + deviceName);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults : Devices : " + results.toString());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed : errorCode : " + errorCode);
        }
    };

    public static boolean serviceRunning;
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = getNotification(Constants.NOTIFICATION_DESC);
        startForeground(NOTIFICATION_ID, notification);
        searchTimestamp = System.currentTimeMillis();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            String channelId = BluetoothServiceUtility.INSTANCE.getNotificationChannel();
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.enableLights(false);
            channel.setSound(null, null);
            channel.setShowBadge(false);

            channel.setDescription(Constants.NOTIFICATION_DESC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    void clearList() {
        long scanPollTime = GTMController.getINSTANCE().getScanPollTime();
        long pollTime = scanPollTime * 1000;
        long difference = System.currentTimeMillis() - searchTimestamp;
        if (difference >= pollTime && !mData.isEmpty()) {
            searchTimestamp = System.currentTimeMillis();
            mData.clear();
        }
    }

    private void advertiseAndScan() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
                                      @Override
                                      public void run() {
                                          if (isBluetoothAvailable()) {
                                              mGattServer.advertise(mAdaptiveScanHelper.getAdvertisementMode());
                                              discover(mAdaptiveScanHelper.getScanMode());
                                          }
                                      }
                                  },
                0,
                FIVE_MINUTES);
        mAdaptiveScanHelper.start();
    }

    private void discover(int scanMode) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        mBluetoothLeScanner = adapter.getBluetoothLeScanner();
        if (mBluetoothLeScanner == null) {
            return;
        }

        ScanSettings.Builder settings = new ScanSettings.Builder().setScanMode(scanMode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settings.setMatchMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setLegacy(false);
            settings.setPhy(BluetoothDevice.PHY_LE_1M);
        }
        try {
            if (isBluetoothAvailable()) {
                mBluetoothLeScanner.startScan(mScanCallback);
            } else {
                Log.e(TAG, "startingScan failed : Bluetooth not available");
            }
        } catch (Exception ex) {
            //Handle Android internal exception for BT adapter not turned ON(Known Android bug)

        }
    }


    /**
     * This method will store the detected device info into the local database to query in future if the need arise
     * to push the data
     *
     * @param bluetoothModel The newly detected device nearby
     */
    void storeDetectedUserDeviceInDB(BluetoothModel bluetoothModel) {
        if (bluetoothModel != null) {
            BluetoothData bluetoothData = new BluetoothData(bluetoothModel.getAddress(), bluetoothModel.getRssi(), bluetoothModel.getTxPower(), bluetoothModel.getTxPowerLevel());
            Location loc = MyApplication.lastKnownLocation;
            if (loc != null) {
                bluetoothData.setLatitude(loc.getLatitude());
                bluetoothData.setLongitude(loc.getLongitude());
            }
            Toast.makeText(this, "Bluetooth Data: " + bluetoothData.toString(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        serviceRunning = true;
        configureNotification();
        mAdaptiveScanHelper = new AdaptiveScanHelper(this);
        mGattServer.onCreate(BluetoothScanningService.this);
        mGattServer.addGattService();
        advertiseAndScan();
        startLocationUpdate();
        registerBluetoothStateListener();
        registerLocationStateListener();
        Log.d(TAG, "onStartCommand service started");
        return START_STICKY;
    }

    private void configureNotification() {
        Notification notification;
        if (!BluetoothServiceUtility.INSTANCE.isLocationOn(MyApplication.context)) {
            notification = getNotification(Constants.PLEASE_ALLOW_LOCATION);
        } else if (!BluetoothServiceUtility.INSTANCE.isBluetoothAvailable()) {
            notification = getNotification(Constants.PLEASE_ALLOW_BLUETOOTH);
        } else {
            notification = getNotification(Constants.NOTIFICATION_DESC);
        }
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification getNotification(String notificationDescText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0, new Intent[]{notificationIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? BluetoothServiceUtility.INSTANCE.getNotificationChannel() : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(getResources().getString(R.string.app_name));
        bigTextStyle.bigText(notificationDescText);
        return notificationBuilder
                .setStyle(bigTextStyle)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(notificationDescText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSound(null)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setSmallIcon(R.drawable.notification_icon)
                .build();
    }

    private void startLocationUpdate() {
        retrieveLocationService = new RetrieveLocationService();
        retrieveLocationService.startService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        serviceRunning = false;
        try {
            if (mBluetoothStatusChangeReceiver != null) {
                unregisterReceiver(mBluetoothStatusChangeReceiver);
            }
            if (mLocationChangeListener != null) {
                unregisterReceiver(mLocationChangeListener);
            }
            stopForeground(true);
            if (retrieveLocationService != null) {
                retrieveLocationService.stopService();
            }
            if (mBluetoothLeScanner != null && isBluetoothAvailable()) {
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
            if (timer != null) {
                timer.cancel();
            }
            mGattServer.onDestroy();
            if (mAdaptiveScanHelper != null) {
                mAdaptiveScanHelper.reset();
            }
        } catch (Exception ex) {
            //As this exception doesn't matter for user,service already destroying,so just logging this on firebase

        }

    }

    public static boolean isBluetoothAvailable() {
        return BluetoothServiceUtility.INSTANCE.isBluetoothAvailable();
    }

    private void registerBluetoothStateListener() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStatusChangeReceiver, filter);
    }

    private void registerLocationStateListener() {
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(mLocationChangeListener, filter);
    }

    private BroadcastReceiver mLocationChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Notification notification;
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                if (!BluetoothServiceUtility.INSTANCE.isLocationOn(MyApplication.context)) {
                    notification = getNotification(Constants.PLEASE_ALLOW_LOCATION);
                    updateNotification(notification);

                } else {
                    if (isBluetoothAvailable()) {
                        notification = getNotification(Constants.NOTIFICATION_DESC);
                        updateNotification(notification);
                    } else {
                        notification = getNotification(Constants.PLEASE_ALLOW_BLUETOOTH);
                        updateNotification(notification);
                    }

                }
            }
        }
    };

    private BroadcastReceiver mBluetoothStatusChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Notification notification;
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mGattServer.stopServer();
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        notification = getNotification(Constants.PLEASE_ALLOW_BLUETOOTH);
                        updateNotification(notification);
                        mAdaptiveScanHelper.stop();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (!BluetoothServiceUtility.INSTANCE.isLocationOn(MyApplication.context)) {
                            notification = getNotification(Constants.PLEASE_ALLOW_LOCATION);
                        } else {
                            notification = getNotification(Constants.NOTIFICATION_DESC);
                        }
                        updateNotification(notification);
                        mGattServer.addGattService();
                        advertiseAndScan();
                        break;
                }
            }
        }
    };

    private void updateNotification(Notification notification) {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG, "onLowMemory");
        super.onLowMemory();
        stopSelf();
        serviceRunning = false;
    }

    @Override
    public void onModeChange(int scanMode, int advertisementMode) {
        try {
            if (isBluetoothAvailable()) {
                if (mBluetoothLeScanner != null) {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
                mGattServer.stopAdvertising();
                discover(scanMode);
                mGattServer.advertise(advertisementMode);
            } else {
                Log.d(TAG, "onModeChange failed due to bluetooth not available");
            }
        } catch (Exception ex) {
            //Handle Android internal exception for BT adapter not turned ON(Known Android bug)
        }
    }
}
