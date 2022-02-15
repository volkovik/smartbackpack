package com.example.smartbackpack

import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.example.smartbackpack.settings.BluetoothViewModel
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.system.exitProcess


const val TAG = "MainActivity"
const val REQUEST_ENABLE_BT: Int = 1
const val CURRENT_BLUETOOTH_DEVICE = "CURRENT_BLUETOOTH_DEVICE"


class MainActivity: AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothConnectionThread: BluetoothConnectionThread
    private lateinit var bluetoothIsOffAlertDialog: AlertDialog

    // Этот ViewModel предназначен для управления Bluetooth устройствами в SettingsFragment
    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    private val bluetoothAdapterStateReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action

            // Если было изменено состояние Bluetooth адаптера
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                when (bluetoothAdapter.state) {
                    // Bluetooth выключается
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        // Показываем диалоговое окно об выключенном Bluetooth
                        bluetoothIsOffAlertDialog.show()
                    }
                    // Bluetooth включается
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        // Скрываем диалоговое окно об выключенном Bluetooth
                        bluetoothIsOffAlertDialog.dismiss()
                    }
                    BluetoothAdapter.STATE_ON -> {
                        // Восстанавливаем соединение
                        initBluetoothFeatures()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)

        // Set up bottom navigation
        bottomNav.let {
            NavigationUI.setupWithNavController(it, navController)
        }

        // Инициализируем диалоговое окно для случаев, когда Bluetooth был выключен
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle(R.string.alert)
            setMessage(R.string.bluetooth_required_explanation)
            setPositiveButton(R.string.turn_on_button) { dialog, _ ->
                // Просим пользователя включить Bluetooth через диалоговое окно
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

                dialog.dismiss()
            }
            setNegativeButton(R.string.exit_button) { _, _ ->
                finish()
                exitProcess(0)
            }
            setCancelable(false)
        }
        bluetoothIsOffAlertDialog = builder.create()

        // Получаем Bluetooth адаптер для инициализации Bluetooth подключения к устройству
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        // Если Bluetooth выключен
        if (!bluetoothAdapter.isEnabled) {
            // Попросить пользователя включить Bluetooth
            bluetoothIsOffAlertDialog.show()
        } else {
            initBluetoothFeatures()
        }
        // Отслеживаем изменения состояния Bluetooth, т. е. включён или выключен
        this.registerReceiver(
            bluetoothAdapterStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothConnectionThread.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Если пользователь отказался от чего-либо
        if (resultCode == RESULT_CANCELED) {
            when (requestCode) {
                // Отказ от включения Bluetooth
                REQUEST_ENABLE_BT -> {
                    // Объяснить пользователю, что Bluetooth обязательное условие для работы
                    // приложения и попробовать снова включить Bluetooth
                    bluetoothIsOffAlertDialog.show()
                }
            }
        }
    }

    private fun initBluetoothFeatures() {
        // Получаем список привязанных Bluetooth устройств
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        // Получаем сохранённые данные с прошлой сессии. А именно выбранное устройство
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val macAddressOfBluetoothDevice: String? =
            sharedPreferences.getString(CURRENT_BLUETOOTH_DEVICE, null)
        // Если выбранное устройство не было найдено, либо его не было до этого, то мы выбираем
        // первое попавшиеся устройство
        val selectedDevice: BluetoothDevice = pairedDevices.find {
            it.address == macAddressOfBluetoothDevice
        } ?: pairedDevices.first()

        // Обновляем информацию в ViewModel для SettingsFragment
        bluetoothViewModel.changePairedDevices(pairedDevices)
        bluetoothViewModel.changeCurrentDevice(selectedDevice)

        bluetoothViewModel.currentDevice.observe(this@MainActivity) {
            // Сохраняем выбранное устройство
            val editor = sharedPreferences.edit()
            editor.putString(CURRENT_BLUETOOTH_DEVICE, it.address)
            editor.apply()

            // Подключаемся к устройству и заканчиваем прошлые сессии
            Log.d(TAG, "Start Bluetooth connection thread with ${it.name}")
            if (this::bluetoothConnectionThread.isInitialized) {
                bluetoothConnectionThread.cancel()
            }
            bluetoothConnectionThread = BluetoothConnectionThread(it)
            bluetoothConnectionThread.start()
        }
    }

    private inner class BluetoothConnectionThread(
        device: BluetoothDevice
    ): Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
        }
        val handler: Handler = object: Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    // Если подключение закончилось неудачно
                    0 -> Toast.makeText(this@MainActivity, msg.obj as String, Toast.LENGTH_LONG)
                        .show()
                }
            }
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
                    handler.obtainMessage(
                        0,
                        "Выбранное Bluetooth устройство не выходит на связь. Выберите другое устройство."
                    ).sendToTarget()
                    return
                }

                Log.i(TAG, "Bluetooth connection with ${it.remoteDevice.name} was success")
                bluetoothTransferDataThread = BluetoothTransferDataThread(it)
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

    private inner class BluetoothTransferDataThread(
        mmSocket: BluetoothSocket
    ): Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmBuffer: ByteArray = ByteArray(32)
        private val handler = object : Handler(Looper.myLooper()!!) {
            var text: String = ""

            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    // Если были получены данные
                    0 -> {
                        text += msg.obj

                        if (text.endsWith("\n\r") || text.endsWith("\r\n")) {
                            text = text.trimEnd()
                            // Прислать данные в виде короткого уведомления
                            Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
                            text = ""
                        }
                    }
                }
            }
        }

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
}