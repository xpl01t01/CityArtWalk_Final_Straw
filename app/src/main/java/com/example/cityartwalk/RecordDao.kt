package com.example.cityartwalk

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(record: Record): Long


    @Update
    suspend fun update(record: Record)

    @Delete
    suspend fun delete(record: Record)

    @Query("SELECT * FROM record_table ORDER BY date DESC")
    fun getAllRecords(): LiveData<List<Record>>

    @Query("SELECT * FROM record_table WHERE id = :id")
    fun getRecordById(id: Int): LiveData<Record>


}