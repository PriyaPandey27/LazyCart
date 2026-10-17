package com.lazycart.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — entry point for Hilt dependency injection.
 * Also sets up notification channels on first launch.
 */
@HiltAndroidApp
class LazyCartApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Channel for items running low / predicted restock
            val restockChannel = NotificationChannel(
                CHANNEL_RESTOCK,
                "Restock Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when pantry items are predicted to run out soon"
            }

            // Channel for items approaching expiry
            val expiryChannel = NotificationChannel(
                CHANNEL_EXPIRY,
                "Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when pantry items are about to expire"
            }

            manager.createNotificationChannels(listOf(restockChannel, expiryChannel))
        }
    }

    companion object {
        const val CHANNEL_RESTOCK = "LazyCart_restock"
        const val CHANNEL_EXPIRY = "LazyCart_expiry"
    }
}
