package com.gem.neteasecloudmd.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Height reserved by scrollable content while the global mini player is visible. */
val LocalPlaybackBarInset = staticCompositionLocalOf<Dp> { 0.dp }
