package com.example.smartbackpack.settings

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbackpack.R


class BluetoothDevicesAdapter(
    private val context: FragmentActivity,
    private val viewModel: BluetoothViewModel,
    private val dataset: List<BluetoothDevice>
): RecyclerView.Adapter<BluetoothDevicesAdapter.ViewHolder>() {
    private var lastSelectedRadioButton: RadioButton? = null
    private var currentBluetoothDevice: BluetoothDevice? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        viewModel.currentDevice.observe(context) {
            currentBluetoothDevice = it
        }
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val radioButton: RadioButton = view.findViewById(R.id.radio_button_device)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(
            R.layout.paired_device_item, parent, false
        )
        return ViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bluetoothDevice: BluetoothDevice = dataset[position]

        // Если это выбранное до этого Bluetooth устройство, то ставим его по умолчанию
        if (bluetoothDevice == currentBluetoothDevice) {
            holder.radioButton.isChecked = true
            lastSelectedRadioButton = holder.radioButton
        }

        holder.radioButton.text = bluetoothDevice.name
        holder.radioButton.setOnClickListener {
            val selectedRadioButton: RadioButton = it as RadioButton

            // Если пользователь нажимает на одну и ту же кнопку, то ничего не делаем
            if (selectedRadioButton == lastSelectedRadioButton) {
                return@setOnClickListener
            }
            // Меняем устройство
            viewModel.changeCurrentDevice(bluetoothDevice)

            // Убираем прошлое выделение кнопки
            lastSelectedRadioButton?.isChecked = false
            lastSelectedRadioButton = selectedRadioButton
        }
    }

    override fun getItemCount(): Int = dataset.size
}