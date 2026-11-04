package com.carosmini.core.db

import androidx.room.*

@Dao
interface DeviceLinkDao {
    @Query("SELECT * FROM device_profile_links") suspend fun getAll(): List<DeviceLinkEntity>
    @Query("SELECT * FROM device_profile_links WHERE macAddress = :mac LIMIT 1")
    suspend fun getByMac(mac: String): DeviceLinkEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(device: DeviceLinkEntity)
    @Query("DELETE FROM device_profile_links WHERE macAddress = :mac") suspend fun delete(mac: String)
}
