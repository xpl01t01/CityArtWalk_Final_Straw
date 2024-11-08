package com.example.cityartwalk

import androidx.lifecycle.LiveData

class RecordRepository(private val recordDao: RecordDao) {

    val allRecords: LiveData<List<Record>> = recordDao.getAllRecords()

    suspend fun insert(record: Record): Long {
        return recordDao.insert(record)
    }

    suspend fun update(record: Record) {
        recordDao.update(record)
    }

    suspend fun delete(record: Record) {
        recordDao.delete(record)
    }

    fun getRecordById(id: Int): LiveData<Record> {
        return recordDao.getRecordById(id)
    }
}
