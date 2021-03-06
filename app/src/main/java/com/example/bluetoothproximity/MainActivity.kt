package com.example.bluetoothproximity

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothproximity.background.BluetoothScanningService
import com.example.bluetoothproximity.background.BluetoothServiceUtility
import com.example.bluetoothproximity.util.Constants
import com.example.bluetoothproximity.util.GpsUtils
import com.example.bluetoothproximity.util.SharedPreferenceHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val BLUETOOTH_LOCATION_PERMISSION = 101
    val BLUETOOTH_ENABLE_CODE = 102
    private var bluetoothAdapter: BluetoothAdapter? = null
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartService?.setOnClickListener {
            if (etEmployeeId.text.isNotEmpty() && etCoronaSymptoms.text.isNotEmpty()) {
                hideKeyboard(this.window, this)

                //update values in share pref
                val sharedPref = SharedPreferenceHelper.getSharedPreferenceHelper()
                with(sharedPref.edit()) {
                    putString(Constants.BLUETOOTH_NAME_UNIQUE_ID, etEmployeeId.text.toString())
                    putString(Constants.CORONA_SEVERITY_LEVEL, etCoronaSymptoms.text.toString())
                    apply()
                }

                //check if the device's bluetooth is working or not, if working then set a name
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    showToast("No Bluetooth on this handset")
                }

                //check all the permissions and all
                handlePermissionsFlow()
            } else {
                showToast("Please fill the details first")
            }
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
            if (!BluetoothServiceUtility.isGPSEnabled(this)) {
                enableGPSLocation()
            } else {
                startBluetoothServiceAndWorkManager()
            }
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
                initBluetoothAndLocationServices()
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

    private fun hideKeyboard(window: Window, context: Context) {
        try {
            val focusView = window.currentFocus
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            focusView?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        } catch (e: Exception) {
        }
    }
}