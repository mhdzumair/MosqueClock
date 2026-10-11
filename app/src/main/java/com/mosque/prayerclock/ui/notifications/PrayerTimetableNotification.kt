package com.mosque.prayerclock.ui.notifications

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.utils.TimeUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date

/** Native RemoteViews: the system notification drawer cannot host Compose content. */
object PrayerTimetableNotification {
    private data class Prayer(val name: Int, val icon: Int, val time: String)

    private fun prayers(times: PrayerTimes): List<Prayer> = listOf(
        Prayer(R.string.fajr, R.drawable.notification_prayer_dawn, times.fajrAzan),
        Prayer(R.string.sunrise, R.drawable.notification_prayer_sunrise, times.sunrise),
        Prayer(R.string.dhuhr, R.drawable.notification_prayer_sun, times.dhuhrAzan),
        Prayer(R.string.asr, R.drawable.notification_prayer_afternoon, times.asrAzan),
        Prayer(R.string.maghrib, R.drawable.notification_prayer_sunset, times.maghribAzan),
        Prayer(R.string.isha, R.drawable.notification_prayer_moon, times.ishaAzan),
    )

    fun nextPrayerIndex(times: PrayerTimes): Int? {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentSecond = now.hour * 3600 + now.minute * 60 + now.second
        return prayers(times).mapIndexedNotNull { index, prayer ->
            // Sunrise remains in the timetable, but is not an upcoming obligatory prayer.
            if (prayer.name == R.string.sunrise) return@mapIndexedNotNull null
            val parts = prayer.time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapIndexedNotNull null
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapIndexedNotNull null
            if (hour !in 0..23 || minute !in 0..59) return@mapIndexedNotNull null
            val remaining = (hour * 3600 + minute * 60 - currentSecond + 86_400) % 86_400
            index to remaining
        }.minByOrNull { it.second }?.first
    }

    fun apply(builder: NotificationCompat.Builder, context: Context, settings: AppSettings, times: PrayerTimes) {
        val prayers = prayers(times)
        val nextIndex = nextPrayerIndex(times)
        val compact = RemoteViews(context.packageName, R.layout.notification_prayer_times_compact)
        val expanded = RemoteViews(context.packageName, R.layout.notification_prayer_times_expanded)
        compact.removeAllViews(R.id.prayer_first_row)
        compact.removeAllViews(R.id.prayer_second_row)
        expanded.removeAllViews(R.id.prayer_first_row)
        expanded.removeAllViews(R.id.prayer_second_row)
        prayers.forEachIndexed { index, prayer ->
            compact.addView(
                if (index < 3) R.id.prayer_first_row else R.id.prayer_second_row,
                cell(context, settings, prayer, index == nextIndex, compact = true),
            )
            expanded.addView(
                if (index < 3) R.id.prayer_first_row else R.id.prayer_second_row,
                cell(context, settings, prayer, index == nextIndex, compact = false),
            )
        }
        val next = nextIndex?.let { prayers[it] }
        val nextText = if (next != null) {
            "${context.getString(R.string.next_prayer)} · ${context.getString(next.name)}  ${TimeUtils.formatTimeBasedOnPreference(next.time, settings.show24HourFormat)}"
        } else {
            context.getString(R.string.prayer_times_notification_title)
        }
        expanded.setTextViewText(R.id.prayer_next, nextText)
        @Suppress("DEPRECATION")
        val locale = context.resources.configuration.locale
        val datePattern = DateFormat.getBestDateTimePattern(locale, "EEE d MMM")
        expanded.setTextViewText(R.id.prayer_date, SimpleDateFormat(datePattern, locale).format(Date()))
        builder
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setContentText(nextText)
            .setColor(ContextCompat.getColor(context, R.color.notification_prayer_accent))
    }

    private fun cell(
        context: Context,
        settings: AppSettings,
        prayer: Prayer,
        selected: Boolean,
        compact: Boolean,
    ): RemoteViews {
        val views = RemoteViews(
            context.packageName,
            if (compact) R.layout.notification_prayer_compact_cell else R.layout.notification_prayer_expanded_cell,
        )
        val name = context.getString(prayer.name)
        val fullTime = TimeUtils.formatTimeBasedOnPreference(prayer.time, settings.show24HourFormat)
        views.setTextViewText(R.id.prayer_name, name)
        views.setTextViewText(R.id.prayer_time,
            if (compact) fullTime else TimeUtils.formatTimeWithoutPeriod(prayer.time, settings.show24HourFormat))
        views.setTextViewText(R.id.prayer_period, fullTime.substringAfterLast(' ', ""))
        views.setViewVisibility(R.id.prayer_period, if (compact || settings.show24HourFormat) View.GONE else View.VISIBLE)
        views.setInt(
            R.id.prayer_cell, "setBackgroundResource",
            when {
                compact && selected -> R.drawable.notification_prayer_compact_card_selected
                compact -> R.drawable.notification_prayer_compact_card
                selected -> R.drawable.notification_prayer_card_selected
                else -> R.drawable.notification_prayer_card
            },
        )
        views.setTextColor(R.id.prayer_name, ContextCompat.getColor(context,
            if (selected) R.color.notification_prayer_on_selected else R.color.notification_prayer_muted))
        views.setTextColor(R.id.prayer_time, ContextCompat.getColor(context,
            if (selected) R.color.notification_prayer_on_selected else R.color.notification_prayer_ink))
        views.setTextColor(R.id.prayer_period, ContextCompat.getColor(context,
            if (selected) R.color.notification_prayer_on_selected else R.color.notification_prayer_muted))
        views.setContentDescription(R.id.prayer_cell, "$name $fullTime")
        if (!compact) views.setImageViewResource(R.id.prayer_icon, prayer.icon)
        return views
    }
}
