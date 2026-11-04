package com.carosmini.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.carosmini.core.PairedDevice

@Entity(tableName = "device_profile_links")
data class DeviceLinkEntity(
    @PrimaryKey val macAddress: String,
    val displayName: String,
    val linkedProfileId: Int = -1,
)

fun DeviceLinkEntity.toParcelable() = PairedDevice(macAddress, displayName, linkedProfileId)
fun PairedDevice.toEntity() = DeviceLinkEntity(macAddress, displayName, linkedProfileId)
