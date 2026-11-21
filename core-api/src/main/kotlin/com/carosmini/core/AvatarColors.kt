package com.carosmini.core

import android.graphics.Color

object AvatarColors {
    val COLORS = intArrayOf(
        Color.parseColor("#F44336"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#009688"),
        Color.parseColor("#E91E63"),
    )

    fun forIndex(index: Int): Int = COLORS[index % COLORS.size]
}
