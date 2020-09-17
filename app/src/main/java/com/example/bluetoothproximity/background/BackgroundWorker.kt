package com.example.bluetoothproximity.background

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.bluetoothproximity.util.Constants

class BackgroundWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val mContext: Context = context

    override fun doWork(): Result {
        val intent = Intent(mContext, BluetoothScanningService::class.java)
        intent.putExtra(Constants.FROM_MY_WORKER, true)
        startService(intent)
        if (BluetoothServiceUtility.isBluetoothAvailable()) {
            BluetoothAdapter.getDefaultAdapter().startDiscovery()
        }
        return Result.success()
    }

    private fun startService(intent: Intent) {
        val uniqueId = "121"
        if (!BluetoothScanningService.serviceRunning && uniqueId.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(intent)
            } else {
                mContext.startService(intent)
            }
        }
    }

    companion object {
        val UNIQUE_WORK_NAME = BackgroundWorker::class.java.simpleName
    }
}