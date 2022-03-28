package com.example.smartbackpack.items

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbackpack.R
import com.example.smartbackpack.utilities.database.AppDatabase
import com.example.smartbackpack.utilities.database.Item
import com.example.smartbackpack.utilities.database.ItemDao

class ItemAdapter(
    val context: Context,
    private val items: List<Item>
): RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val adapterLayout: View = LayoutInflater.from(parent.context).inflate(
            R.layout.tag_item, parent, false
        )
        return ViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.label
        holder.textView.setOnClickListener {
            AlertDialog.Builder(context).apply {
                setTitle(item.label)
                setMessage(with(item) {
                    "ID: ${id}\nСостояние: ${if (state) "в рюкзаке" else "нет в рюкзаке"}"
                })
                setNeutralButton("Изменить") { dialog, _ ->
                    val database: AppDatabase = AppDatabase.getDatabase(context)
                    val itemDao: ItemDao = database.itemDao()

                    val editText = EditText(context)
                    editText.setText(item.label)

                    AlertDialog.Builder(context).apply {
                        setTitle("Изменить метку")
                        setView(editText)
                        setPositiveButton("Сохранить") { _, _ ->
                            itemDao.changeItemLabel(item.id, editText.text.toString())
                        }
                        setNegativeButton("Отмена", null)
                        setNeutralButton("Удалить") { _, _ ->
                            itemDao.deleteItem(item.id)
                        }
                    }.create().show()

                    dialog.dismiss()
                }
                setNegativeButton("Закрыть") { dialog, _ -> dialog.dismiss() }
                setCancelable(false)
            }.create().show()
        }
    }

    override fun getItemCount(): Int = items.size
}