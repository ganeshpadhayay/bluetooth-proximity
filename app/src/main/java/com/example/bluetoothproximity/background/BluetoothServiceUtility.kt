package com.example.bluetoothproximity.background

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.bluetoothproximity.MyApplication
import com.example.bluetoothproximity.util.Constants
import java.util.*
import java.util.concurrent.TimeUnit

object BluetoothServiceUtility {

    fun getCurrentEpochTimeInSec(): Int {
        val dateObj = Date()
        return (dateObj.time / 1000).toInt()
    }

    fun startBackgroundWorker() {
        val workManager = WorkManager.getInstance(MyApplication.context)
        val workRequest = PeriodicWorkRequest.Builder(BackgroundWorker::class.java, 16, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(BackgroundWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest
        )
    }

    fun isBluetoothAvailable(): Boolean {
        if (isBluetoothPermissionAvailable(MyApplication.context)) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return bluetoothAdapter != null &&
                    bluetoothAdapter.isEnabled &&
                    bluetoothAdapter.state == BluetoothAdapter.STATE_ON
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

    fun getNotificationChannel(): String {
        return Constants.NOTIFICATION_CHANNEL
    }

    fun startService(activity: Activity) {
        if (!BluetoothScanningService.serviceRunning) {
            val uniqueId = "121"
            if (uniqueId.isNotEmpty() && !activity.isFinishing) {
                val intent = Intent(activity, BluetoothScanningService::class.java)
                ContextCompat.startForegroundService(activity, intent)
            }
        }
    }
}