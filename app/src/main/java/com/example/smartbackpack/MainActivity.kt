package com.example.smartbackpack

import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.app.AlertDialog
import android.content.*
import kotlin.system.exitProcess


const val REQUEST_ENABLE_BT: Int = 1


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
        }
        // Отслеживаем изменения состояния Bluetooth, т. е. включён или выключен
        this.registerReceiver(
            bluetoothAdapterStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
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
}