package com.example.smartbackpack.settings

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BluetoothViewModel: ViewModel() {
    private val mutablePairedDevices = MutableLiveData<Set<BluetoothDevice>>()
    val pairedDevices: LiveData<Set<BluetoothDevice>> get() = mutablePairedDevices

    private val mutableCurrentDevice = MutableLiveData<BluetoothDevice>()
    val currentDevice: LiveData<BluetoothDevice> get() = mutableCurrentDevice

    fun changeCurrentDevice(device: BluetoothDevice) {
        mutableCurrentDevice.value = device
    }

    fun changePairedDevices(devices: Set<BluetoothDevice>) {
        mutablePairedDevices.value = devices
    }
}