package app.hearthspell.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.hearthspell.MainActivity
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.HelperKind
import app.hearthspell.domain.Glow
import app.hearthspell.domain.World
import app.hearthspell.domain.gendered
import app.hearthspell.ui.floorName
import app.hearthspell.ui.letters.Letters
import app.hearthspell.ui.quote
import app.hearthspell.ui.spellName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/** Hearthspell comes by once a day, in a helper's voice: imps on a floor, or a letter from the master. */
object Alarms {
    private const val CHANNEL = "tower_v1"
    const val EXTRA_ROUTE = "route"

    private const val ACTION_DAILY = "app.hearthspell.notify.DAILY"
    private const val ID_DAILY = 1

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_tower), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_tower_desc)
                }
            )
        }
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_DAILY),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val m = Math.floorMod(minutes, 24 * 60)
        val today = now.toLocalDate().atTime(m / 60, m % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val p = pi(context)
        // Reminders don't need to-the-second precision; this avoids the exact-alarm permission.
        if (!s.onboarded || !s.remindOn) am.cancel(p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(s.remindAt, LocalDateTime.now(zone)).atZone(zone).toInstant().toEpochMilli(), p)
    }

    internal suspend fun world(context: Context): World {
        val c = context.container
        val d = c.dao
        return World(
            d.floors(), d.spells(), d.casts(), d.reagents(), d.decor(), System.currentTimeMillis(), ZoneId.systemDefault(), c.settings.now().startDay,
            d.finds(), d.brews(), d.wardrobe(),
        )
    }

    suspend fun fire(context: Context) {
        val s = context.container.settings.now()
        if (!s.onboarded || !s.remindOn) return
        val w = world(context)
        val locale = context.resources.configuration.locales[0]
        fun g(id: Int, vararg args: Any) = gendered(context.getString(id, *args), s.gender)

        // A letter that arrived today and is still sealed comes first: it's rarer.
        val arrived = w.lettersArrived(Letters.COUNT)
        val newest = arrived - 1
        val letterToday = newest > 0 && (w.day - s.startDay) % 7 == 0L && newest.toString() !in s.lettersRead
        if (letterToday) {
            notify(context, g(R.string.notif_letter_title), g(R.string.notif_letter_text), "letter/$newest")
            return
        }
        // Back after a long while: the master writes himself, gently, once the imps have had their party.
        if (w.homecoming && w.daysAway in 5..60 && w.totalImps > 0) {
            notify(context, g(R.string.notif_return_title), g(R.string.notif_return_text), "tower")
            return
        }
        val worst = w.due.firstOrNull { w.glow(it) == Glow.IMPS } ?: return
        val floor = w.floor(worst.floorId) ?: return
        val helper = w.helper(HelperKind.of(worst.helper))
        val who = if (helper.awake) helper.kind.title.of(locale) else context.getString(R.string.notif_someone)
        val title = g(R.string.notif_imps_title, floorName(floor, locale))
        val spell = quote(spellName(worst, locale), locale)
        val more = w.totalImps - 1
        val text = g(R.string.notif_imps_text, who, spell) + if (more > 0) "\n" + context.resources.getQuantityString(R.plurals.notif_more_imps, more, more) else ""
        notify(context, title, text, "floor/${floor.id}")
    }

    private fun open(context: Context, route: String) = PendingIntent.getActivity(
        context, 100,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ROUTE, route),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(context: Context, title: String, text: String, route: String) {
        if (!canNotify(context)) return
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, route))
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(ID_DAILY, b.build())
        } catch (_: SecurityException) {
        }
    }

    internal const val action = ACTION_DAILY
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Alarms.action) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context)
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}
