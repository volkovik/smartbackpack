package com.example.smartbackpack.utilities.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey
    val id: String,
    val label: String,
    val state: Boolean
)