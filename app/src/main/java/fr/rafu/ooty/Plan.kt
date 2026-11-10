package fr.rafu.ooty

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Reads the training plan: the copy downloaded from the repo if any, otherwise the one bundled in the APK. */
object Plan {
    private const val PLAN = "plan.json"
    private const val HTML = "index.html"

    private fun read(ctx: Context, name: String): String {
        val f = File(ctx.filesDir, name)
        if (f.exists() && f.length() > 0) return f.readText()
        return ctx.assets.open(name).bufferedReader().use { it.readText() }
    }

    fun html(ctx: Context): String = read(ctx, HTML)

    private fun plan(ctx: Context): JSONObject = JSONObject(read(ctx, PLAN))

    data class Day(
        val date: LocalDate, val sem: String, val phase: String, val course: String, val km: Double?,
        val type: String, val kick: String?, val salle: String?, val routine: String?, val event: String?, val done: Boolean
    )

    data class Race(val name: String, val date: LocalDate)

    private fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) optString(k).takeIf { it.isNotBlank() } else null

    fun day(ctx: Context, date: LocalDate): Day? {
        val days = plan(ctx).getJSONArray("days")
        val key = date.toString()
        for (i in 0 until days.length()) {
            val o = days.getJSONObject(i)
            if (o.optString("date") == key) {
                return Day(
                    date, o.str("sem") ?: "", o.str("phase") ?: "", o.str("course") ?: "Pas de course",
                    if (o.has("km")) o.optDouble("km").takeIf { !it.isNaN() } else null,
                    o.str("type") ?: "Repos course", o.str("kick"), o.str("salle"), o.str("routine"), o.str("event"),
                    o.str("fait") != null
                )
            }
        }
        return null
    }

    fun nextRace(ctx: Context, today: LocalDate): Race? {
        val days = plan(ctx).getJSONArray("days")
        for (i in 0 until days.length()) {
            val o = days.getJSONObject(i)
            if (o.optString("type") == "COURSE") {
                val d = LocalDate.parse(o.getString("date"))
                if (!d.isBefore(today)) {
                    val name = o.str("event") ?: o.optString("course").removePrefix("COURSE : ").substringBefore(",")
                    return Race(name, d)
                }
            }
        }
        return null
    }

    fun daysUntil(today: LocalDate, d: LocalDate) = ChronoUnit.DAYS.between(today, d)

    /** Downloads the latest plan from the repo. Returns true if something changed. Call off the main thread. */
    fun refresh(ctx: Context): Boolean {
        val base = BuildConfig.PLAN_BASE_URL
        if (base.isBlank()) return false
        var changed = false
        for (name in listOf(PLAN, HTML)) {
            try {
                val conn = URL(base.trimEnd('/') + "/" + name).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.useCaches = false
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val f = File(ctx.filesDir, name)
                    if (text.isNotBlank() && (!f.exists() || f.readText() != text)) {
                        f.writeText(text)
                        changed = true
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                // Offline: keep the copy we have
            }
        }
        return changed
    }
}
