package com.shuayb.capstone.android.sirboxalot

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BaseApp : Application() {

    companion object {
        val CHANNEL_1_ID = "channel1"
        val CHANNEL_1_NAME = "Channel 1"
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_1_ID,
                CHANNEL_1_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel1.setDescription("This is " + CHANNEL_1_NAME)

            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel1)
        }

    }

}