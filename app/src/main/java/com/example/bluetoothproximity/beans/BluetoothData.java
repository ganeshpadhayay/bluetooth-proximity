package com.example.bluetoothproximity.beans;


import com.example.bluetoothproximity.background.BluetoothServiceUtility;

public class BluetoothData {
    private int id;
    // This is not MAC Address. This is UNIQUE ID Assign to scanned device.
    private String bluetoothMacAddress;
    // This is the RSSI of the scanned device.
    private Integer distance;
    private String txPower;
    private String txPowerLevel;
    private double latitude;
    private double longitude;
    private Integer timeStamp;
    
    public BluetoothData(String bluetoothMacAddress, Integer distance, String txPower, String txPowerLevel) {
        this.bluetoothMacAddress = bluetoothMacAddress;
        this.distance = distance;
        this.txPower = txPower;
        this.txPowerLevel = txPowerLevel;
        timeStamp = BluetoothServiceUtility.INSTANCE.getCurrentEpochTimeInSec();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBluetoothMacAddress() {
        return bluetoothMacAddress;
    }

    public void setBluetoothMacAddress(String bluetoothMacAddress) {
        this.bluetoothMacAddress = bluetoothMacAddress;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public String getTxPower() {
        return txPower;
    }

    public void setTxPower(String txPower) {
        this.txPower = txPower;
    }

    public String getTxPowerLevel() {
        return txPowerLevel;
    }

    public void setTxPowerLevel(String txPowerLevel) {
        this.txPowerLevel = txPowerLevel;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Integer getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Integer timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public String toString() {
        return "BluetoothData{" +
                "id=" + id +
                ", bluetoothMacAddress='" + bluetoothMacAddress + '\'' +
                ", distance=" + distance +
                ", txPower='" + txPower + '\'' +
                ", txPowerLevel='" + txPowerLevel + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
