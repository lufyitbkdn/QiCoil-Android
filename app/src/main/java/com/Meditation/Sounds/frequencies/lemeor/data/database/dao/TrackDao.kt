package com.Meditation.Sounds.frequencies.lemeor.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: Track?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listAlbums: List<Track>?)

    @Delete
    suspend fun deleteTracks(list: List<Track>?)

    @Delete
    suspend fun delete(track: Track?)

    @Query("DELETE FROM track")
    suspend fun clear()

    @Query("SELECT * FROM track")
    fun getTracks() : LiveData<List<Track>>

    @Query("SELECT * FROM track WHERE id=:id")
    suspend fun getTrackById(id: Int) : Track?

    @Query("SELECT id FROM track WHERE name=:name")
    suspend fun getTrackIdByName(name: String) : Int

    @Query("UPDATE track SET isDownloaded=:isDownloaded WHERE id=:id")
    suspend fun isTrackDownloaded(isDownloaded: Boolean, id: Int)

    @Query("UPDATE track SET isFavorite=:isFavorite WHERE id=:id")
    suspend fun isTrackFavorite(isFavorite: Boolean, id: Int)

    @Query("SELECT * FROM track WHERE name LIKE :searchString")
    suspend fun searchTrack(searchString: String): List<Track>

    @Query("UPDATE track SET isUnlocked=:isUnlocked WHERE id=:id")
    suspend fun setTrackUnlocked(isUnlocked: Boolean, id: Int)

    @Query("UPDATE track SET isDownloaded=:isDownloaded WHERE id=:id")
    suspend fun setTrackDownloaded(isDownloaded: Boolean, id: Int)

    @Query("UPDATE track SET duration=:d WHERE id=:id")
    suspend fun setDuration(d: Long, id: Int)

    @Query("SELECT * FROM track")
    suspend fun getData() : List<Track>

    @Query("UPDATE track SET isDownloaded=:isDownloaded, isFavorite=:isFavorite, isUnlocked=:isUnlocked WHERE id=:id")
    suspend fun syncTracks(isDownloaded: Boolean, isFavorite: Boolean, isUnlocked: Boolean, id: Int)
}