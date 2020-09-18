package com.example.bluetoothproximity

import android.app.Application
import android.location.Location

class MyApplication : Application() {

    companion object {
        lateinit var context: MyApplication
        lateinit var lastKnownLocation: Location
        fun setLocation(lastLocation: Location) {
            lastKnownLocation = lastLocation
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}