package ac.mdiq.podcini.config.automation

import ac.mdiq.podcini.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * The ongoing notification both automation services put up.
 *
 * One channel and one builder for the two of them, so the thing 白い熊 sees while a backup is
 * running does not depend on which door started it. Kept out of either service because both need it
 * as their **first** statement — see the foreground-first note in [AutomationDataService].
 */
internal object AutomationNotifications {

    private const val CHANNEL = "kuchusen_automation_data"

    /** The §2a data door's own notification. */
    const val ID_DATA = 9714

    /** The §1 receiver's export. A separate id: both can be running at once. */
    const val ID_EXPORT = 9715

    fun build(context: Context, titleRes: Int): Notification {
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.kuchusen_auto_notif_channel),
                NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(context, CHANNEL)
            .setContentTitle(context.getString(titleRes))
            .setSmallIcon(R.drawable.ic_notification_sync)
            .setOngoing(true)
            .build()
    }
}
