package com.example.bluetoothproximity.background

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.bluetoothproximity.MyApplication
import com.google.android.gms.location.*

class RetrieveLocationService {

    companion object {
        private const val UPDATE_INTERVAL: Long = 30 * 60 * 1000  /* 30 min */
        private const val FASTEST_INTERVAL: Long = 5 * 60 * 1000 /* 5 min */
        private const val DISPLACEMENT = 100f //100m
    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val context = MyApplication.context
    private var isServiceRunning = false

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult?.let {
                if (it.lastLocation != null) {
                    MyApplication.setLocation(it.lastLocation)
                }
            }
        }
    }

    fun startService() {
        if (isServiceRunning) {
            return
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        getLocation()
    }

    private fun getLocation() {
        // Create the location request to start receiving updates
        val mLocationRequestHighAccuracy = LocationRequest()
        mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        mLocationRequestHighAccuracy.interval = UPDATE_INTERVAL
        mLocationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL
        mLocationRequestHighAccuracy.smallestDisplacement = DISPLACEMENT

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, locationCallback, Looper.myLooper())
        isServiceRunning = true
    }

    fun stopService() {
        if (isServiceRunning) {
            mFusedLocationClient.removeLocationUpdates(locationCallback).addOnSuccessListener {
                isServiceRunning = false
            }
        }
    }
}