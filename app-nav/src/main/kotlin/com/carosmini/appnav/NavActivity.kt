package com.carosmini.appnav

import android.os.Bundle
import android.os.DeadObjectException
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.carosmini.core.IProfileListener
import com.carosmini.core.Profile
import com.carosmini.core.ProfileServiceConnection

class NavActivity : AppCompatActivity() {

    private lateinit var tvProfile: TextView
    private var isServiceReady = false

    private val profileListener = object : IProfileListener.Stub() {
        override fun onProfileChanged(profile: Profile) {
            runOnUiThread { tvProfile.text = "Profile: ${profile.name}" }
        }
    }

    private val profileConn = ProfileServiceConnection { _ ->
        isServiceReady = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        tvProfile = findViewById(R.id.tv_profile)
        profileConn.bind(this)
    }

    override fun onResume() {
        super.onResume()
        if (isServiceReady) {
            window.decorView.postDelayed({
                try {
                    profileConn.service?.registerProfileListener(profileListener)
                    val profile = profileConn.service?.activeProfile
                    tvProfile.text = "Profile: ${profile?.name ?: "Guest"}"
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
}
