package com.carosmini.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles") fun getAll(): Flow<List<ProfileEntity>>
    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1") suspend fun getDefault(): ProfileEntity?
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1") suspend fun getById(id: Int): ProfileEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(profile: ProfileEntity): Long
    @Update suspend fun update(profile: ProfileEntity)
    @Delete suspend fun delete(profile: ProfileEntity)
    @Query("UPDATE profiles SET isDefault = 0") suspend fun clearDefault()
    @Query("UPDATE profiles SET isDefault = 1 WHERE id = :id") suspend fun setDefault(id: Int)
}
