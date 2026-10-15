package com.mywebapp.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class NotifyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

  companion object {
    const val CHANNEL_ID = "updates"
    const val PREFS = "notify"
    const val KEY_LAST_ID = "last_id"

    suspend fun checkNow(context: Context) {
      val feed = Config.load(context).optString("feed")
      if (feed.isBlank()) return
      val json = withContext(Dispatchers.IO) { httpGet(feed) } ?: return
      val arr = try { JSONArray(json) } catch (e: Exception) { return }
      if (arr.length() == 0) return

      val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      val lastId = prefs.getString(KEY_LAST_ID, "") ?: ""
      val newest = arr.getJSONObject(0)

      if (lastId.isEmpty()) {
        prefs.edit().putString(KEY_LAST_ID, newest.optString("id")).apply()
        return
      }
      if (newest.optString("id") == lastId) return

      var shown = 0
      for (i in 0 until arr.length()) {
        val n = arr.getJSONObject(i)
        if (n.optString("id") == lastId) break
        if (shown >= 5) break
        showNotification(context, n.optString("title"), n.optString("body"), n.optString("link"), i)
        shown++
      }
      prefs.edit().putString(KEY_LAST_ID, newest.optString("id")).apply()
    }

    private fun httpGet(url: String): String? = try {
      val c = URL(url).openConnection() as HttpURLConnection
      c.connectTimeout = 10000; c.readTimeout = 10000
      c.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) { null }

    private fun showNotification(context: Context, title: String, body: String, link: String, id: Int) {
      val intent = if (link.isNotBlank())
        Intent(Intent.ACTION_VIEW, Uri.parse(link))
      else
        Intent(context, MainActivity::class.java)
      val pi = PendingIntent.getActivity(
        context, id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val notif = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setContentIntent(pi)
        .build()
      try { NotificationManagerCompat.from(context).notify(1000 + id, notif) } catch (_: SecurityException) {}
    }
  }

  override suspend fun doWork(): Result {
    checkNow(applicationContext)
    return Result.success()
  }
}
