package com.tvmanager.launcher

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {
    private val refresh = ArrayList<() -> Unit>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun onOff(b: Boolean) = if (b) "включён" else "выключен"
    private fun refreshAll() = refresh.forEach { it() }

    private fun flip(k: String, def: Boolean) {
        val p = Store.prefs(this)
        p.edit().putBoolean(k, !p.getBoolean(k, def)).apply()
        refreshAll()
    }

    private fun vol(k: String) {
        val p = Store.prefs(this)
        var n = p.getInt(k, 50) + 10
        if (n > 100) n = 10
        p.edit().putInt(k, n).apply()
        refreshAll()
    }

    private fun minutes() {
        val p = Store.prefs(this)
        val opts = listOf(0, 1, 3, 5, 10, 30)
        val next = opts[(opts.indexOf(p.getInt("ss_min", 5)) + 1) % opts.size]
        p.edit().putInt("ss_min", next).apply()
        refreshAll()
    }

    private fun pick(folder: String, key: String) {
        val files = Store.list(this, folder)
        val b = AlertDialog.Builder(this).setTitle("Выберите видео")
        if (files.isEmpty()) {
            b.setMessage("Папка пуста. Отправьте видео (mp4/mkv/webm) из ПК-приложения или положите в:\n" +
                Store.sub(this, folder).absolutePath)
        } else {
            b.setItems((listOf("— без видео —") + files).toTypedArray()) { _, i ->
                Store.prefs(this).edit().putString(key, if (i == 0) "" else files[i - 1]).apply()
                refreshAll()
            }
        }
        b.show()
    }

    private fun bg(f: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(10).toFloat()
        setColor(if (f) Color.argb(130, 255, 255, 255) else Color.argb(40, 255, 255, 255))
    }

    private fun row(parent: LinearLayout, text: () -> String, action: () -> Unit) {
        val t = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(dp(20), dp(14), dp(20), dp(14))
            isFocusable = true
            isClickable = true
            background = bg(false)
            setOnFocusChangeListener { v, f -> v.background = bg(f) }
            setOnClickListener { action() }
        }
        val upd: () -> Unit = { t.text = text() }
        upd()
        refresh.add(upd)
        parent.addView(t, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = Store.prefs(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(60), dp(30), dp(60), dp(30))
        }
        col.addView(TextView(this).apply {
            text = "Настройки лаунчера"
            textSize = 28f
            setTextColor(Color.WHITE)
        })
        row(col, { "Видео обоев: " + (p.getString("wp_file", "") ?: "").ifEmpty { "нет" } }) { pick("wallpapers", "wp_file") }
        row(col, { "Звук обоев: " + onOff(p.getBoolean("wp_sound", false)) }) { flip("wp_sound", false) }
        row(col, { "Громкость обоев: ${p.getInt("wp_vol", 50)}%" }) { vol("wp_vol") }
        row(col, { "Видео заставки: " + (p.getString("ss_file", "") ?: "").ifEmpty { "нет" } }) { pick("screensaver", "ss_file") }
        row(col, { "Звук заставки: " + onOff(p.getBoolean("ss_sound", false)) }) { flip("ss_sound", false) }
        row(col, { "Громкость заставки: ${p.getInt("ss_vol", 50)}%" }) { vol("ss_vol") }
        row(col, {
            val m = p.getInt("ss_min", 5)
            "Заставка в лаунчере через: " + (if (m == 0) "выключена" else "$m мин")
        }) { minutes() }
        row(col, { "Свои звуки клавиш в лаунчере: " + onOff(p.getBoolean("key_sounds", true)) }) { flip("key_sounds", true) }
        col.addView(TextView(this).apply {
            text = "Папка с файлами: " + Store.dir(this@SettingsActivity).absolutePath
            textSize = 14f
            setTextColor(Color.argb(180, 255, 255, 255))
            setPadding(dp(20), dp(24), dp(20), 0)
        })
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.rgb(14, 17, 23))
            addView(col)
        })
    }
}
