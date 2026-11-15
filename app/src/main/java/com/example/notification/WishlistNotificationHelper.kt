package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.MainActivity
import java.util.Locale

object WishlistNotificationHelper {

    const val CHANNEL_ID = "wishlist_deals_channel"
    private const val CHANNEL_NAME = "Alertes Prix Wishlist (Vinted & eBay)"
    private const val CHANNEL_DESC = "Notifications envoyées lorsqu'un jeu de la wishlist est disponible sous votre prix max."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendDealNotification(
        context: Context,
        notificationId: Int,
        gameTitle: String,
        platformName: String,
        dealPrice: Double,
        maxPrice: Double,
        dealUrl: String,
        marketplace: String
    ) {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return
            }
        }

        val formattedPrice = String.format(Locale.FRANCE, "%.2f", dealPrice)
        val formattedMax = String.format(Locale.FRANCE, "%.2f", maxPrice)
        val savings = String.format(Locale.FRANCE, "%.2f", (maxPrice - dealPrice).coerceAtLeast(0.0))

        // Intent to open browser listing directly
        val browserIntent = if (dealUrl.isNotBlank()) {
            Intent(Intent.ACTION_VIEW, dealUrl.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Secondary intent to open GameDex app
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1000,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎯 $gameTitle à $formattedPrice € sur $marketplace !"
        val text = "Votre prix max : $formattedMax € (Économie : $savings €). Cliquez pour voir l'annonce."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$text\n\nPlateforme : $platformName • Source : $marketplace\nAnnonce disponible immédiatement.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Voir l'offre ($marketplace)",
                pendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "Ouvrir GameDex",
                appPendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked or not granted
        }
    }
}
