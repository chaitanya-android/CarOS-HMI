package com.carosmini.core

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteCallbackList
import com.carosmini.core.db.CarOsDatabase
import com.carosmini.core.db.toEntity
import com.carosmini.core.db.toParcelable
import kotlinx.coroutines.*

class PhoneConnectionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = RemoteCallbackList<IPhoneConnectionListener>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var disconnectRunnable: Runnable? = null
    private var currentDevice: PairedDevice? = null

    // In-process binding to ProfileService — same package, no IPC overhead.
    private var profileService: IProfileService? = null
    private val profileServiceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            profileService = IProfileService.Stub.asInterface(binder)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            profileService = null
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            val device: BluetoothDevice =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> handleConnected(device)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleDisconnected(device)
            }
        }
    }

    private val binder = object : IPhoneConnectionService.Stub() {

        override fun getPairedDevices(): List<PairedDevice> = runBlocking(Dispatchers.IO) {
            CarOsDatabase.getInstance(this@PhoneConnectionService)
                .deviceLinkDao().getAll().map { it.toParcelable() }
        }

        override fun linkDeviceToProfile(macAddress: String, displayName: String, profileId: Int) {
            scope.launch {
                val db = CarOsDatabase.getInstance(this@PhoneConnectionService)
                val updated = (db.deviceLinkDao().getByMac(macAddress)?.toParcelable()
                    ?: PairedDevice(macAddress = macAddress, displayName = displayName))
                    .copy(displayName = displayName, linkedProfileId = profileId)
                db.deviceLinkDao().upsert(updated.toEntity())
            }
        }

        override fun getCurrentlyConnectedDevice(): PairedDevice? = currentDevice

        override fun registerConnectionListener(listener: IPhoneConnectionListener?) {
            listeners.register(listener)
        }

        override fun unregisterConnectionListener(listener: IPhoneConnectionListener?) {
            listeners.unregister(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(btReceiver, filter)

        bindService(
            Intent(IProfileService::class.java.name).apply { setPackage("com.carosmini.core") },
            profileServiceConn,
            BIND_AUTO_CREATE,
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btReceiver)
        unbindService(profileServiceConn)
        scope.cancel()
        listeners.kill()
    }

    private fun handleConnected(btDevice: BluetoothDevice) {
        disconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        disconnectRunnable = null

        scope.launch {
            val db = CarOsDatabase.getInstance(this@PhoneConnectionService)
            val linked = db.deviceLinkDao().getByMac(btDevice.address)?.toParcelable()
                ?: PairedDevice(macAddress = btDevice.address, displayName = btDevice.address)
            currentDevice = linked

            if (linked.linkedProfileId != -1) {
                profileService?.setActiveProfile(linked.linkedProfileId)
            }

            val count = listeners.beginBroadcast()
            repeat(count) { i -> listeners.getBroadcastItem(i).onDeviceConnected(linked) }
            listeners.finishBroadcast()
        }
    }

    private fun handleDisconnected(btDevice: BluetoothDevice) {
        val device = currentDevice ?: return
        if (device.macAddress != btDevice.address) return

        disconnectRunnable = Runnable {
            val snapshot = device
            currentDevice = null

            scope.launch {
                // Revert to whichever profile is marked default in the DB.
                val db = CarOsDatabase.getInstance(this@PhoneConnectionService)
                val defaultProfile = db.profileDao().getDefault()
                if (defaultProfile != null) {
                    profileService?.setActiveProfile(defaultProfile.id)
                }

                val count = listeners.beginBroadcast()
                repeat(count) { i -> listeners.getBroadcastItem(i).onDeviceDisconnected(snapshot) }
                listeners.finishBroadcast()
            }
        }.also { mainHandler.postDelayed(it, 30_000L) }
    }
}
