package com.Meditation.Sounds.frequencies.lemeor.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.Meditation.Sounds.frequencies.lemeor.data.model.Playlist

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Playlist>?)

    @Delete
    suspend fun deletePlaylists(list: List<Playlist>?)

    @Query("SELECT * FROM playlist ORDER BY `order` ASC")
    fun getPlaylists() : LiveData<List<Playlist>>

    @Query("SELECT * FROM playlist ORDER BY `order` ASC")
    suspend fun getData() : List<Playlist>

    @Query("DELETE FROM playlist")
    suspend fun clear()
}