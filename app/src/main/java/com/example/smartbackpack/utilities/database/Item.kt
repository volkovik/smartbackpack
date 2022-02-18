package com.example.smartbackpack.utilities.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "items")
data class Item(
    @PrimaryKey
    @NotNull
    var id: String,
    var label: String,
    var state: Boolean
)