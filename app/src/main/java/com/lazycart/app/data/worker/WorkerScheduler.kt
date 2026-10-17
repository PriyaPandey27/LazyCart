package com.lazycart.app.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for background WorkManager jobs in LazyCart.
 * Handles recurring daily notification checks for pantry restock predictions and item expiry.
 */
@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Enqueues the periodic daily reminder worker.
     * Uses [ExistingPeriodicWorkPolicy.KEEP] to preserve the existing schedule if already enqueued.
     */
    fun scheduleDailyReminders() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyReminderRequest = PeriodicWorkRequestBuilder<PantryReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyReminderRequest
        )
    }

    /**
     * Cancels any scheduled reminder work.
     */
    fun cancelReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY_REMINDER)
    }

    companion object {
        const val WORK_NAME_DAILY_REMINDER = "LazyCart_daily_reminder"
    }
}
