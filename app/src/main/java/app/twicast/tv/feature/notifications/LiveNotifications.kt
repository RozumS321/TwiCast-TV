package app.twicast.tv.feature.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import app.twicast.tv.MainActivity
import app.twicast.tv.R
import app.twicast.tv.data.model.Stream
import app.twicast.tv.feature.player.PlayerDestination

/** High importance: TVs that pop notifications up (some Xiaomi, Google TV) do it only for these. */
private const val ChannelId = "live_starts_high"

/** The first channel, at default importance: an existing channel's importance cannot be raised, so it goes. */
private const val OldChannelId = "live_starts"

/** More at once collapse into one summary: a dozen notifications after a long pause would be noise. */
private const val MaxSeparate = 3
private const val SummaryId = 1
private const val LogoSizePx = 192

private const val ExtraLogin = "twicast.live.login"
private const val ExtraName = "twicast.live.name"
private const val ExtraCategory = "twicast.live.category"
private const val ExtraAvatar = "twicast.live.avatar"
private const val ExtraTitle = "twicast.live.title"

/** System notifications "X в эфире"; a tap opens that stream in TwiCast. */
object LiveNotifications {

    fun show(context: Context, streams: List<Stream>) {
        if (streams.isEmpty()) return
        val manager = NotificationManagerCompat.from(context)
        // Off in the system (or never allowed on Android 13+): nothing to post.
        if (!manager.areNotificationsEnabled()) return
        createChannel(context)
        val shown = if (streams.size > MaxSeparate) listOf(summary(context, streams)) else streams.map { notification(context, it) }
        shown.forEach { (id, notification) ->
            try {
                manager.notify(id, notification)
            } catch (denied: SecurityException) {
                // Permission withdrawn between the check and the post.
                return
            }
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(ChannelId, context.getString(R.string.notify_channel_name), NotificationManager.IMPORTANCE_HIGH)
        channel.description = context.getString(R.string.notify_channel_description)
        val system = context.getSystemService(NotificationManager::class.java)
        system.deleteNotificationChannel(OldChannelId)
        system.createNotificationChannel(channel)
    }

    /** The TwiCast mark next to the text: the small icon alone is a white glyph most TVs barely show. */
    private fun logo(context: Context): Bitmap? =
        ContextCompat.getDrawable(context, R.drawable.ic_logo_mark)?.toBitmap(LogoSizePx, LogoSizePx)

    private fun notification(context: Context, stream: Stream): Pair<Int, android.app.Notification> {
        val id = stream.userLogin.lowercase().hashCode()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(ExtraLogin, stream.userLogin)
            .putExtra(ExtraName, stream.channelName)
            .putExtra(ExtraCategory, stream.categoryName)
            .putExtra(ExtraAvatar, stream.avatarUrl)
            .putExtra(ExtraTitle, stream.title)
        val text = listOf(stream.categoryName, stream.title).filter { it.isNotBlank() }.joinToString(" · ")
        return id to NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(logo(context))
            .setContentTitle(context.getString(R.string.notify_live_one, stream.channelName))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
    }

    private fun summary(context: Context, streams: List<Stream>): Pair<Int, android.app.Notification> {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val first = streams.first().channelName
        return SummaryId to NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(logo(context))
            .setContentTitle(context.resources.getQuantityString(R.plurals.notify_live_many, streams.size - 1, first, streams.size - 1))
            .setContentText(streams.drop(1).joinToString(" · ") { it.channelName })
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, SummaryId, intent, PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    /** The stream a tapped notification asks for, or null for any other launch. */
    fun destinationOf(intent: Intent?): PlayerDestination? {
        val login = intent?.getStringExtra(ExtraLogin) ?: return null
        return PlayerDestination(
            channelLogin = login,
            channelName = intent.getStringExtra(ExtraName) ?: login,
            categoryName = intent.getStringExtra(ExtraCategory).orEmpty(),
            viewerCount = null,
            avatarUrl = intent.getStringExtra(ExtraAvatar),
            title = intent.getStringExtra(ExtraTitle),
        )
    }
}
