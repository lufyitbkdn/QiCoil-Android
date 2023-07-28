package com.Meditation.Sounds.frequencies.lemeor.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.Meditation.Sounds.frequencies.lemeor.data.model.HomeResponse

@Dao
interface HomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHome(home: HomeResponse)

    @Query("SELECT * FROM home")
    fun getHome() : LiveData<HomeResponse>

    @Query("DELETE FROM home")
    suspend fun clear()
}