package com.gem.neteasecloudmd.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.dynamicColorScheme
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/** Uses the current cover as the seed for the same Material Kolor system used on Android. */
@Composable
fun DesktopTheme(artworkUri: String?, darkTheme: Boolean, content: @Composable () -> Unit) {
    val seedColor by produceState<Color?>(initialValue = null, artworkUri) {
        value = withContext(Dispatchers.IO) { DesktopArtworkColorExtractor.seedColor(artworkUri) }
    }
    val targetScheme = seedColor?.let { seed ->
        dynamicColorScheme(
            seedColor = seed,
            isDark = darkTheme,
            contrastLevel = Contrast.Default.value
        )
    } ?: if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = targetScheme, content = content)
}

private object DesktopArtworkColorExtractor {
    fun seedColor(artworkUri: String?): Color? = runCatching {
        if (artworkUri.isNullOrBlank()) return null
        val image = ImageIO.read(File(URI(artworkUri))) ?: return null
        val buckets = linkedMapOf<Int, ColorBucket>()
        val horizontalStep = max(1, image.width / MAX_SAMPLE_EDGE)
        val verticalStep = max(1, image.height / MAX_SAMPLE_EDGE)

        for (x in 0 until image.width step horizontalStep) {
            for (y in 0 until image.height step verticalStep) {
                val argb = image.getRGB(x, y)
                if ((argb ushr ALPHA_SHIFT) < MIN_ALPHA) continue

                val red = (argb ushr RED_SHIFT) and COLOR_MASK
                val green = (argb ushr GREEN_SHIFT) and COLOR_MASK
                val blue = argb and COLOR_MASK
                val maximum = max(red, max(green, blue))
                val minimum = min(red, min(green, blue))
                val saturation = if (maximum == 0) 0f else (maximum - minimum).toFloat() / maximum
                val luminance = maximum / COLOR_COMPONENT_MAX.toFloat()
                if (saturation < MIN_SATURATION || luminance < MIN_LUMINANCE) continue

                val key = ((red shr QUANTIZATION_SHIFT) shl RED_BUCKET_SHIFT) or
                    ((green shr QUANTIZATION_SHIFT) shl GREEN_BUCKET_SHIFT) or
                    (blue shr QUANTIZATION_SHIFT)
                buckets.getOrPut(key) { ColorBucket() }.add(red, green, blue, saturation)
            }
        }

        buckets.values.maxByOrNull { it.score }?.toColor()
    }.getOrNull()

    private class ColorBucket {
        private var count = 0
        private var totalRed = 0L
        private var totalGreen = 0L
        private var totalBlue = 0L
        private var totalSaturation = 0f

        val score: Float
            get() = count * (0.4f + totalSaturation / count)

        fun add(red: Int, green: Int, blue: Int, saturation: Float) {
            count += 1
            totalRed += red
            totalGreen += green
            totalBlue += blue
            totalSaturation += saturation
        }

        fun toColor(): Color = Color(
            (OPAQUE_ALPHA shl ALPHA_SHIFT) or
                ((totalRed / count).toInt() shl RED_SHIFT) or
                ((totalGreen / count).toInt() shl GREEN_SHIFT) or
                (totalBlue / count).toInt()
        )
    }

    private const val MAX_SAMPLE_EDGE = 96
    private const val MIN_ALPHA = 224
    private const val MIN_SATURATION = 0.12f
    private const val MIN_LUMINANCE = 0.10f
    private const val COLOR_MASK = 0xFF
    private const val COLOR_COMPONENT_MAX = 255
    private const val OPAQUE_ALPHA = 0xFF
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val QUANTIZATION_SHIFT = 3
    private const val RED_BUCKET_SHIFT = 10
    private const val GREEN_BUCKET_SHIFT = 5
}
