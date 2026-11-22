package com.rahulbaberwal.marg.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import org.maplibre.android.maps.Style

object MapMarkerHelper {

    fun createGooglePinBitmap(
        context: Context,
        pinColor: Int,
        badgeText: String? = null,
        widthDp: Float = 38f,
        heightDp: Float = 50f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).toInt()
        val h = (heightDp * density).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = w / 2f
        val r = (w / 2f) - (2.5f * density)
        val cy = r + (2f * density)
        val tipY = h - (4.5f * density)

        // 1. Soft Drop Shadow under pin tip
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(65, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            RectF(cx - (7f * density), h - (6f * density), cx + (7f * density), h - (0.5f * density)),
            shadowPaint
        )

        // 2. Google Maps Pin Path (Teardrop shape)
        val path = Path().apply {
            moveTo(cx, tipY)
            // Left curve
            cubicTo(
                cx - r * 0.85f, cy + r * 1.15f,
                cx - r, cy + r * 0.45f,
                cx - r, cy
            )
            // Head circle arc
            arcTo(
                RectF(cx - r, cy - r, cx + r, cy + r),
                180f,
                180f,
                false
            )
            // Right curve
            cubicTo(
                cx + r, cy + r * 0.45f,
                cx + r * 0.85f, cy + r * 1.15f,
                cx, tipY
            )
            close()
        }

        // 3. Pin Body
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, pinPaint)

        // 4. White outer border for contrast against dark/satellite tiles
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * density
        }
        canvas.drawPath(path, strokePaint)

        // 5. White Inner Badge
        val badgeRadius = r * 0.52f
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, badgeRadius, badgePaint)

        // 6. Badge Label (e.g. A, B, 1, 2, or dot)
        if (!badgeText.isNullOrEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pinColor
                textSize = badgeRadius * 1.25f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val bounds = Rect()
            textPaint.getTextBounds(badgeText, 0, badgeText.length, bounds)
            canvas.drawText(badgeText, cx, cy + (bounds.height() / 2f) - (0.5f * density), textPaint)
        } else {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pinColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, badgeRadius * 0.45f, dotPaint)
        }

        return bitmap
    }

    fun registerPinIcons(context: Context, style: Style) {
        // Start: Green with 'A'
        style.addImage("marker-start", createGooglePinBitmap(context, Color.parseColor("#16A34A"), "A"))
        // Destination: Saffron/Red with 'B'
        style.addImage("marker-end", createGooglePinBitmap(context, Color.parseColor("#E11D48"), "B"))
        // Fixed: Saffron with 'H'
        style.addImage("marker-fixed", createGooglePinBitmap(context, Color.parseColor("#FF9933"), "H"))
        // Default Waypoint: Teal
        style.addImage("marker-waypoint", createGooglePinBitmap(context, Color.parseColor("#0E7490"), "•"))

        // Waypoints 1 through 10
        for (i in 1..10) {
            style.addImage("marker-waypoint-$i", createGooglePinBitmap(context, Color.parseColor("#0E7490"), "$i"))
        }
    }
}
