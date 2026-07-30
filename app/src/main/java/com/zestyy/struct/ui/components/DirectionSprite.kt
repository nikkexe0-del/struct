package com.zestyy.struct.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Small orange direction-arrow sprite drawn in code (no asset needed) — points "up" at rotation
 * 0, `Marker.rotation` handles the actual heading. Used as the live "you are here" indicator on
 * the tracking and follow-route maps, in place of a plain dot, per the request that the marker
 * should visibly point the way you're moving, à la Strava/Google Maps' blue arrow.
 */
fun directionSpriteDrawable(context: Context): Drawable {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    // soft outer halo so it reads clearly against both light and dark map tiles
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 255, 122, 26)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size * 0.42f, haloPaint)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size * 0.30f, ringPaint)

    // orange arrow/chevron pointing "up" (north) — rotated at runtime via Marker.rotation
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 122, 26)
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(cx, cy - size * 0.24f)               // tip
        lineTo(cx + size * 0.16f, cy + size * 0.14f) // bottom-right
        lineTo(cx, cy + size * 0.04f)                // notch
        lineTo(cx - size * 0.16f, cy + size * 0.14f) // bottom-left
        close()
    }
    canvas.drawPath(path, arrowPaint)

    return BitmapDrawable(context.resources, bitmap)
}
