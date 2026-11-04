package com.carosmini.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // ProfileService must start first — PhoneConnectionService binds to it on onCreate.
            context.startService(Intent(context, ProfileService::class.java))
            context.startService(Intent(context, PhoneConnectionService::class.java))
        }
    }
}
