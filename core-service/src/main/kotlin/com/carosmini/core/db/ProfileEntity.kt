package com.carosmini.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.carosmini.core.Profile

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarResId: Int = 0,
    val isDefault: Boolean = false,
)

fun ProfileEntity.toParcelable() = Profile(id, name, avatarResId, isDefault)
fun Profile.toEntity() = ProfileEntity(id, name, avatarResId, isDefault)
