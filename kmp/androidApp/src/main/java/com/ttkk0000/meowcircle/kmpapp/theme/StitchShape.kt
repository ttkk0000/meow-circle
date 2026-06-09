package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * M&D Stitch V2 radius rules for Android.
 *
 * Consumer surfaces use 16dp cards, form controls/buttons use 12dp, dialogs
 * use 20dp, and Neutral dense cards stay at 8dp.
 */
object StitchShape {
    val pill = RoundedCornerShape(999.dp)
    val cardFeed = RoundedCornerShape(16.dp)
    /** Matches [cardFeed] top corners for media clipping. */
    val cardFeedTop = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val container = RoundedCornerShape(16.dp)
    val dialog = RoundedCornerShape(20.dp)
    val field = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(12.dp)
    val neutralCard = RoundedCornerShape(8.dp)
}
