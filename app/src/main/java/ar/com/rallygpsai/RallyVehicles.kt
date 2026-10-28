package ar.com.rallygpsai

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/**
 * Original top-down vehicle markers, all facing north (hood at top).
 * Artwork is drawn on-device: works offline without external image services or trademarks.
 */
object RallyVehicles {
    const val KEY = "vehicle_id"
    const val DEFAULT = "subaru"
    const val DOT = "dot"

    enum class Body { RALLY, HATCH, SEDAN, PICKUP, SUV, OFFROAD, UTV }
    data class Model(
        val id: String, val name: String, val group: String,
        val body: Body, val color: String, val accent: String,
        val number: Int = 0
    )

    val models: List<Model> = listOf(
        Model("subaru", "Subaru WRX STI", "Rally", Body.RALLY, "#145BFA", "#F9D537", 5),
        Model("lancer", "Mitsubishi Lancer Evo", "Rally", Body.RALLY, "#E83232", "#F4F8FA", 7),
        Model("yaris", "Toyota GR Yaris", "Rally", Body.HATCH, "#EDF1F5", "#E83932", 8),
        Model("fiesta", "Ford Fiesta R5", "Rally", Body.HATCH, "#1A5CCE", "#E8F1FF", 2),
        Model("polo", "VW Polo Rally", "Rally", Body.HATCH, "#E7F0FF", "#1868DE", 6),
        Model("peugeot", "Peugeot 208 Rally4", "Rally", Body.HATCH, "#EDEFF4", "#D73B41", 9),
        Model("clio", "Renault Clio Rally", "Rally", Body.HATCH, "#F5B52D", "#15243D", 4),
        Model("skoda", "Skoda Fabia Rally2", "Rally", Body.HATCH, "#62C83F", "#F4F7F6", 3),
        Model("citroen", "Citroën C3 Rally", "Rally", Body.HATCH, "#E93C3B", "#F5F8FC", 1),
        Model("hyundai", "Hyundai i20 N Rally", "Rally", Body.HATCH, "#167BDF", "#F7CD37", 12),
        Model("civic", "Honda Civic Type R", "Calle", Body.HATCH, "#F1F2F6", "#D83245"),
        Model("golf", "VW Golf GTI", "Calle", Body.HATCH, "#E13934", "#1C1F27"),
        Model("focus", "Ford Focus RS", "Calle", Body.HATCH, "#1682DE", "#1C2635"),
        Model("sandero", "Renault Sandero RS", "Calle", Body.HATCH, "#EEF1F6", "#35383E"),
        Model("onix", "Chevrolet Onix", "Calle", Body.HATCH, "#E94036", "#1F2637"),
        Model("palio", "Fiat Palio", "Calle", Body.HATCH, "#EFF0ED", "#E63C35"),
        Model("hilux", "Toyota Hilux GR", "Pickup", Body.PICKUP, "#E9ECEF", "#D73738"),
        Model("ranger", "Ford Ranger Raptor", "Pickup", Body.PICKUP, "#1762B5", "#303841"),
        Model("amarok", "VW Amarok", "Pickup", Body.PICKUP, "#ADB7C5", "#28344A"),
        Model("renegade", "Jeep Renegade", "4x4", Body.SUV, "#D5D5C9", "#293843"),
        Model("niva", "Lada Niva", "4x4", Body.OFFROAD, "#E5A730", "#1A2C37"),
        Model("jimny", "Suzuki Jimny", "4x4", Body.OFFROAD, "#ADDD32", "#26343A"),
        Model("utv", "UTV todoterreno", "Especial", Body.UTV, "#E4A037", "#222D3C"),
        Model(DOT, "Punto GPS clásico", "Clásico", Body.SEDAN, "#22D3EE", "#FFFFFF")
    )

    private val cache = mutableMapOf<String, Bitmap>()

    fun selectedId(preferences: SharedPreferences): String {
        val legacy = if (preferences.getBoolean("subaru_marker", true)) DEFAULT else DOT
        val stored = preferences.getString(KEY, legacy) ?: DEFAULT
        return if (models.any { it.id == stored }) stored else DEFAULT
    }

    fun get(id: String): Model = models.firstOrNull { it.id == id } ?: models.first()

    fun marker(id: String): Bitmap {
        val valid = get(id).id
        return cache.getOrPut(valid) {
            if (valid == DEFAULT) SubaruMarker.bitmap() ?: draw(get(valid), 96, 144)
            else draw(get(valid), 96, 144)
        }
    }

    fun preview(id: String): Bitmap =
        if (id == DEFAULT) marker(DEFAULT) else marker(id)

