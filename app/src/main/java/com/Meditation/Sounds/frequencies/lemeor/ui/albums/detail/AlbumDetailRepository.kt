package com.Meditation.Sounds.frequencies.lemeor.ui.albums.detail

import androidx.lifecycle.LiveData
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album

class AlbumDetailRepository(private val localData: DataBase) {
    fun getAlbumsById(id: Int): LiveData<Album>? {
        return localData.albumDao().getAlbumsById(id)
    }

    suspend fun getAlbumsByIdOnce(id: Int): Album? {
        return localData.albumDao().getAlbumsByIdOnce(id)
    }
}

