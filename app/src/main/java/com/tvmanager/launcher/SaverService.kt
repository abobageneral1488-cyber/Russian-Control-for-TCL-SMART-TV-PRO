package com.tvmanager.launcher

import android.service.dreams.DreamService

/** Системная заставка: видео из папки screensaver со своим звуком (вкл/выкл в настройках лаунчера). */
class SaverService : DreamService() {
    private var layer: VideoLayer? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val p = Store.prefs(this)
        val l = VideoLayer(this)
        layer = l
        setContentView(l)
        val vol = if (p.getBoolean("ss_sound", false)) p.getInt("ss_vol", 50) / 100f else 0f
        l.play(Store.file(this, "screensaver", p.getString("ss_file", "")), vol)
    }

    override fun onDetachedFromWindow() {
        layer?.stop()
        layer = null
        super.onDetachedFromWindow()
    }
}
