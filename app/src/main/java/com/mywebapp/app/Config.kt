package com.mywebapp.app
import android.content.Context
import org.json.JSONObject
object Config {
  fun load(c: Context): JSONObject =
    JSONObject(c.assets.open("config.json").bufferedReader().use { it.readText() })
}
