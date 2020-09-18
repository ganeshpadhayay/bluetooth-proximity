package com.example.bluetoothproximity

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothproximity.background.BluetoothScanningService
import com.example.bluetoothproximity.background.BluetoothServiceUtility
import com.example.bluetoothproximity.util.Constants
import com.example.bluetoothproximity.util.GpsUtils

class MainActivity : AppCompatActivity() {

    val BLUETOOTH_LOCATION_PERMISSION = 101
    val BLUETOOTH_ENABLE_CODE = 102
    private var bluetoothAdapter: BluetoothAdapter? = null
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //check if the device's bluetooth iw working or not, if working then set a name
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            showToast("No Bluetooth on this handset")
        } else {
            setBluetoothName()
        }

        //check all the permissions and all
        handlePermissionsFlow()

        //check for GPS location
        if (!BluetoothServiceUtility.isGPSEnabled(this)) {
            enableGPSLocation()
        }
    }

    private fun handlePermissionsFlow() {
        if (bluetoothAdapter?.isEnabled!!) {
            if (bluetoothAdapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                initBluetoothAndLocationServices()
            } else {
                makeBluetoothDiscoverable()
            }
        } else {
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
        if (defaultAdapter != null && !defaultAdapter.isEnabled) {
            defaultAdapter.enable()
            handlePermissionsFlow()
        }
    }

    private fun initBluetoothAndLocationServices() {
        if (BluetoothServiceUtility.arePermissionsGranted(this)) {
            startBluetoothServiceAndWorkManager()
        } else {
            BluetoothServiceUtility.requestPermissions(this, BLUETOOTH_LOCATION_PERMISSION)
        }
    }

    private fun startBluetoothServiceAndWorkManager() {
        if (!BluetoothScanningService.serviceRunning) {
            BluetoothServiceUtility.startService(this)
        }
        BluetoothServiceUtility.startBackgroundWorker()
    }

    private fun setBluetoothName() {
        val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
        val uniqueId = Constants.BLUETOOTH_UNIQUE_NAME
        if (uniqueId.isNotEmpty()) {
            if (BluetoothServiceUtility.isBluetoothPermissionAvailable(MyApplication.context) && defaultAdapter != null && uniqueId.isNotEmpty()) {
                defaultAdapter.name = uniqueId
            }
        }
    }

    private fun makeBluetoothDiscoverable() {
        try {
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply { putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300000) }
            startActivityForResult(discoverableIntent, BLUETOOTH_ENABLE_CODE)
        } catch (ex: Exception) {
            Log.d(TAG, ex.toString())
        }
    }

    private fun enableGPSLocation() {
        GpsUtils(this).turnGPSOn(object : GpsUtils.OnGPSStateChangeListener {
            override fun onGpsTurnedOn() {
                initBluetoothAndLocationServices()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == BLUETOOTH_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!BluetoothServiceUtility.isGPSEnabled(this)) {
                    enableGPSLocation()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BLUETOOTH_ENABLE_CODE || requestCode == Constants.GPS_REQUEST) {
            handlePermissionsFlow()
        }
    }

    private fun showToast(message: String, interval: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, interval).show()
    }

}