package com.example.smartbackpack.utilities.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItem(id: String): Item?

    @Query("SELECT * FROM items ORDER BY label ASC")
    fun getAllItems(): List<Item>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun postItem(item: Item)

    @Query("UPDATE items SET state = :state WHERE id = :id")
    fun changeItemState(id: String, state: Boolean)

    @Query("UPDATE items SET label = :label WHERE id = :id")
    fun changeItemLabel(id: String, label: String)
}