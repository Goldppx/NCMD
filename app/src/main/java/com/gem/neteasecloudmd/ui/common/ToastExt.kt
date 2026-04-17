package com.gem.neteasecloudmd.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast as AndroidToast

object Toast {
    const val LENGTH_SHORT: Int = AndroidToast.LENGTH_SHORT
    const val LENGTH_LONG: Int = AndroidToast.LENGTH_LONG

    fun makeText(
        context: Context,
        message: CharSequence,
        duration: Int = LENGTH_SHORT
    ): AndroidToast {
        val textView = TextView(context).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(36, 20, 36, 20)
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xCC222222.toInt())
            }
        }

        val container = LinearLayout(context).apply {
            addView(textView)
        }

        return AndroidToast(context).apply {
            this.duration = duration
            view = container
        }
    }
}

fun Context.showPlainToast(
    message: CharSequence,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showPlainToast(
    messageResId: Int,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(this, getString(messageResId), duration).show()
}
