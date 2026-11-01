package com.tvmanager.launcher

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import java.io.File

/** Зацикленное видео на весь экран (как обои). Громкость — своя, не зависит от системных настроек звука. */
class VideoLayer(ctx: Context) : TextureView(ctx), TextureView.SurfaceTextureListener {
    private var mp: MediaPlayer? = null
    private var surf: Surface? = null
    private var st: SurfaceTexture? = null
    private var path: String? = null
    private var volume = 0f
    private var vw = 0
    private var vh = 0

    init {
        surfaceTextureListener = this
    }

    fun play(file: File?, vol: Float) {
        path = file?.absolutePath
        volume = vol
        restart()
    }

    fun setVol(v: Float) {
        volume = v
        try {
            mp?.setVolume(v, v)
        } catch (e: Exception) {
        }
    }

    fun stop() {
        try {
            mp?.release()
        } catch (e: Exception) {
        }
        mp = null
        surf?.release()
        surf = null
    }

    private fun restart() {
        stop()
        val p = path ?: return
        val t = st ?: return
        val s = Surface(t)
        surf = s
        val m = MediaPlayer()
        try {
            m.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            m.setDataSource(p)
            m.setSurface(s)
            m.isLooping = true
            m.setVolume(volume, volume)
            m.setOnVideoSizeChangedListener { _, w, h ->
                vw = w
                vh = h
                fit()
            }
            m.setOnErrorListener { _, _, _ -> true }
            m.setOnPreparedListener { it.start() }
            m.prepareAsync()
            mp = m
        } catch (e: Exception) {
            m.release()
        }
    }

    /** Масштаб «с обрезкой», чтобы видео закрывало весь экран без искажений. */
    private fun fit() {
        if (vw == 0 || vh == 0 || width == 0 || height == 0) return
        val vr = vw.toFloat() / vh
        val br = width.toFloat() / height
        val m = Matrix()
        if (vr > br) m.setScale(vr / br, 1f, width / 2f, height / 2f)
        else m.setScale(1f, br / vr, width / 2f, height / 2f)
        setTransform(m)
    }

    override fun onSurfaceTextureAvailable(t: SurfaceTexture, w: Int, h: Int) {
        st = t
        restart()
    }

    override fun onSurfaceTextureSizeChanged(t: SurfaceTexture, w: Int, h: Int) {
        fit()
    }

    override fun onSurfaceTextureDestroyed(t: SurfaceTexture): Boolean {
        stop()
        st = null
        return true
    }

    override fun onSurfaceTextureUpdated(t: SurfaceTexture) {}
}
