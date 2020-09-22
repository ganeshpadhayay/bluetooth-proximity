package com.example.bluetoothproximity.background

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.bluetoothproximity.MyApplication
import com.example.bluetoothproximity.util.Constants
import java.util.concurrent.TimeUnit

object BluetoothServiceUtility {

    fun startBackgroundWorker() {
        val workManager = WorkManager.getInstance(MyApplication.context)
        val workRequest = PeriodicWorkRequest.Builder(BluetoothBackgroundWorker::class.java, 16, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(BluetoothBackgroundWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest
        )
    }

    fun isBluetoothAvailable(): Boolean {
        if (isBluetoothPermissionAvailable(MyApplication.context)) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled && bluetoothAdapter.state == BluetoothAdapter.STATE_ON
        }
        return false
    }

    fun isBluetoothPermissionAvailable(context: Context): Boolean {
        val permission1 = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
        val permission2 = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
        return (permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED)
    }


    fun isLocationOn(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var gpsEnabled = false
        var networkEnabled = false
        try {
            gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        } catch (e: Exception) {
        }
        try {
            networkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        } catch (e: Exception) {
        }

        return (gpsEnabled || networkEnabled)
    }

    fun isGPSEnabled(context: Context): Boolean {
        var gpsEnabled = false
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        try {
            gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        } catch (e: Exception) {
        }
        return gpsEnabled
    }

    fun getNotificationChannel(): String {
        return Constants.NOTIFICATION_CHANNEL
    }

    fun startService(activity: Activity) {
        if (!BluetoothScanningService.serviceRunning) {
            if (!activity.isFinishing) {
                val intent = Intent(activity, BluetoothScanningService::class.java)
                ContextCompat.startForegroundService(activity, intent)
            }
        }
    }

    fun requestPermissions(context: Activity, permissionRequestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(context,
                    arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    permissionRequestCode)
        } else {
            ActivityCompat.requestPermissions(context,
                    arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    permissionRequestCode)
        }
    }

    fun arePermissionsGranted(context: Context): Boolean {
        val permission1 = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
        val permission2 = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val gpsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val locPermission = if (coarseLocation == PackageManager.PERMISSION_GRANTED || gpsPermission == PackageManager.PERMISSION_GRANTED) PackageManager.PERMISSION_GRANTED else -1
        if (permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED || locPermission != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

}