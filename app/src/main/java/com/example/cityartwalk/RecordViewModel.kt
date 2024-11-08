package com.example.cityartwalk

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.cityartwalk.data.RecordDatabase
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordRepository
    val allRecords: LiveData<List<Record>>
    var imageBitmap: Bitmap? = null

    init {
        val recordDao = RecordDatabase.getDatabase(application).recordDao()
        repository = RecordRepository(recordDao)
        allRecords = repository.allRecords
    }

    suspend fun insert(record: Record): Long {
        return repository.insert(record)
    }

    fun update(record: Record) = viewModelScope.launch {
        repository.update(record)
    }

    fun delete(record: Record) = viewModelScope.launch {
        repository.delete(record)
    }

    fun getRecordById(id: Int): LiveData<Record> {
        return repository.getRecordById(id)
    }


}
