package it.nivora.launcher

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import java.util.Locale
import kotlin.math.min

/**
 * Original Nivora Mono glyphs.
 *
 * The pack deliberately uses one coherent, restrained visual language rather than
 * tinting arbitrary vendor artwork (which often contains its own dark background).
 * Known app families receive a meaningful symbol; unknown apps get a clean monogram.
 */
internal class NivoraIconDrawable(
    label: String,
    packageName: String,
    private val glyphColor: Int
) : Drawable() {
    private enum class Glyph {
        SHOPPING, TV, ANDROID, ANIMAL, MUSIC, FOLDER, SHIELD, MAIL, AUTH, CAR,
        BOLT, WALLET, BROWSER, BITCOIN, CALCULATOR, CALENDAR, CAMERA, AI, ID,
        PHONE, CONTACTS, MESSAGE, VIDEO, MAP, SETTINGS, CLOCK, PHOTOS, BANK,
        CLOUD, CODE, HEALTH, BOOK, GENERIC
    }

    private val appLabel = label.trim()
    private val glyph = identify(appLabel.lowercase(Locale.ROOT), packageName.lowercase(Locale.ROOT))
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(31, 35, 39) }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(62, 69, 75)
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
    }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = glyphColor
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var drawableAlpha = 255

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val save = canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        canvas.scale(b.width() / 100f, b.height() / 100f)

        val tile = RectF(5f, 5f, 95f, 95f)
        backgroundPaint.alpha = drawableAlpha
        edgePaint.alpha = drawableAlpha
        ink.alpha = drawableAlpha
        canvas.drawRoundRect(tile, 27f, 27f, backgroundPaint)
        canvas.drawRoundRect(tile, 27f, 27f, edgePaint)

        when (glyph) {
            Glyph.CLOUD -> {
                val p = Path().apply {
                    moveTo(23f, 65f)
                    cubicTo(18f, 60f, 20f, 50f, 29f, 48f)
                    cubicTo(30f, 35f, 40f, 30f, 49f, 36f)
                    cubicTo(59f, 25f, 76f, 34f, 74f, 47f)
                    cubicTo(84f, 48f, 85f, 60f, 78f, 65f)
                    close()
                }
                canvas.drawPath(p, ink)
                line(canvas, 31f, 66f, 69f, 66f)
            }
            Glyph.CODE -> {
                line(canvas, 40f, 34f, 27f, 50f)
                line(canvas, 27f, 50f, 40f, 66f)
                line(canvas, 60f, 34f, 73f, 50f)
                line(canvas, 73f, 50f, 60f, 66f)
                line(canvas, 56f, 29f, 44f, 71f)
            }
            Glyph.HEALTH -> {
                canvas.drawRoundRect(RectF(18f, 22f, 82f, 78f), 12f, 12f, ink)
                line(canvas, 50f, 34f, 50f, 66f)
                line(canvas, 34f, 50f, 66f, 50f)
            }
            Glyph.BOOK -> {
                val left = Path().apply {
                    moveTo(49f, 31f)
                    cubicTo(40f, 25f, 28f, 25f, 20f, 30f)
                    lineTo(20f, 72f)
                    cubicTo(30f, 68f, 40f, 69f, 49f, 76f)
                    close()
                }
                val right = Path().apply {
                    moveTo(51f, 31f)
                    cubicTo(60f, 25f, 72f, 25f, 80f, 30f)
                    lineTo(80f, 72f)
                    cubicTo(70f, 68f, 60f, 69f, 51f, 76f)
                    close()
                }
                canvas.drawPath(left, ink)
                canvas.drawPath(right, ink)
                line(canvas, 50f, 31f, 50f, 76f)
            }
            Glyph.SHOPPING -> {
                canvas.drawRoundRect(RectF(27f, 38f, 73f, 76f), 7f, 7f, ink)
                val p = Path().apply { moveTo(38f, 40f); cubicTo(38f, 18f, 62f, 18f, 62f, 40f) }
                canvas.drawPath(p, ink)
            }
            Glyph.TV -> {
                canvas.drawRoundRect(RectF(20f, 25f, 80f, 67f), 7f, 7f, ink)
                line(canvas, 35f, 77f, 65f, 77f); line(canvas, 50f, 67f, 50f, 77f)
                line(canvas, 36f, 15f, 46f, 23f); line(canvas, 64f, 15f, 54f, 23f)
            }
            Glyph.ANDROID -> {
                val p = Path().apply { moveTo(25f, 42f); quadTo(25f, 23f, 50f, 23f); quadTo(75f, 23f, 75f, 42f); close() }
                canvas.drawPath(p, ink); line(canvas, 34f, 20f, 27f, 12f); line(canvas, 66f, 20f, 73f, 12f)
                line(canvas, 24f, 45f, 24f, 66f); line(canvas, 76f, 45f, 76f, 66f)
                line(canvas, 37f, 66f, 37f, 79f); line(canvas, 63f, 66f, 63f, 79f)
                dot(canvas, 40f, 35f, 2.7f, true); dot(canvas, 60f, 35f, 2.7f, true)
            }
            Glyph.ANIMAL -> {
                dot(canvas, 35f, 32f, 8f); dot(canvas, 50f, 25f, 8f); dot(canvas, 65f, 32f, 8f)
                dot(canvas, 27f, 47f, 7f); dot(canvas, 73f, 47f, 7f)
                val p = Path().apply { moveTo(30f, 62f); cubicTo(30f, 45f, 43f, 44f, 50f, 52f); cubicTo(57f, 44f, 70f, 45f, 70f, 62f); cubicTo(67f, 79f, 33f, 79f, 30f, 62f); close() }
                ink.style = Paint.Style.FILL; canvas.drawPath(p, ink); ink.style = Paint.Style.STROKE
            }
            Glyph.MUSIC -> {
                line(canvas, 60f, 26f, 60f, 63f); line(canvas, 60f, 27f, 76f, 22f); line(canvas, 76f, 22f, 76f, 57f)
                dot(canvas, 51f, 67f, 9f); dot(canvas, 67f, 61f, 9f)
            }
            Glyph.FOLDER -> {
                val p = Path().apply { moveTo(19f, 31f); lineTo(42f, 31f); lineTo(49f, 39f); lineTo(81f, 39f); lineTo(81f, 73f); lineTo(19f, 73f); close() }
                canvas.drawPath(p, ink)
            }
            Glyph.SHIELD -> {
                val p = Path().apply { moveTo(50f, 18f); lineTo(76f, 28f); lineTo(73f, 54f); quadTo(68f, 70f, 50f, 82f); quadTo(32f, 70f, 27f, 54f); lineTo(24f, 28f); close() }
                canvas.drawPath(p, ink); line(canvas, 38f, 49f, 46f, 57f); line(canvas, 46f, 57f, 63f, 39f)
            }
            Glyph.MAIL -> {
                canvas.drawRoundRect(RectF(18f, 28f, 82f, 72f), 6f, 6f, ink)
                line(canvas, 20f, 33f, 50f, 55f); line(canvas, 50f, 55f, 80f, 33f)
            }
            Glyph.AUTH -> {
                dot(canvas, 36f, 42f, 14f)
                line(canvas, 48f, 51f, 72f, 75f); line(canvas, 61f, 64f, 70f, 55f); line(canvas, 69f, 72f, 78f, 63f)
            }
            Glyph.CAR -> {
                val p = Path().apply { moveTo(22f, 58f); lineTo(29f, 40f); quadTo(31f, 35f, 38f, 35f); lineTo(63f, 35f); quadTo(69f, 35f, 72f, 42f); lineTo(79f, 58f); lineTo(79f, 68f); lineTo(21f, 68f); close() }
                canvas.drawPath(p, ink); dot(canvas, 32f, 68f, 5f); dot(canvas, 68f, 68f, 5f)
                line(canvas, 34f, 47f, 66f, 47f)
            }
            Glyph.BOLT -> {
                val p = Path().apply { moveTo(56f, 17f); lineTo(31f, 53f); lineTo(48f, 53f); lineTo(42f, 82f); lineTo(69f, 44f); lineTo(52f, 44f); close() }
                canvas.drawPath(p, ink)
            }
            Glyph.WALLET -> {
                canvas.drawRoundRect(RectF(19f, 28f, 81f, 75f), 8f, 8f, ink)
                canvas.drawRoundRect(RectF(53f, 42f, 82f, 60f), 5f, 5f, ink)
                dot(canvas, 63f, 51f, 2.5f, true)
            }
            Glyph.BROWSER -> {
                dot(canvas, 50f, 50f, 29f)
                canvas.drawOval(RectF(36f, 21f, 64f, 79f), ink)
                line(canvas, 22f, 42f, 78f, 42f); line(canvas, 22f, 58f, 78f, 58f)
            }
            Glyph.BITCOIN -> {
                dot(canvas, 50f, 50f, 29f)
                text(canvas, "B", 50f, 66f, 43f)
                line(canvas, 43f, 19f, 43f, 31f); line(canvas, 57f, 19f, 57f, 31f)
                line(canvas, 43f, 69f, 43f, 81f); line(canvas, 57f, 69f, 57f, 81f)
            }
            Glyph.CALCULATOR -> {
                canvas.drawRoundRect(RectF(25f, 17f, 75f, 83f), 7f, 7f, ink)
                canvas.drawRoundRect(RectF(33f, 26f, 67f, 39f), 2f, 2f, ink)
                for (y in listOf(50f, 63f, 76f)) for (x in listOf(36f, 50f, 64f)) dot(canvas, x, y, 2.5f, true)
            }
            Glyph.CALENDAR -> {
                canvas.drawRoundRect(RectF(20f, 24f, 80f, 77f), 7f, 7f, ink)
                line(canvas, 20f, 40f, 80f, 40f); line(canvas, 35f, 17f, 35f, 31f); line(canvas, 65f, 17f, 65f, 31f)
                for (y in listOf(51f, 63f)) for (x in listOf(36f, 50f, 64f)) dot(canvas, x, y, 2.1f, true)
            }
            Glyph.CAMERA -> {
                val p = Path().apply { moveTo(23f, 34f); lineTo(37f, 34f); lineTo(43f, 26f); lineTo(58f, 26f); lineTo(64f, 34f); lineTo(77f, 34f); quadTo(82f, 34f, 82f, 40f); lineTo(82f, 70f); quadTo(82f, 76f, 76f, 76f); lineTo(24f, 76f); quadTo(18f, 76f, 18f, 70f); lineTo(18f, 40f); quadTo(18f, 34f, 23f, 34f); close() }
                canvas.drawPath(p, ink); dot(canvas, 50f, 54f, 14f)
            }
            Glyph.AI -> {
                dot(canvas, 50f, 50f, 8f, true)
                for (i in 0 until 6) {
                    val angle = Math.PI * i / 3.0
                    val x = 50f + kotlin.math.cos(angle).toFloat() * 23f
                    val y = 50f + kotlin.math.sin(angle).toFloat() * 23f
                    line(canvas, 50f, 50f, x, y); dot(canvas, x, y, 5f)
                }
            }
            Glyph.ID -> {
                canvas.drawRoundRect(RectF(17f, 28f, 83f, 72f), 6f, 6f, ink)
                dot(canvas, 35f, 44f, 7f); canvas.drawArc(RectF(26f, 51f, 44f, 66f), 180f, 180f, false, ink)
                line(canvas, 52f, 43f, 73f, 43f); line(canvas, 52f, 54f, 72f, 54f); line(canvas, 52f, 64f, 66f, 64f)
            }
            Glyph.PHONE -> {
                val p = Path().apply { moveTo(34f, 23f); lineTo(45f, 35f); lineTo(39f, 44f); quadTo(47f, 58f, 59f, 62f); lineTo(66f, 55f); lineTo(79f, 65f); lineTo(76f, 77f); quadTo(72f, 83f, 64f, 79f); quadTo(35f, 67f, 22f, 39f); quadTo(18f, 31f, 25f, 25f); close() }
                canvas.drawPath(p, ink)
            }
            Glyph.CONTACTS -> {
                dot(canvas, 50f, 36f, 12f); val p = Path().apply { moveTo(23f, 77f); cubicTo(23f, 53f, 77f, 53f, 77f, 77f) }; canvas.drawPath(p, ink)
            }
            Glyph.MESSAGE -> {
                val p = Path().apply { moveTo(22f, 25f); quadTo(18f, 25f, 18f, 32f); lineTo(18f, 61f); quadTo(18f, 68f, 25f, 68f); lineTo(36f, 68f); lineTo(30f, 80f); lineTo(49f, 68f); lineTo(75f, 68f); quadTo(82f, 68f, 82f, 61f); lineTo(82f, 32f); quadTo(82f, 25f, 75f, 25f); close() }
                canvas.drawPath(p, ink); dot(canvas, 35f, 47f, 2f, true); dot(canvas, 50f, 47f, 2f, true); dot(canvas, 65f, 47f, 2f, true)
            }
            Glyph.VIDEO -> {
                canvas.drawRoundRect(RectF(18f, 29f, 66f, 71f), 7f, 7f, ink)
                val p = Path().apply { moveTo(67f, 41f); lineTo(82f, 32f); lineTo(82f, 68f); lineTo(67f, 59f); close() }
                canvas.drawPath(p, ink)
            }
            Glyph.MAP -> {
                val p = Path().apply { moveTo(50f, 82f); cubicTo(43f, 69f, 25f, 50f, 25f, 39f); cubicTo(25f, 5f, 75f, 5f, 75f, 39f); cubicTo(75f, 50f, 57f, 69f, 50f, 82f); close() }
                canvas.drawPath(p, ink); dot(canvas, 50f, 39f, 9f)
            }
            Glyph.SETTINGS -> {
                dot(canvas, 50f, 50f, 17f)
                for (i in 0 until 8) {
                    val angle = Math.PI * i / 4.0
                    val x1 = 50f + kotlin.math.cos(angle).toFloat() * 22f
                    val y1 = 50f + kotlin.math.sin(angle).toFloat() * 22f
                    val x2 = 50f + kotlin.math.cos(angle).toFloat() * 30f
                    val y2 = 50f + kotlin.math.sin(angle).toFloat() * 30f
                    line(canvas, x1, y1, x2, y2)
                }
                dot(canvas, 50f, 50f, 5f)
            }
            Glyph.CLOCK -> {
                dot(canvas, 50f, 50f, 29f); line(canvas, 50f, 30f, 50f, 51f); line(canvas, 50f, 51f, 66f, 58f)
            }
            Glyph.PHOTOS -> {
                dot(canvas, 69f, 32f, 6f)
                val p = Path().apply { moveTo(20f, 73f); lineTo(39f, 47f); lineTo(51f, 60f); lineTo(61f, 49f); lineTo(81f, 73f); close() }
                canvas.drawPath(p, ink)
            }
            Glyph.BANK -> {
                val roof = Path().apply { moveTo(18f, 38f); lineTo(50f, 18f); lineTo(82f, 38f); close() }
                canvas.drawPath(roof, ink); line(canvas, 22f, 42f, 78f, 42f); line(canvas, 24f, 76f, 76f, 76f)
                for (x in listOf(31f, 44f, 56f, 69f)) { line(canvas, x, 44f, x, 72f) }
            }
            Glyph.GENERIC -> {
                val initial = appLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "•"
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = glyphColor; alpha = drawableAlpha; textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    textSize = if (initial.length > 1) 31f else 43f
                }
                canvas.drawText(initial, 50f, 65f, p)
            }
        }
        canvas.restoreToCount(save)
    }

    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) = c.drawLine(x1, y1, x2, y2, ink)

    private fun dot(c: Canvas, x: Float, y: Float, radius: Float, filled: Boolean = false) {
        val oldStyle = ink.style
        if (filled) ink.style = Paint.Style.FILL
        c.drawCircle(x, y, radius, ink)
        ink.style = oldStyle
    }

    private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float) {
        val oldStyle = ink.style
        ink.style = Paint.Style.FILL
        val oldSize = ink.textSize
        ink.textAlign = Paint.Align.CENTER
        ink.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        ink.textSize = size
        c.drawText(value, x, y, ink)
        ink.textSize = oldSize
        ink.typeface = android.graphics.Typeface.DEFAULT
        ink.style = oldStyle
    }

    override fun setAlpha(alpha: Int) { drawableAlpha = alpha.coerceIn(0, 255); invalidateSelf() }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { ink.colorFilter = colorFilter; backgroundPaint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android API; Drawable opacity is retained for compatibility")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private fun identify(label: String, pkg: String): Glyph {
            val s = "$label $pkg"
            fun has(vararg terms: String) = terms.any { s.contains(it) }
            return when {
                // Match apps commonly installed on the target device before falling back to initials.
                has("cloudflare") -> Glyph.SHIELD
                has("dropbox", "google drive", "drive", "cloud") -> Glyph.CLOUD
                has("codespaces", "developer tools", "programming", "visual studio code") -> Glyph.CODE
                has("geofence") -> Glyph.MAP
                has("doctolib", "medical", "healthcare", "clinic", "doctor") -> Glyph.HEALTH
                has("ebook", "e-book", "kindle", "book reader", "books") -> Glyph.BOOK
                has("claude", "deepseek", "chatgpt", "gemini", "copilot", "bixby", "assistant", "ai ") -> Glyph.AI
                has("comet", "brave", "chrome", "firefox", "browser", "internet", "samsunginternet", "webview") -> Glyph.BROWSER
                has("creative studio", "gallery", "photos", "galleria", "foto") -> Glyph.PHOTOS
                has("deliveroo", "amazon", "shopping", "ebay", "shop", "marketplace") -> Glyph.SHOPPING
                has("amy 4k", "iptv", "smarttv", "androidtv", "television") -> Glyph.TV
                label == "android" || pkg.contains("android") && label.contains("android") -> Glyph.ANDROID
                has("animalingua", "animal", "pet", "paw") -> Glyph.ANIMAL
                has("music", "spotify", "audio", "podcast", "soundcloud") -> Glyph.MUSIC
                has("archivio", "file manager", "my files", "files", "documenti") -> Glyph.FOLDER
                has("area personale", "secure folder", "security", "privacy", "defender") -> Glyph.SHIELD
                has("pec", "gmail", "email", "e-mail", "mail", "outlook", "posta") -> Glyph.MAIL
                has("authenticator", "2fa", "otp", "passkey", "password") -> Glyph.AUTH
                has("benzawash", "car wash", "automotive", "auto ") -> Glyph.CAR
                has("bitrefill", "lightning", "bolt") -> Glyph.BOLT
                has("bluewallet", "wallet", "portafoglio") -> Glyph.WALLET
                has("bitcoin", "bull", "btc", "crypto", "cryptocurrency") -> Glyph.BITCOIN
                has("calcolatrice", "calculator") -> Glyph.CALCULATOR
                has("calendar", "calendario", "agenda") -> Glyph.CALENDAR
                has("camera", "fotocamera") -> Glyph.CAMERA
                has("cieid", "cie id", "identity", "identity card", "carta identità", "carta identita", "spid") -> Glyph.ID
                has("telefono", "phone", "dialer") -> Glyph.PHONE
                has("contacts", "contatti", "rubrica") -> Glyph.CONTACTS
                has("messages", "messaggi", "messaging", "whatsapp", "telegram", "signal", "chat") -> Glyph.MESSAGE
                has("youtube", "video", "netflix", "prime video", "disney") -> Glyph.VIDEO
                has("maps", "mappe", "google maps", "navigazione", "waze", "geofence") -> Glyph.MAP
                has("settings", "impostazioni") -> Glyph.SETTINGS
                has("clock", "orologio", "alarm", "sveglia") -> Glyph.CLOCK
                has("revolut", "bank", "banca", "banking", "trade republic", "postepay", "intesa", "unicredit") -> Glyph.BANK
                has("eni plenitude", "plenitude", "eolo", "energy", "energia", "electricity", "utility") -> Glyph.BOLT
                else -> Glyph.GENERIC
            }
        }
    }
}
