package com.baconnish.gobuddy.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.baconnish.gobuddy.data.BuddyLevel

fun buddyTierColor(level: BuddyLevel): Color = when (level) {
    BuddyLevel.BUDDY -> Color(0xFF9E9E9E)
    BuddyLevel.GOOD -> Color(0xFF78909C)
    BuddyLevel.GREAT -> Color(0xFF42A5F5)
    BuddyLevel.ULTRA -> Color(0xFFFFB300)
    BuddyLevel.BEST -> Color(0xFFE91E63)
}

fun buddyTierIcon(level: BuddyLevel): ImageVector =
    if (level == BuddyLevel.BUDDY) Icons.Default.FavoriteBorder else Icons.Default.Favorite
