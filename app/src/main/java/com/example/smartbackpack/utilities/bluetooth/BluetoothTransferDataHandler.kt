package com.example.smartbackpack.utilities.bluetooth

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast

class BluetoothTransferDataHandler(val context: Context) : Handler(Looper.myLooper()!!) {
    var text: String = ""

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            // Если были получены данные
            0 -> {
                text += msg.obj

                if (text.endsWith("\n\r") || text.endsWith("\r\n")) {
                    text = text.trimEnd()
                    // Прислать данные в виде короткого уведомления
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    text = ""
                }
            }
        }
    }
}