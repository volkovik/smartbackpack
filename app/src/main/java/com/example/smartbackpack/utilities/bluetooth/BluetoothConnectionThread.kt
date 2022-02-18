package com.example.smartbackpack.utilities.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Looper
import android.util.Log
import com.example.smartbackpack.TAG
import java.io.IOException
import java.util.*

class BluetoothConnectionThread(
    private val bluetoothAdapter: BluetoothAdapter,
    private val device: BluetoothDevice,
    private val hBluetoothConnection: BluetoothConnectionHandler,
    private val hBluetoothTransferData: BluetoothTransferDataHandler
): Thread() {
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
    }
    private var bluetoothTransferDataThread: BluetoothTransferDataThread? = null

    override fun run() {
        Looper.prepare()
        bluetoothAdapter.cancelDiscovery()

        mmSocket?.let {
            // Подключаемся к устройству
            try {
                it.connect()
            } catch (e: IOException) {
                Log.e(TAG, "Bluetooth connection went wrong with ${it.remoteDevice.name}", e)
                hBluetoothConnection.obtainMessage(
                    0,
                    "Выбранное Bluetooth устройство не выходит на связь. Выберите другое устройство."
                ).sendToTarget()
                return
            }

            Log.i(TAG, "Bluetooth connection with ${it.remoteDevice.name} was success")
            bluetoothTransferDataThread = BluetoothTransferDataThread(it, hBluetoothTransferData)
            bluetoothTransferDataThread!!.start()
        }

        Looper.loop()
    }

    fun cancel() {
        // Закрыть Bluetooth подключение
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the client socket", e)
        }
    }
}