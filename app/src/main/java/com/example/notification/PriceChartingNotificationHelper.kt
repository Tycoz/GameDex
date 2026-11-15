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
import com.example.MainActivity
import java.util.Locale

object PriceChartingNotificationHelper {

    const val CHANNEL_ID = "pricecharting_updates_channel"
    private const val CHANNEL_NAME = "Actualisation des cotes PriceCharting"
    private const val CHANNEL_DESC = "Notifications lors de l'actualisation mensuelle des cotes de votre collection."
    private const val NOTIFICATION_ID = 4001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendMonthlyUpdateNotification(
        context: Context,
        gamesUpdatedCount: Int,
        totalCollectionValueEur: Double,
        diffEur: Double
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val diffText = if (diffEur != 0.0) {
            val sign = if (diffEur > 0) "+" else ""
            String.format(Locale.FRANCE, " (%s%.2f €)", sign, diffEur)
        } else ""

        val contentTitle = "Cotes PriceCharting actualisées"
        val contentText = String.format(
            Locale.FRANCE,
            "%d jeux mis à jour. Nouvelle valeur totale : %.2f €%s",
            gamesUpdatedCount,
            totalCollectionValueEur,
            diffText
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "L'actualisation mensuelle automatique depuis PriceCharting.com est terminée.\n" +
                            "• Jeux actualisés : $gamesUpdatedCount\n" +
                            String.format(
                                Locale.FRANCE,
                                "• Valeur estimée de votre collection : %.2f €%s",
                                totalCollectionValueEur,
                                diffText
                            )
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }
}
