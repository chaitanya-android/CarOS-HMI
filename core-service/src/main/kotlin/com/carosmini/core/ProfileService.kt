package com.carosmini.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import com.carosmini.core.db.CarOsDatabase
import com.carosmini.core.db.ProfileEntity
import com.carosmini.core.db.toEntity
import com.carosmini.core.db.toParcelable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class ProfileService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = RemoteCallbackList<IProfileListener>()
    private var activeProfile: Profile? = null

    private val binder = object : IProfileService.Stub() {

        override fun getProfiles(): List<Profile> = runBlocking(Dispatchers.IO) {
            CarOsDatabase.getInstance(this@ProfileService).profileDao().getAll().first()
                .map { it.toParcelable() }
        }

        override fun getActiveProfile(): Profile? = activeProfile

        override fun setActiveProfile(profileId: Int) {
            scope.launch {
                val db = CarOsDatabase.getInstance(this@ProfileService)
                val entity = db.profileDao().getById(profileId) ?: return@launch
                db.profileDao().clearDefault()
                db.profileDao().setDefault(profileId)
                this@ProfileService.activeProfile = entity.toParcelable()
                notifyListeners(this@ProfileService.activeProfile!!)
            }
        }

        override fun createProfile(profile: Profile) {
            scope.launch {
                CarOsDatabase.getInstance(this@ProfileService).profileDao().insert(profile.toEntity())
            }
        }

        override fun updateProfile(profile: Profile) {
            scope.launch {
                val db = CarOsDatabase.getInstance(this@ProfileService)
                val existing = db.profileDao().getById(profile.id) ?: return@launch
                db.profileDao().update(existing.copy(name = profile.name, avatarResId = profile.avatarResId))
            }
        }

        override fun deleteProfile(profileId: Int) {
            scope.launch {
                val db = CarOsDatabase.getInstance(this@ProfileService)
                db.profileDao().getById(profileId)?.let { db.profileDao().delete(it) }
            }
        }

        override fun registerProfileListener(listener: IProfileListener?) {
            listeners.register(listener)
        }

        override fun unregisterProfileListener(listener: IProfileListener?) {
            listeners.unregister(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Block until the DB is ready so any binder call that arrives immediately
        // after onBind() sees a non-empty profile list. This is a background service
        // with no UI, so a brief main-thread block here is safe.
        runBlocking(Dispatchers.IO) {
            val db = CarOsDatabase.getInstance(this@ProfileService)
            val existing = db.profileDao().getDefault()
            activeProfile = if (existing != null) {
                existing.toParcelable()
            } else {
                val guest = ProfileEntity(name = "Guest", isDefault = true)
                val id = db.profileDao().insert(guest).toInt()
                guest.copy(id = id).toParcelable()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        listeners.kill()
    }

    private fun notifyListeners(profile: Profile) {
        val count = listeners.beginBroadcast()
        repeat(count) { i -> listeners.getBroadcastItem(i).onProfileChanged(profile) }
        listeners.finishBroadcast()
    }
}
