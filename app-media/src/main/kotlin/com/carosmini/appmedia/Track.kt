package com.carosmini.appmedia

data class Track(val id: String, val title: String, val artist: String, val uri: String)

// Royalty-free demo tracks — swap for local content on real hardware.
val PLAYLIST = listOf(
    Track("1", "SoundHelix Song 1", "SoundHelix", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
    Track("2", "SoundHelix Song 2", "SoundHelix", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
    Track("3", "SoundHelix Song 3", "SoundHelix", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
)
