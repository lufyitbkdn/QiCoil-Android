package com.Meditation.Sounds.frequencies.lemeor.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program

@Dao
interface ProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(program: Program?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<Program>?)

    @Delete
    fun delete(program: Program?)

    @Delete
    fun deletePrograms(list: List<Program>?)

    @Query("SELECT * FROM program ORDER BY `order` ASC")
    fun getPrograms() : LiveData<List<Program>>?

    @Query("SELECT * FROM program WHERE isMy=:isMy ORDER BY `order` ASC")
    fun getMy(isMy: Boolean) : LiveData<List<Program>>?

    @Query("SELECT * FROM program WHERE id=:id ORDER BY `order` ASC")
    fun getProgramByIdLive(id: Int) : LiveData<Program>?

    @Query("SELECT * FROM program WHERE id=:id")
    fun getProgramById(id: Int) : Program?

    @Query("SELECT * FROM program WHERE name=:name")
    fun getProgramByName(name: String) : Program?

    @Update
    fun updateProgram(program: Program)

    @Query("SELECT * FROM program WHERE name LIKE :searchString")
    fun searchProgram(searchString: String): List<Program>?

    @Query("SELECT * FROM program WHERE isMy=:isMy ORDER BY `order` ASC")
    fun getData(isMy: Boolean) : List<Program>?

    @Query("UPDATE program SET isMy=:isMy WHERE id=:id")
    fun syncPrograms(isMy: Boolean, id: Int)
}