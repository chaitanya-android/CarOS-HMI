package com.carosmini.appmedia

import android.content.ComponentName
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.carosmini.core.AvatarColors
import com.carosmini.core.IProfileListener
import com.carosmini.core.Profile
import com.carosmini.core.ProfileServiceConnection

class MediaActivity : AppCompatActivity() {

    private lateinit var viewArt: View
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvProfile: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnPrev: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaController: MediaControllerCompat? = null

    private var isServiceReady = false

    // Poll position every 500 ms while playing.
    private val seekHandler = Handler(Looper.getMainLooper())
    private val seekRunnable = object : Runnable {
        override fun run() {
            mediaController?.let { ctrl ->
                val dur = ctrl.metadata
                    ?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    ?.takeIf { it > 0 } ?: 0L
                if (dur > 0) {
                    seekBar.max = dur.toInt()
                    seekBar.progress = (ctrl.playbackState?.position ?: 0L).toInt()
                }
            }
            seekHandler.postDelayed(this, 500)
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(meta: MediaMetadataCompat?) {
            tvTitle.text = meta?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "—"
            tvArtist.text = meta?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "—"
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            val playing = state?.state == PlaybackStateCompat.STATE_PLAYING
            btnPlayPause.setImageResource(
                if (playing) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }
    }

    private val browserCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(
                this@MediaActivity, mediaBrowser.sessionToken
            ).also { ctrl ->
                MediaControllerCompat.setMediaController(this@MediaActivity, ctrl)
                ctrl.registerCallback(controllerCallback)
                controllerCallback.onMetadataChanged(ctrl.metadata)
                controllerCallback.onPlaybackStateChanged(ctrl.playbackState)
            }
            seekHandler.post(seekRunnable)
        }

        override fun onConnectionFailed() {
            Log.e("MEDIA", "MediaBrowser connection failed")
        }
    }

    private val profileListener = object : IProfileListener.Stub() {
        override fun onProfileChanged(profile: Profile) {
            runOnUiThread { applyProfile(profile) }
        }
    }

    private val profileConn = ProfileServiceConnection { _ ->
        isServiceReady = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        viewArt = findViewById(R.id.view_album_art)
        tvTitle = findViewById(R.id.tv_title)
        tvArtist = findViewById(R.id.tv_artist)
        tvProfile = findViewById(R.id.tv_profile)
        seekBar = findViewById(R.id.seek_bar)
        btnPrev = findViewById(R.id.btn_prev)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnNext = findViewById(R.id.btn_next)

        viewArt.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(AvatarColors.COLORS[1])
        }

        btnPrev.setOnClickListener {
            MediaControllerCompat.getMediaController(this)?.transportControls?.skipToPrevious()
        }
        btnPlayPause.setOnClickListener {
            val ctrl = MediaControllerCompat.getMediaController(this) ?: return@setOnClickListener
            if (ctrl.playbackState?.state == PlaybackStateCompat.STATE_PLAYING)
                ctrl.transportControls.pause()
            else
                ctrl.transportControls.play()
        }
        btnNext.setOnClickListener {
            MediaControllerCompat.getMediaController(this)?.transportControls?.skipToNext()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) MediaControllerCompat.getMediaController(this@MediaActivity)
                    ?.transportControls?.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            browserCallback,
            null,
        )

        profileConn.bind(this)
    }

    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    override fun onResume() {
        super.onResume()
        if (isServiceReady) {
            window.decorView.postDelayed({
                try {
                    profileConn.service?.registerProfileListener(profileListener)
                    profileConn.service?.activeProfile?.let { applyProfile(it) }
                } catch (e: DeadObjectException) {
                    Log.e("SERVICE_DEBUG", "Binder died, rebinding", e)
                    isServiceReady = false
                    profileConn.bind(this)
                }
            }, 200)
        }
    }

    override fun onStop() {
        super.onStop()
        seekHandler.removeCallbacks(seekRunnable)
        mediaController?.unregisterCallback(controllerCallback)
        MediaControllerCompat.setMediaController(this, null)
        mediaController = null
        mediaBrowser.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            profileConn.service?.unregisterProfileListener(profileListener)
        } catch (e: DeadObjectException) {
            Log.w("SERVICE_DEBUG", "Binder already dead", e)
        }
        profileConn.unbind(this)
    }

    private fun applyProfile(profile: Profile) {
        tvProfile.text = "Profile: ${profile.name}"
        (viewArt.background as? GradientDrawable)
            ?.setColor(AvatarColors.forIndex(profile.avatarResId))
    }
}