    /** All drawings point forward toward the top edge, for direct map-bearing rotation. */
    private fun draw(model: Model, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(width / 100f, height / 150f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun fill(color: Int) { paint.color = color; paint.style = Paint.Style.FILL; paint.shader = null }
        fun rounded(x0: Float, y0: Float, x1: Float, y1: Float, radius: Float, color: Int) {
            fill(color)
            canvas.drawRoundRect(RectF(x0, y0, x1, y1), radius, radius, paint)
        }
        fun colour(hex: String) = Color.parseColor(hex)
        val bodyColour = colour(model.color)
        val accent = colour(model.accent)
        val outline = Color.rgb(10, 15, 28)
        val glass = Color.rgb(23, 43, 63)
        val reflect = Color.argb(185, 115, 179, 214)
        val isPickup = model.body == Body.PICKUP
        val isOffroad = model.body == Body.OFFROAD || model.body == Body.SUV
        val isUtv = model.body == Body.UTV
        val cabinEnd = if (isPickup) 84f else if (isUtv) 108f else 105f
        val side = if (isOffroad || isPickup) 19f else 23f
        val nose = if (isOffroad || isPickup) 9f else 13f

        if (model.id == DOT) {
            fill(Color.argb(75, 34, 211, 238))
            canvas.drawCircle(50f, 75f, 37f, paint)
            fill(Color.WHITE)
            canvas.drawCircle(50f, 75f, 27f, paint)
            fill(bodyColour)
            canvas.drawCircle(50f, 75f, 20f, paint)
            return bmp
        }

        // Ground shadow and four clearly separated tyres.
        rounded(side - 5, 8f, 105f - side, 146f, 16f, Color.argb(110, 0, 0, 0))
        listOf(27f, 103f).forEach { y ->
            rounded(side - 11, y, side + 1, y + 24, 5f, outline)
            rounded(99f - side, y, 111f - side, y + 24, 5f, outline)
            rounded(side - 9, y + 4, side - 6, y + 20, 2f, Color.rgb(83, 91, 103))
            rounded(106f - side, y + 4, 109f - side, y + 20, 2f, Color.rgb(83, 91, 103))
        }

        val main = RectF(side, nose, 100f - side, 139f)
        rounded(main.left - 2, main.top - 2, main.right + 2, main.bottom + 2, 14f, outline)
        paint.color = bodyColour
        paint.shader = LinearGradient(25f, 15f, 75f, 140f, intArrayOf(
            lighten(bodyColour, 0.24f), bodyColour, darken(bodyColour, 0.18f)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(main, if (isOffroad) 9f else 13f, if (isOffroad) 9f else 13f, paint)
        paint.shader = null

        // Hood at top (front of vehicle), bumper and unmistakable headlights.
        rounded(side + 4, nose + 5, 96f - side, nose + 10, 3f, Color.argb(130, 255, 255, 255))
        rounded(side + 3, nose + 11, side + 15, nose + 19, 2f, Color.rgb(244, 241, 194))
        rounded(85f - side, nose + 11, 97f - side, nose + 19, 2f, Color.rgb(244, 241, 194))
        rounded(38f, nose + 7, 62f, nose + 11, 3f, outline)

        // Sport bonnet stripe. For other cars a subtle colored centre accent.
        rounded(44f, 25f, 56f, 43f, 3f, accent)
        rounded(47f, 27f, 53f, 42f, 2f, Color.argb(100, 255, 255, 255))

        // Dark angled windscreen, roof and rear window.
        rounded(side + 6, 46f, 94f - side, 64f, 5f, outline)
        rounded(side + 9, 48f, 91f - side, 61f, 4f, glass)
        rounded(side + 11, 48f, 91f - side, 50f, 1.5f, reflect)
        rounded(side + 8, 65f, 92f - side, cabinEnd - 5f, 6f, darken(bodyColour, 0.09f))

        if (isPickup) {
            // Pickup-specific open cargo bed separated from cabin.
            rounded(side + 5, 87f, 95f - side, 129f, 3f, outline)
            rounded(side + 8, 91f, 92f - side, 126f, 2f, darken(bodyColour, 0.35f))
            rounded(side + 8, 93f, 92f - side, 97f, 1f, Color.argb(100, 255, 255, 255))
            rounded(side + 8, 120f, 92f - side, 124f, 1f, Color.argb(80, 255, 255, 255))
        } else if (isUtv) {
            // Lightweight buggy rollcage and exposed cabin.
            rounded(27f, 58f, 73f, 106f, 5f, outline)
            rounded(33f, 65f, 67f, 100f, 3f, darken(bodyColour, 0.40f))
            rounded(29f, 69f, 71f, 73f, 2f, accent)
            rounded(29f, 97f, 71f, 101f, 2f, accent)
        } else {
            rounded(side + 8, 105f, 92f - side, 124f, 5f, outline)
            rounded(side + 11, 107f, 89f - side, 120f, 3f, glass)
            rounded(side + 12, 107f, 88f - side, 109f, 1f, reflect)
            // rally diagonal flashes along both sides
            if (model.body == Body.RALLY || model.number > 0) {
                val path = Path()
                path.moveTo(side + 2, 77f)
                path.lineTo(side + 12, 73f)
                path.lineTo(side + 12, 111f)
                path.lineTo(side + 2, 118f)
                path.close()
                fill(accent)
                canvas.drawPath(path, paint)
                val right = Path()
                right.moveTo(98f - side, 77f)
                right.lineTo(88f - side, 73f)
                right.lineTo(88f - side, 111f)
                right.lineTo(98f - side, 118f)
                right.close()
                canvas.drawPath(right, paint)
            }
        }
        rounded(side + 6, 133f, side + 19, 138f, 2f, Color.rgb(247, 64, 60))
        rounded(81f - side, 133f, 94f - side, 138f, 2f, Color.rgb(247, 64, 60))

        if (model.body == Body.RALLY) {
            rounded(side - 4, 128f, 104f - side, 134f, 2f, outline)
        }
        if (model.number > 0) {
            paint.typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 14f
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.WHITE
            canvas.drawText(model.number.toString(), 50f, 90f, paint)
        }
        return bmp
    }

    private fun lighten(color: Int, factor: Float) = Color.rgb(
        (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255))
    private fun darken(color: Int, factor: Float) = Color.rgb(
        (Color.red(color) * (1 - factor)).toInt(),
        (Color.green(color) * (1 - factor)).toInt(),
        (Color.blue(color) * (1 - factor)).toInt())
}
