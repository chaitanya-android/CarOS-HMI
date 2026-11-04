package com.carosmini.applauncher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.carosmini.core.IProfileListener
import com.carosmini.core.Profile
import com.carosmini.core.ProfileServiceConnection

class LauncherActivity : AppCompatActivity() {

    private lateinit var tvProfile: TextView
    private lateinit var tvProfileSub: TextView

    // Set to true once onServiceConnected fires; guards the onResume fetch.
    private var isServiceReady = false

    private val profileListener = object : IProfileListener.Stub() {
        override fun onProfileChanged(profile: Profile) {
            runOnUiThread { showProfile(profile) }
        }
    }

    private val profileConn = ProfileServiceConnection { _ ->
        // No IPC here — binder may not survive any transact call this early.
        // All IPC (listener registration + profile fetch) is deferred to onResume().
        isServiceReady = true
    }

    // Bluetooth runtime permissions — only required on API 31+
    private val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        emptyArray()
    }

    private val requestBtPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Permissions resolved (granted or denied). ProfileSettingsActivity will
            // check and show a message if still missing.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        tvProfile = findViewById(R.id.tv_profile_name)
        tvProfileSub = findViewById(R.id.tv_profile_sub)

        profileConn.bind(this)
        requestMissingBtPermissions()

        findViewById<Button>(R.id.btn_media).setOnClickListener {
            startActivity(Intent().apply {
                setClassName("com.carosmini.appmedia", "com.carosmini.appmedia.MediaActivity")
            })
        }
        findViewById<Button>(R.id.btn_nav).setOnClickListener {
            startActivity(Intent().apply {
                setClassName("com.carosmini.appnav", "com.carosmini.appnav.NavActivity")
            })
        }
        findViewById<Button>(R.id.btn_dashboard).setOnClickListener {
            startActivity(Intent().apply {
                setClassName("com.carosmini.appdashboard", "com.carosmini.appdashboard.DashboardActivity")
            })
        }
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, ProfileSettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btn_manage_profiles).setOnClickListener {
            startActivity(Intent(this, ProfileManageActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (isServiceReady) {
            window.decorView.postDelayed({
                try {
                    profileConn.service?.registerProfileListener(profileListener)
                    val profile = profileConn.service?.activeProfile
                    showProfile(profile)
                } catch (e: DeadObjectException) {
                    Log.e("SERVICE_DEBUG", "Binder died, rebinding", e)
                    isServiceReady = false
                    profileConn.bind(this)
                }
            }, 200)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            profileConn.service?.unregisterProfileListener(profileListener)
        } catch (e: DeadObjectException) {
            Log.w("SERVICE_DEBUG", "Binder already dead on destroy", e)
        }
        profileConn.unbind(this)
    }

    private fun requestMissingBtPermissions() {
        val missing = btPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestBtPermissions.launch(missing.toTypedArray())
    }

    private fun showProfile(profile: Profile?) {
        tvProfile.text = profile?.name ?: "Guest"
        tvProfileSub.text = if (profile?.isDefault == true) "Default profile" else "Active profile"
    }
}
