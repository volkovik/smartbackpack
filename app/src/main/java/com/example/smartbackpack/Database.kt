package com.example.smartbackpack

import android.content.Context
import androidx.room.*

@Entity(tableName = "items")
data class Item(
    @PrimaryKey
    val id: String,
    val label: String,
    val state: Boolean
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItem(id: String): Item

    @Query("SELECT * FROM items ORDER BY label ASC")
    fun getAllItems(): List<Item>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun postItem(item: Item)

    @Query("UPDATE items SET state = :state WHERE id = :id")
    fun changeItemState(id: String, state: Boolean)

    @Query("UPDATE items SET label = :label WHERE id = :id")
    fun changeItemLabel(id: String, label: String)
}

@Database(entities = [Item::class], version = 1, exportSchema = true)
abstract class AppDatabase: RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE

            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "main"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}