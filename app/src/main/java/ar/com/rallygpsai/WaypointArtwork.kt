package ar.com.rallygpsai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Original high-contrast map pins. Icons are anchored at the road coordinate.
 * Unlike tiny map circles, they stay visible on both dark and satellite map styles.
 */
object WaypointArtwork {
    enum class Kind(val id: String, val caption: String, val hue: Int) {
        DESTINATION("destination", "D", Color.rgb(52, 228, 153)),
        ORIGIN("origin", "O", Color.rgb(37, 211, 238)),
        STOP("stop", "+", Color.rgb(253, 204, 68))
    }
    private val cache = mutableMapOf<Kind, Bitmap>()

    fun bitmap(kind: Kind): Bitmap = cache.getOrPut(kind) {
        val bmp = Bitmap.createBitmap(96, 116, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG)
        ink.setShadowLayer(5f, 0f, 3f, Color.argb(180, 0, 0, 0))
        ink.color = Color.argb(165, 0, 0, 0)
        c.drawOval(RectF(25f, 103f, 71f, 115f), ink)
        ink.clearShadowLayer()

        val tip = Path().apply {
            moveTo(48f, 107f)
            cubicTo(37f, 85f, 15f, 64f, 15f, 43f)
            cubicTo(15f, 18f, 30f, 7f, 48f, 7f)
            cubicTo(66f, 7f, 81f, 18f, 81f, 43f)
            cubicTo(81f, 64f, 59f, 85f, 48f, 107f)
            close()
        }
        ink.color = Color.rgb(244, 248, 255)
        c.drawPath(tip, ink)
        c.save()
        c.scale(0.86f, 0.85f, 48f, 51f)
        ink.color = kind.hue
        c.drawPath(tip, ink)
        c.restore()
        ink.color = Color.rgb(9, 20, 31)
        c.drawCircle(48f, 42f, 23f, ink)
        ink.color = Color.WHITE
        ink.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        ink.textSize = 29f
        ink.textAlign = Paint.Align.CENTER
        c.drawText(kind.caption, 48f, 52f, ink)
        bmp
    }
}
