package com.Meditation.Sounds.frequencies.lemeor.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.Meditation.Sounds.frequencies.lemeor.data.model.Tier

@Dao
interface TierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tier: Tier?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Tier>?)

    @Query("UPDATE tier SET isShow=:isShow WHERE id=:id")
    suspend fun updateShowStatus(isShow: Boolean, id: Int)

    @Query("SELECT tier.isShow FROM tier WHERE id=:tierId")
    suspend fun getShowStatus(tierId: Int) : Boolean

    @Query("UPDATE tier SET isPurchased=:isPurchased WHERE id=:id")
    suspend fun updatePurchaseStatus(isPurchased: Boolean, id: Int)

    @Query("DELETE FROM tier")
    suspend fun clear()

    @Delete
    suspend fun delete(tier: Tier)

    @Delete
    suspend fun deleteTiers(list: List<Tier>?)

    @Query("SELECT * FROM tier ORDER BY `order` ASC")
    fun getTiers() : LiveData<List<Tier>>

    @Query("SELECT * FROM tier WHERE isShow=:isShow ORDER BY `order` ASC")
    fun getTiers(isShow: Boolean) : LiveData<List<Tier>>

    @Query("SELECT * FROM tier ORDER BY `order` ASC")
    suspend fun getData() : List<Tier>

    @Query("SELECT name FROM tier WHERE id=:tierId ORDER BY `order` ASC")
    suspend fun getTierNameById(tierId: Int) : String?
}