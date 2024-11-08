package com.example.cityartwalk

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record_table")
data class Record(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var title: String = "",
    var address: String = "",
    var date: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var imagePath: String? = null
)