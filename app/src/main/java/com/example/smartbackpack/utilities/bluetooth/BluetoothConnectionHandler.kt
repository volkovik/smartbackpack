package com.example.smartbackpack.utilities.bluetooth

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast

class BluetoothConnectionHandler(private val context: Context) : Handler(Looper.myLooper()!!) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            // Если подключение закончилось неудачно
            0 -> Toast.makeText(context, msg.obj as String, Toast.LENGTH_LONG)
                .show()
        }
    }
}