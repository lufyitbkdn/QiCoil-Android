package com.Meditation.Sounds.frequencies.lemeor.ui.programs.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track


class ProgramDetailViewModel (private val repository: ProgramDetailRepository) : ViewModel() {
    fun program(id: Int): LiveData<Program>? {
        return repository.getProgramById(id)
    }

    suspend fun getTrackById(id: Int): Track? {
        return repository.getTrackById(id)
    }
}