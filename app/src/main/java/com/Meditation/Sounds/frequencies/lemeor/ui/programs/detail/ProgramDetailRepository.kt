package com.Meditation.Sounds.frequencies.lemeor.ui.programs.detail

import androidx.lifecycle.LiveData
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track

class ProgramDetailRepository(private val localData: DataBase) {
    fun getProgramById(id: Int): LiveData<Program>? {
        return localData.programDao().getProgramByIdLive(id)
    }

    suspend fun getTrackById(id: Int): Track? {
        return localData.trackDao().getTrackById(id)
    }
}