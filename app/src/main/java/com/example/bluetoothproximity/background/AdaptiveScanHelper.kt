package com.example.bluetoothproximity.background

import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.bluetoothproximity.util.GTMController
import org.jetbrains.annotations.NotNull
import java.util.*

/**
 * This is a helper class used to change BluetoothLE Scan and advertisement mode using and adaptive algorithm
 * @author Punit Chhajer
 */
class AdaptiveScanHelper(private val listener: AdaptiveModeListener) {

    private var mUniqueDevicesTime = mutableMapOf<String, Long>()
    private var mMinAdvertisingInterval = Long.MAX_VALUE
    private var mOldUniqueDevicesCount = Int.MAX_VALUE

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    var scanMode = ScanSettings.SCAN_MODE_BALANCED

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    var advertisementMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED

    private var mTimer: Timer? = null

    interface AdaptiveModeListener {
        fun onModeChange(scanMode: Int, advertisementMode: Int)
    }

    /**
     * This method is used to add scan result
     * This calculate the minimum latency in the K scan intervals.
     * Store scan timestamp for future latency calculation.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun addScanResult(@NotNull result: ScanResult) {
        if (!GTMController.INSTANCE.isAdaptiveScanEnabled()) {
            return
        }
        val name = result.device.name
        if (name.isNullOrEmpty()) {
            return
        }
        val timestampInMilliSec = result.timestampNanos / 1000000
        val lastResultTimestamp = mUniqueDevicesTime[name]

        lastResultTimestamp?.let {
            val latency = timestampInMilliSec - it
            if (latency < mMinAdvertisingInterval)
                mMinAdvertisingInterval = latency
        }
        mUniqueDevicesTime[name] = timestampInMilliSec
    }

    /**
     * This method is used to Start the adaptive scan evaluation
     */
    fun start() {
        if (!GTMController.INSTANCE.isAdaptiveScanEnabled()) {
            return
        }
        val kScanInterval = GTMController.INSTANCE.getAdaptiveScanKScanInterval()
        mTimer?.cancel()
        mTimer = Timer()
        mTimer?.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        evaluate()
                    }
                },
                kScanInterval,
                kScanInterval
        )
    }

    /**
     * This method is used to Stop the adaptive scan evaluation
     */
    fun stop() {
        mTimer?.cancel()
        mTimer = null
    }

    /**
     * This method is used to reset all values to default
     */
    fun reset() {
        stop()
        clear()
        mOldUniqueDevicesCount = Int.MAX_VALUE

        scanMode = ScanSettings.SCAN_MODE_BALANCED
        advertisementMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED
    }


    /**
     * This method is used to evaluate if scan or advertisement mode needs to be change.
     * If scan mode is Balanced or Low Power Mode and data is crossing upper limit of balance mode then change mode to Low Latency
     * if scan mode is Balanced Mode and data is crossing lower limit of balanced mode then change mode to Low Power
     * if scan mode is Low Latency and delta is in acceptable range then change mode to Balanced else change advertisement mode between Balanced and Low Latency alternatively
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun evaluate() {
        val uniqueAppDevicesCount = mUniqueDevicesTime.size
        if (scanMode != ScanSettings.SCAN_MODE_LOW_LATENCY) {
            if (uniqueAppDevicesCount >= GTMController.INSTANCE.getAdaptiveScanUpperBalancedUniqueAppDevices() || (mMinAdvertisingInterval != Long.MAX_VALUE && mMinAdvertisingInterval >= GTMController.INSTANCE.getAdaptiveScanUpperBalancedAdvertisementInterval())) {
                changeMode(ScanSettings.SCAN_MODE_LOW_LATENCY, AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            } else if (uniqueAppDevicesCount >= GTMController.INSTANCE.getAdaptiveScanLowerBalancedUniqueAppDevices() || (mMinAdvertisingInterval != Long.MAX_VALUE && mMinAdvertisingInterval >= GTMController.INSTANCE.getAdaptiveScanLowerBalancedAdvertisementInterval())) {
                changeMode(ScanSettings.SCAN_MODE_BALANCED, AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            } else {
                changeMode(ScanSettings.SCAN_MODE_LOW_POWER, AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            }
        } else if (mOldUniqueDevicesCount != Int.MAX_VALUE) {
            if ((uniqueAppDevicesCount - mOldUniqueDevicesCount) >= GTMController.INSTANCE.getAdaptiveScanAcceptableUniqueDeviceDelta()) {
                val advertisementMode = if (advertisementMode == AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) AdvertiseSettings.ADVERTISE_MODE_BALANCED else AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                changeMode(ScanSettings.SCAN_MODE_LOW_LATENCY, advertisementMode)
            } else {
                changeMode(ScanSettings.SCAN_MODE_BALANCED, AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            }
        }
        mOldUniqueDevicesCount = uniqueAppDevicesCount
        clear()
    }

    /**
     * This method is used to change mode of BluetoothLE Scan and advertisement and restarting scan window
     */
    private fun changeMode(scanMode: Int, advertisementMode: Int) {
        if (!GTMController.INSTANCE.isAdaptiveScanEnabled()) {
            return
        }
        if (this.scanMode != scanMode || this.advertisementMode != advertisementMode) {
            this.scanMode = scanMode
            this.advertisementMode = advertisementMode
            listener.onModeChange(scanMode, advertisementMode)
            start()
        }
    }

    /**
     * This method is to clear data after every cycle
     */
    private fun clear() {
        mUniqueDevicesTime.clear()
        mMinAdvertisingInterval = Long.MAX_VALUE
    }
}