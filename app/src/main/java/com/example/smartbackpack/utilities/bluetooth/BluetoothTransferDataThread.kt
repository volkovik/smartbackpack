package com.example.smartbackpack.utilities.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.example.smartbackpack.TAG
import java.io.IOException
import java.io.InputStream

class BluetoothTransferDataThread(
    mmSocket: BluetoothSocket,
    private val handler: Handler
): Thread() {
    private val mmInStream: InputStream = mmSocket.inputStream
    private val mmBuffer: ByteArray = ByteArray(32)

    override fun run() {
        Looper.prepare()
        var numBytes: Int

        Log.d(TAG, "Trying to start input stream...")

        // Запускаем цикл, проверяющий на данные в Input Stream
        while (true)
        {
            numBytes = try {
                mmInStream.read(mmBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "Input stream was disconnected", e)
                break
            }

            // Переводим полученные байты в строку
            val message = String(mmBuffer, 0, numBytes)
            Log.d(TAG, "Received data: $message")
            handler.obtainMessage(0, message).sendToTarget()
        }

        Looper.loop()
    }
}