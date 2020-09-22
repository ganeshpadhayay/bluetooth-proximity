package com.example.bluetoothproximity.beans

data class BluetoothModel(
        var uniqueId: String?,
        var data: String?,
        var rssi: Int? = 0,
        var txPower: String? = "",
        var txPowerLevel: String? = "",
        var longitude: Double? = null,
        var lattitude: Double? = null
) {
    override fun toString(): String {
        return "BluetoothModel(uniqueUserId=$uniqueId, coronaLevel=$data, rssi=$rssi, txPower=$txPower, txPowerLevel=$txPowerLevel, longitude=$longitude, lattitude=$lattitude)"
    }
}
