package com.lazycart.app.data.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lazycart.app.MainActivity
import com.lazycart.app.LazyCartApp
import com.lazycart.app.data.local.datastore.UserPreferencesDataStore
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.repository.ConsumptionRepository
import com.lazycart.app.domain.repository.PantryRepository
import com.lazycart.app.domain.usecase.RestockPredictionEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker executing daily checks for:
 * 1. Predicted restock events based on recent consumption rates.
 * 2. Approaching item expiration dates.
 *
 * Dispatches Android system notifications when items cross configured thresholds.
 */
@HiltWorker
class PantryReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pantryRepository: PantryRepository,
    private val consumptionRepository: ConsumptionRepository,
    private val userPrefs: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Read user notification preferences
            val restockRemindersEnabled = userPrefs.isRestockRemindersEnabled.first()
            val expiryRemindersEnabled = userPrefs.isExpiryRemindersEnabled.first()
            val restockDaysBefore = userPrefs.restockReminderDaysBefore.first()
            val expiryDaysBefore = userPrefs.expiryReminderDaysBefore.first()

            // If user disabled both reminder types, complete early
            if (!restockRemindersEnabled && !expiryRemindersEnabled) {
                return Result.success()
            }

            // Retrieve all active pantry items
            val activeItems = pantryRepository.observeActivePantryItems().first()

            for (item in activeItems) {
                // Fetch consumption history and compute restock prediction
                val history = consumptionRepository.getAllEventsForItem(item.id)
                val prediction = RestockPredictionEngine.predict(item, history)
                val itemWithPrediction = item.copy(prediction = prediction)

                // Check restock notification condition
                if (restockRemindersEnabled) {
                    val daysRemaining = prediction.daysRemaining
                    if (daysRemaining != null && daysRemaining <= restockDaysBefore.toFloat()) {
                        sendRestockNotification(itemWithPrediction, daysRemaining)
                    }
                }

                // Check expiry notification condition
                if (expiryRemindersEnabled && itemWithPrediction.isExpiringSoon(expiryDaysBefore)) {
                    sendExpiryNotification(itemWithPrediction)
                }
            }

            Result.success()
        } catch (e: Exception) {
            // Retry transient errors with exponential backoff managed by WorkManager
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }
    }

    private fun sendRestockNotification(item: PantryItem, daysRemaining: Float) {
        if (!hasNotificationPermission()) return

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val notificationId = (item.id % 100_000).toInt() + RESTOCK_NOTIFICATION_OFFSET

        val pendingIntent = createContentPendingIntent(notificationId)

        val daysInt = daysRemaining.toInt().coerceAtLeast(0)
        val contentText = when {
            daysRemaining <= 0f -> "${item.name} is out of stock! Add it to your shopping list."
            daysInt <= 1 -> "${item.name} will run out in ~1 day. Restock soon!"
            else -> "${item.name} will run out in ~$daysInt days. Restock soon!"
        }

        val notification = NotificationCompat.Builder(applicationContext, LazyCartApp.CHANNEL_RESTOCK)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Restock Alert: ${item.name}")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ permission edge case
        }
    }

    private fun sendExpiryNotification(item: PantryItem) {
        if (!hasNotificationPermission()) return

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val notificationId = (item.id % 100_000).toInt() + EXPIRY_NOTIFICATION_OFFSET

        val pendingIntent = createContentPendingIntent(notificationId)

        val contentText = if (item.isExpired()) {
            "${item.name} has expired on ${item.expiryDate}! Check before consuming."
        } else {
            "${item.name} is expiring on ${item.expiryDate}. Use it before it spoils!"
        }

        val notification = NotificationCompat.Builder(applicationContext, LazyCartApp.CHANNEL_EXPIRY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Expiry Alert: ${item.name}")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ permission edge case
        }
    }

    private fun createContentPendingIntent(notificationId: Int): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val RESTOCK_NOTIFICATION_OFFSET = 100_000
        private const val EXPIRY_NOTIFICATION_OFFSET = 200_000
    }
}
