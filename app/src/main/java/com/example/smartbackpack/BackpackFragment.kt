package com.example.smartbackpack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbackpack.utilities.ItemAdapter
import com.example.smartbackpack.utilities.database.AppDatabase
import com.example.smartbackpack.utilities.database.ItemDao

class BackpackFragment: Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView: RecyclerView = view.findViewById(R.id.current_items_in_backpack)

        val database: AppDatabase = AppDatabase.getDatabase(requireContext())
        val itemDao: ItemDao = database.itemDao()

        itemDao.getAllItems().observe(requireActivity()) {
            recyclerView.adapter = context?.let { ctx ->
                ItemAdapter(ctx, it.filter { item -> item.state }, listOf())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_backpack, container, false)
    }

}