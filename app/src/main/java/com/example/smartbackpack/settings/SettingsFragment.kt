package com.example.smartbackpack.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbackpack.R


class SettingsFragment: Fragment() {
    private val bluetoothViewModel: BluetoothViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView: RecyclerView = view.findViewById(R.id.paired_bluetooth_devices_choice)
        recyclerView.setHasFixedSize(true)

        bluetoothViewModel.pairedDevices.observe(requireActivity()) {
            recyclerView.adapter = BluetoothDevicesAdapter(
                requireActivity(), bluetoothViewModel, it.toList()
            )
        }
    }
}