package com.example.bluetoothproximity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothproximity.background.BluetoothScanningService
import com.example.bluetoothproximity.background.BluetoothServiceUtility

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        if (!BluetoothScanningService.serviceRunning) {
            BluetoothServiceUtility.startService(this)
        }
        BluetoothServiceUtility.startBackgroundWorker()
    }
}