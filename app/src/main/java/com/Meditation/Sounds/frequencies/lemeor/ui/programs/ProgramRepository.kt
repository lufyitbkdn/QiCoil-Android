package com.Meditation.Sounds.frequencies.lemeor.ui.programs

import androidx.lifecycle.LiveData
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track

class ProgramRepository(private val localData: DataBase) {

    fun getMy(): LiveData<List<Program>> {
        return localData.programDao().getMy(true)
    }

    fun getPrograms(): LiveData<List<Program>> {
        return localData.programDao().getPrograms()
    }

    suspend fun insert(program: Program?) {
        localData.programDao().insert(program)
    }

    suspend fun delete(program: Program?) {
        localData.programDao().delete(program)
    }

    suspend fun getTrackById(id: Int): Track? {
        return localData.trackDao().getTrackById(id)
    }

    suspend fun getAlbumById(id: Int): Album? {
        return localData.albumDao().getAlbumById(id)
    }
}