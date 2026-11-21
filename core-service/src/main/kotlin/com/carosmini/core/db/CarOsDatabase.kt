package com.carosmini.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, DeviceLinkEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CarOsDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun deviceLinkDao(): DeviceLinkDao

    companion object {
        @Volatile private var instance: CarOsDatabase? = null

        fun getInstance(context: Context): CarOsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CarOsDatabase::class.java,
                    "caros.db",
                ).build().also { instance = it }
            }
    }
}
