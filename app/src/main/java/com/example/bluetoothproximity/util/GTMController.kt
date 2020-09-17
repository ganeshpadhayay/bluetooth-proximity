package com.example.bluetoothproximity.util

class GTMController {

    companion object {
        @JvmStatic
        val INSTANCE: GTMController by lazy {
            GTMController()
        }
    }

    fun getScanPollTime(): Long {
        return 60 * 1000
    }

    fun isAdaptiveScanEnabled(): Boolean {
        return true
    }

    fun getAdaptiveScanUpperBalancedUniqueAppDevices(): Long {
        return 10
    }

    fun getAdaptiveScanUpperBalancedAdvertisementInterval(): Long {
        return 60 * 1000
    }

    fun getAdaptiveScanLowerBalancedUniqueAppDevices(): Long {
        return 5
    }

    fun getAdaptiveScanLowerBalancedAdvertisementInterval(): Long {
        return 60 * 1000
    }

    fun getAdaptiveScanAcceptableUniqueDeviceDelta(): Long {
        return 10
    }

    fun getAdaptiveScanKScanInterval(): Long {
        return 60 * 1000
    }
}