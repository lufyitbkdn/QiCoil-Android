package com.Meditation.Sounds.frequencies.lemeor.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.Meditation.Sounds.frequencies.lemeor.data.model.Category

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Category>?)

    @Query("UPDATE category SET isShow=:isShow WHERE id=:id")
    suspend fun updateShowStatus(isShow: Boolean, id: Int)

    @Query("UPDATE category SET isPurchased=:isPurchased WHERE id=:id")
    suspend fun updatePurchaseStatus(isPurchased: Boolean, id: Int)

    @Query("DELETE FROM category")
    suspend fun clear()

    @Delete
    suspend fun delete(category: Category)

    @Delete
    suspend fun deleteCategories(list: List<Category>?)

    @Query("SELECT * FROM category WHERE isShow=:isShow ORDER BY `order` ASC")
    fun getCategories(isShow: Boolean) : LiveData<List<Category>>

    @Query("SELECT * FROM category WHERE tier_id=:tierId ORDER BY `order` ASC")
    fun getCategoriesByTierId(tierId: Int) : LiveData<List<Category>>

    @Query("SELECT name FROM category WHERE id=:categoryId ORDER BY `order` ASC")
    suspend fun getCategoryNameById(categoryId: Int) : String?

    @Query("SELECT * FROM category ORDER BY `order` ASC")
    suspend fun getData() : List<Category>
}