package fr.rafu.ooty

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        render(ctx, mgr, ids)
        scheduleMidnight(ctx)
        val pending = goAsync()
        Thread {
            try {
                if (Plan.refresh(ctx)) render(ctx, mgr, ids)
            } finally {
                pending.finish()
            }
        }.start()
    }

    override fun onEnabled(ctx: Context) = scheduleMidnight(ctx)

    companion object {
        private val HEADER = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TodayWidget::class.java))
            if (ids.isNotEmpty()) render(ctx, mgr, ids)
        }

        fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            val today = LocalDate.now()
            val views = RemoteViews(ctx.packageName, R.layout.widget_today)
            val day = try { Plan.day(ctx, today) } catch (_: Exception) { null }
            val race = try { Plan.nextRace(ctx, today) } catch (_: Exception) { null }

            val header = HEADER.format(today).replace(".", "") + (day?.let { " · Sem. ${it.sem}" } ?: "")
            views.setTextViewText(R.id.tvHeader, header)
            views.setTextViewText(R.id.tvCountdown, race?.let {
                val n = Plan.daysUntil(today, it.date)
                (if (n == 0L) "Jour J · " else "J-$n · ") + it.name
            } ?: "")

            if (day == null) {
                views.setTextViewText(R.id.tvType, "Hors plan")
                views.setTextColor(R.id.tvType, ctx.getColor(R.color.t_repos))
                views.setTextViewText(R.id.tvKm, "")
                views.setTextViewText(R.id.tvCourse, "Pas de programme pour aujourd'hui")
                views.setTextViewText(R.id.tvExtras, "")
            } else {
                val rest = day.type.startsWith("Repos")
                views.setTextViewText(R.id.tvType, if (rest) "Repos course" else day.type)
                views.setTextColor(R.id.tvType, ctx.getColor(colorFor(day.type)))
                views.setTextViewText(R.id.tvKm, day.km?.takeIf { it > 0 }?.let { fmtKm(it) + " km" } ?: "")
                views.setTextViewText(R.id.tvCourse, (if (day.done) "✓ " else "") + day.course)
                val extras = buildList {
                    day.event?.let { add("★ $it") }
                    day.kick?.takeIf { !it.startsWith("Repos") }?.let { add("🥊 $it") }
                    day.salle?.let { add("🏋 $it") }
                    day.routine?.let { add("◦ $it") }
                    if (day.routine?.contains("obilit") != true) add("◦ Mobilité 12' ce soir")
                }
                views.setTextViewText(R.id.tvExtras, extras.joinToString("\n"))
            }

            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.root, open)
            mgr.updateAppWidget(ids, views)
        }

        private fun colorFor(type: String) = when {
            type == "Dure" -> R.color.t_dure
            type == "Soutenue" -> R.color.t_soutenue
            type == "COURSE" -> R.color.w_course
            type == "Vacances" -> R.color.t_vacances
            type.startsWith("Repos") -> R.color.t_repos
            else -> R.color.t_facile
        }

        private fun fmtKm(k: Double) = if (k % 1.0 == 0.0) k.toInt().toString() else k.toString().replace('.', ',')

        /** Refreshes the widget just after midnight so the new day shows up. */
        fun scheduleMidnight(ctx: Context) {
            val am = ctx.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(ctx, TodayWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, TodayWidget::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            val pi = PendingIntent.getBroadcast(ctx, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val next = LocalDate.now().plusDays(1).atTime(0, 1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            am.set(AlarmManager.RTC, next, pi)
        }
    }
}
