package com.example.smartbackpack.utilities.bluetooth

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.EditText
import android.widget.Toast
import com.example.smartbackpack.R
import com.example.smartbackpack.utilities.database.AppDatabase
import com.example.smartbackpack.utilities.database.Item
import com.example.smartbackpack.utilities.database.ItemDao

class BluetoothTransferDataHandler(val context: Context) : Handler(Looper.myLooper()!!) {
    var tempID: String = ""

    override fun handleMessage(msg: Message) {
        tempID += msg.obj

        if (tempID.endsWith("\n\r") || tempID.endsWith("\r\n")) {
            tempID = tempID.trimEnd()
            val tagID = tempID

            Toast.makeText(context, tempID, Toast.LENGTH_LONG).show()

            val database: AppDatabase = AppDatabase.getDatabase(context)
            val itemDao: ItemDao = database.itemDao()

            if (itemDao.getItem(tagID) != null) {
                return
            }

            AlertDialog.Builder(context).apply {
                setTitle(R.string.new_item)
                setMessage(R.string.new_item_description)
                setPositiveButton(R.string.add_button) { dialog, _ ->

                    val editText = EditText(context)

                    AlertDialog.Builder(context).apply {
                        setTitle(R.string.add_new_item)
                        setView(editText)
                        setPositiveButton(R.string.add_button) { dialog, _ ->
                            val label: String = editText.text.toString()
                            val item = Item(tagID, label, true)
                            itemDao.postItem(item)
                            dialog.dismiss()
                        }
                        setNegativeButton(R.string.cancel_button) { dialog, _ ->
                            dialog.dismiss()
                        }
                    }.create().show()

                    dialog.dismiss()
                }
                setNegativeButton(R.string.cancel_button) { dialog, _ -> dialog.dismiss() }
                setCancelable(false)
            }.create().show()

            // Прислать данные в виде короткого уведомления
            tempID = ""
        }
    }
}