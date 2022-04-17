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
import android.content.*
import android.os.*
import android.util.Log
import androidx.activity.viewModels
import com.example.smartbackpack.settings.BluetoothViewModel
import com.example.smartbackpack.utilities.bluetooth.BluetoothConnectionHandler
import com.example.smartbackpack.utilities.bluetooth.BluetoothConnectionThread
import com.example.smartbackpack.utilities.bluetooth.BluetoothTransferDataHandler
import kotlin.system.exitProcess


const val TAG = "MainActivity"
const val REQUEST_ENABLE_BT: Int = 1
const val CURRENT_BLUETOOTH_DEVICE = "CURRENT_BLUETOOTH_DEVICE"


class MainActivity: AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothIsOffAlertDialog: AlertDialog
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

    private lateinit var tBluetoothConnection: BluetoothConnectionThread
    private val hBluetoothConnection = BluetoothConnectionHandler(this)
    private val hBluetoothTransferData = BluetoothTransferDataHandler(this)

    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)

        // Подготавливаем нижнее меню
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
        tBluetoothConnection.cancel()
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
            if (this::tBluetoothConnection.isInitialized) {
                tBluetoothConnection.cancel()
            }
            tBluetoothConnection = BluetoothConnectionThread(
                bluetoothAdapter, it,
                hBluetoothConnection, hBluetoothTransferData
            )
            tBluetoothConnection.start()
        }
    }
}