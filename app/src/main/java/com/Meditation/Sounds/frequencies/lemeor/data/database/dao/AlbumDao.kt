package com.Meditation.Sounds.frequencies.lemeor.data.database.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listAlbums: List<Album>?)

    @Delete
    suspend fun deleteAlbums(list: List<Album>?)

    @Delete
    suspend fun delete(album: Album)

    @Query("SELECT * FROM album WHERE category_id=:id ORDER BY `order` ASC")
    suspend fun getAlbumsByCategory(id: Int) : List<Album>

    @Query("SELECT * FROM album WHERE category_id=:id ORDER BY `order` ASC")
    fun getAlbumsByCategoryLiveData(id: Int) : LiveData<List<Album>>

    @Query("SELECT * FROM album WHERE tier_id=:id ORDER BY `order` ASC")
    suspend fun getAlbumsByTierId(id: Int) : List<Album>

    @Query("SELECT * FROM album ORDER BY `order` ASC")
    suspend fun getAllAlbums() : List<Album>

    @Query("SELECT * FROM album WHERE descriptions LIKE :searchString OR descriptions LIKE :searchString")
    suspend fun searchAlbum(searchString: String): List<Album>

    @Query("SELECT * FROM album WHERE id=:id ORDER BY `order` ASC")
    fun getAlbumsById(id: Int) : LiveData<Album>

    @Query("SELECT * FROM album WHERE id=:id ORDER BY `order` ASC")
    suspend fun getAlbumsByIdOnce(id: Int) : Album?

    @Query("UPDATE album SET isDownloaded=:isDownloaded WHERE id=:id")
    suspend fun isAlbumDownloaded(isDownloaded: Boolean, id: Int)

    @Query("UPDATE album SET isUnlocked=:isUnlocked WHERE category_id=:categoryId AND is_free=:isFree")
    suspend fun setUnlockedStatusByCategoryId(isUnlocked: Boolean, categoryId: Int, isFree: Boolean)

    @Query("SELECT * FROM album WHERE isUnlocked=:isUnlocked ORDER BY `order` ASC")
    suspend fun getUnlockedAlbums(isUnlocked: Boolean): List<Album>

    @Query("SELECT * FROM album WHERE id=:id")
    suspend fun getAlbumById(id: Int) : Album?

    @Query("SELECT * FROM album WHERE isUnlocked=:isUnlocked ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomAlbum(isUnlocked: Boolean) : Album?

    @Query("SELECT * FROM album ORDER BY `order` ASC")
    suspend fun getData() : List<Album>

    @Query("UPDATE album SET isDownloaded=:isDownloaded,  isUnlocked=:isUnlocked WHERE id=:id")
    suspend fun syncAlbums(isDownloaded: Boolean, isUnlocked: Boolean, id: Int)

    @Query("UPDATE album SET isUnlocked=:isUnlocked WHERE id=:id")
    suspend fun syncAlbums(isUnlocked: Boolean, id: Int)

    @Query("UPDATE album SET isDownloaded=:isDownloaded WHERE id=:id")
    suspend fun syncDownloaded(isDownloaded: Boolean, id: Int)



    // new build 29.03.2021
    @Query("UPDATE album SET isUnlocked=:isUnlocked WHERE id=:id AND is_free=:isFree")
    suspend fun setNewUnlockedById(isUnlocked: Boolean, id: Int, isFree: Boolean = false)

    @Query("UPDATE album SET isUnlocked=:isUnlocked WHERE category_id=:categoryId AND is_free=:isFree")
    suspend fun setNewUnlockedByCategoryId(isUnlocked: Boolean, categoryId: Int, isFree: Boolean = false)

    @Query("UPDATE album SET isUnlocked=:isUnlocked WHERE tier_id=:tierId AND is_free=:isFree")
    suspend fun setNewUnlockedByTierId(isUnlocked: Boolean, tierId: Int, isFree: Boolean = false)

    @Query("DELETE FROM album")
    suspend fun clear()
}