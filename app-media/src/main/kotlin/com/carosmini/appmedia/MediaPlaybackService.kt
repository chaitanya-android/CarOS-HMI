package com.carosmini.appmedia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MediaPlaybackService : MediaBrowserServiceCompat() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
            AudioManager.AUDIOFOCUS_GAIN -> player.play()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
            if (isPlaying) startForeground(NOTIFICATION_ID, buildNotification())
            else stopForeground(false)
        }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            updateMetadata()
            updatePlaybackState()
            if (player.isPlaying) startForeground(NOTIFICATION_ID, buildNotification())
        }
        override fun onPlaybackStateChanged(state: Int) = updatePlaybackState()
    }

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            requestAudioFocus()
            player.play()
            session.isActive = true
            startService(android.content.Intent(this@MediaPlaybackService, MediaPlaybackService::class.java))
        }
        override fun onPause() {
            player.pause()
            stopForeground(false)
        }
        override fun onSkipToNext() { player.seekToNextMediaItem() }
        override fun onSkipToPrevious() { player.seekToPreviousMediaItem() }
        override fun onSeekTo(pos: Long) { player.seekTo(pos) }
        override fun onStop() {
            player.stop()
            abandonAudioFocus()
            session.isActive = false
            stopForeground(true)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()

        player = ExoPlayer.Builder(this).build().apply {
            setMediaItems(PLAYLIST.map { MediaItem.fromUri(it.uri) })
            prepare()
            addListener(playerListener)
        }

        session = MediaSessionCompat(this, TAG).apply {
            setCallback(sessionCallback)
            isActive = true
        }
        sessionToken = session.sessionToken

        updateMetadata()
        updatePlaybackState()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        session.release()
        abandonAudioFocus()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?) =
        BrowserRoot(ROOT_ID, null)

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(PLAYLIST.map { track ->
            val desc = MediaDescriptionCompat.Builder()
                .setMediaId(track.id)
                .setTitle(track.title)
                .setSubtitle(track.artist)
                .build()
            MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        })
    }

    private fun updateMetadata() {
        val track = PLAYLIST.getOrNull(player.currentMediaItemIndex) ?: return
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.duration.coerceAtLeast(-1L))
                .build()
        )
    }

    private fun updatePlaybackState() {
        val state = when {
            player.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            player.playbackState == Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, player.currentPosition, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
    }

    private fun buildNotification(): Notification {
        val track = PLAYLIST.getOrNull(player.currentMediaItemIndex)
        val isPlaying = player.isPlaying

        val prevIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
        val toggleIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        )
        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track?.title ?: "CarOSMini Media")
            .setContentText(track?.artist ?: "")
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                toggleIntent,
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun requestAudioFocus() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()
            .also { audioManager.requestAudioFocus(it) }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    companion object {
        private const val TAG = "CarOSMiniMedia"
        private const val CHANNEL_ID = "media_playback"
        private const val NOTIFICATION_ID = 1001
        const val ROOT_ID = "root"
    }
}
