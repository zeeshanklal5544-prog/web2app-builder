package com.mywebapp.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    if (Build.VERSION.SDK_INT >= 26) {
      val ch = NotificationChannel(
        NotifyWorker.CHANNEL_ID, "Updates",
        NotificationManager.IMPORTANCE_DEFAULT
      )
      getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
    val work = PeriodicWorkRequestBuilder<NotifyWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      "notify-check", ExistingPeriodicWorkPolicy.KEEP, work
    )
  }
}
