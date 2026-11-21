package com.carosmini.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

private const val SERVICE_PACKAGE = "com.carosmini.core"

/**
 * Wraps the boilerplate of binding to ProfileService.
 * Usage:
 *   private val conn = ProfileServiceConnection { svc -> ... }
 *   conn.bind(context)   // in onCreate
 *   conn.unbind(context) // in onDestroy
 */
class ProfileServiceConnection(
    private val onConnected: (IProfileService) -> Unit,
) : ServiceConnection {

    var service: IProfileService? = null
        private set

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = IProfileService.Stub.asInterface(binder).also(onConnected)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    fun bind(context: Context) {
        val intent = Intent(IProfileService::class.java.name).apply {
            setPackage(SERVICE_PACKAGE)
        }
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        service = null
        context.unbindService(this)
    }
}

class PhoneConnectionServiceConnection(
    private val onConnected: (IPhoneConnectionService) -> Unit,
) : ServiceConnection {

    var service: IPhoneConnectionService? = null
        private set

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = IPhoneConnectionService.Stub.asInterface(binder).also(onConnected)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    fun bind(context: Context) {
        val intent = Intent(IPhoneConnectionService::class.java.name).apply {
            setPackage(SERVICE_PACKAGE)
        }
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        service = null
        context.unbindService(this)
    }
}
