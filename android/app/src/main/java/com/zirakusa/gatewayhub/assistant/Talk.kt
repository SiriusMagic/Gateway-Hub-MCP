package com.zirakusa.gatewayhub.assistant

import android.content.res.Resources
import com.zirakusa.gatewayhub.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The words of each answer and the names of the actions, from the app's texts (what is written is also what is said). */
object Talk {
    fun text(r: Reply, res: Resources, locale: Locale = Locale.getDefault()): String {
        val a = r.args
        val head = when (r.say) {
            Say.OPENING -> res.getString(R.string.say_opening, a[0])
            Say.NOT_INSTALLED -> res.getString(R.string.say_not_installed, a[0])
            Say.PLAYING -> res.getString(R.string.say_playing)
            Say.PLAYING_QUERY -> res.getString(R.string.say_playing_query, a[0])
            Say.RECOMMENDED -> res.getString(R.string.say_recommended)
            Say.MEDIA_DONE -> res.getString(when (MediaKey.valueOf(a[0])) { MediaKey.PAUSE -> R.string.say_paused; MediaKey.RESUME -> R.string.say_resumed
                MediaKey.NEXT -> R.string.say_next; MediaKey.PREVIOUS -> R.string.say_previous })
            Say.ALARM_SET -> res.getString(R.string.say_alarm, "%d:%02d".format(a[0].toInt(), a[1].toInt()))
            Say.TIMER_SET -> res.getString(R.string.say_timer, duration(a[0].toInt(), res))
            Say.CALLING -> res.getString(R.string.say_calling, a[0])
            Say.MESSAGING -> res.getString(R.string.say_messaging, a[0])
            Say.NO_CONTACT -> res.getString(R.string.say_no_contact, a[0])
            Say.NEED_CONTACTS -> res.getString(R.string.say_need_contacts)
            Say.TIME -> res.getString(R.string.say_time, "%d:%02d".format(a[0].toInt(), a[1].toInt()))
            Say.DATE -> res.getString(R.string.say_date, DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(LocalDate.parse(a[0])))
            Say.SUMMARY -> a[0]
            Say.NOTHING_TODAY -> a[0].ifBlank { res.getString(R.string.say_nothing_today) }
            Say.NOT_UNDERSTOOD -> res.getString(R.string.say_not_understood)
            Say.UNKNOWN_APP -> res.getString(R.string.say_unknown_app, a[0])
            Say.FAILED -> res.getString(R.string.say_failed, a[0])
            Say.COUNTING -> res.getString(R.string.say_counting)
            Say.VOLUME_SET -> res.getString(R.string.say_volume, a[0].toInt())
        }
        return (listOf(head) + r.lines + listOfNotNull(r.volume?.let { res.getString(R.string.say_volume, it) })).filter { it.isNotBlank() }.joinToString("\n")
    }

    private fun duration(s: Int, res: Resources) = when {
        s >= 3600 && s % 3600 == 0 -> res.getQuantityString(R.plurals.hours, s / 3600, s / 3600)
        s >= 60 && s % 60 == 0 -> res.getQuantityString(R.plurals.minutes, s / 60, s / 60)
        s >= 60 -> "%d:%02d".format(s / 60, s % 60)
        else -> res.getQuantityString(R.plurals.seconds, s, s)
    }

    fun label(a: Action, res: Resources) = res.getString(when (a) {
        Action.PLAY_MUSIC -> R.string.do_music; Action.PLAY_RECOMMENDED -> R.string.do_recommended; Action.MEDIA -> R.string.do_media
        Action.ALARM -> R.string.do_alarm; Action.TIMER -> R.string.do_timer; Action.CALL -> R.string.do_call; Action.MESSAGE -> R.string.do_message
        Action.WEATHER -> R.string.do_weather; Action.AGENDA -> R.string.do_agenda; Action.TODAY -> R.string.do_today; Action.TIME -> R.string.do_time
        Action.DATE -> R.string.do_date; Action.PHOTO -> R.string.do_photo; Action.SCAN -> R.string.do_scan; Action.SPEED -> R.string.do_speed
        Action.COUNT -> R.string.do_count; Action.VOLUME -> R.string.do_volume; Action.OPEN -> R.string.do_open; Action.UNKNOWN -> R.string.do_unknown
    })
}
