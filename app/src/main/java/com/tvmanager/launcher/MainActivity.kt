package com.tvmanager.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private lateinit var wallpaper: VideoLayer
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private var saver: VideoLayer? = null
    private var pool: SoundPool? = null
    private val keySounds = HashMap<Int, Int>()
    private val idle = Runnable { showSaver() }
    private val prefs: SharedPreferences get() = Store.prefs(this)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        root = FrameLayout(this)
        root.setBackgroundColor(Color.rgb(14, 17, 23))
        wallpaper = VideoLayer(this)
        root.addView(wallpaper, FrameLayout.LayoutParams(-1, -1))
        val dim = View(this)
        dim.setBackgroundColor(Color.argb(90, 0, 0, 0))
        root.addView(dim, FrameLayout.LayoutParams(-1, -1))
        scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.clipChildren = false
        content.setPadding(dp(40), dp(24), dp(40), dp(40))
        scroll.addView(content)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
        loadSounds()
        startWallpaper()
        resetIdle()
    }

    override fun onPause() {
        h.removeCallbacks(idle)
        hideSaver(false)
        wallpaper.stop()
        pool?.release()
        pool = null
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        hideSaver()
        scroll.smoothScrollTo(0, 0)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Лаунчер не закрывается кнопкой «Назад»
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (saver != null) {
            if (e.action == KeyEvent.ACTION_DOWN) hideSaver()
            return true
        }
        if (e.action == KeyEvent.ACTION_DOWN) {
            resetIdle()
            if (e.repeatCount == 0) keySounds[e.keyCode]?.let { pool?.play(it, 1f, 1f, 1, 0, 1f) }
        }
        return super.dispatchKeyEvent(e)
    }

    // ---------- обои, заставка, звуки ----------
    private fun wallVolume() = if (prefs.getBoolean("wp_sound", false)) prefs.getInt("wp_vol", 50) / 100f else 0f

    private fun startWallpaper() {
        wallpaper.play(Store.file(this, "wallpapers", prefs.getString("wp_file", "")), wallVolume())
    }

    private fun resetIdle() {
        h.removeCallbacks(idle)
        val m = prefs.getInt("ss_min", 5)
        if (m > 0 && saver == null) h.postDelayed(idle, m * 60_000L)
    }

    private fun showSaver() {
        val p = prefs
        val f = Store.file(this, "screensaver", p.getString("ss_file", "")) ?: return
        wallpaper.stop()
        val l = VideoLayer(this)
        saver = l
        root.addView(l, FrameLayout.LayoutParams(-1, -1))
        l.play(f, if (p.getBoolean("ss_sound", false)) p.getInt("ss_vol", 50) / 100f else 0f)
    }

    private fun hideSaver(restart: Boolean = true) {
        val l = saver ?: return
        l.stop()
        root.removeView(l)
        saver = null
        if (restart) {
            startWallpaper()
            resetIdle()
        }
    }

    private fun loadSounds() {
        pool?.release()
        pool = null
        keySounds.clear()
        if (!prefs.getBoolean("key_sounds", true)) return
        val map = Store.config(this).optJSONObject("sounds") ?: return
        val sp = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
        val codes = mapOf(
            "left" to KeyEvent.KEYCODE_DPAD_LEFT, "right" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "up" to KeyEvent.KEYCODE_DPAD_UP, "down" to KeyEvent.KEYCODE_DPAD_DOWN,
            "ok" to KeyEvent.KEYCODE_DPAD_CENTER, "back" to KeyEvent.KEYCODE_BACK,
            "home" to KeyEvent.KEYCODE_HOME
        )
        for ((k, code) in codes) {
            val f = Store.file(this, "sounds", map.optString(k)) ?: continue
            keySounds[code] = sp.load(f.absolutePath, 1)
        }
        pool = sp
    }

    // ---------- экран ----------
    private fun soundTitle() = "Звук обоев: " + (if (prefs.getBoolean("wp_sound", false)) "вкл" else "выкл")

    private fun toggleWallSound(v: View) {
        prefs.edit().putBoolean("wp_sound", !prefs.getBoolean("wp_sound", false)).apply()
        wallpaper.setVol(wallVolume())
        v.findViewWithTag<TextView>("label")?.text = soundTitle()
    }

    private fun render() {
        content.removeAllViews()
        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(this)
        title.text = "TV Manager"
        title.textSize = 26f
        title.setTextColor(Color.WHITE)
        head.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val clock = TextClock(this)
        clock.format24Hour = "HH:mm"
        clock.format12Hour = "HH:mm"
        clock.textSize = 26f
        clock.setTextColor(Color.WHITE)
        head.addView(clock)
        content.addView(head)

        val cfg = Store.config(this)
        val quick = ArrayList<View>()
        val btns = cfg.optJSONArray("buttons")
        if (btns != null) {
            for (i in 0 until btns.length()) {
                val bt = btns.getJSONObject(i)
                quick.add(tile(bt.optString("name"), iconOf(bt), 160, 110) { runButton(bt) })
            }
        }
        quick.add(tile(soundTitle(), null, 160, 110) { v -> toggleWallSound(v) })
        quick.add(tile("Настройки лаунчера", null, 160, 110) { startActivity(Intent(this, SettingsActivity::class.java)) })
        quick.add(tile("Настройки ТВ", null, 160, 110) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        content.addView(row("Быстрые кнопки", quick))

        val pm = packageManager
        val hidden = HashSet<String>()
        val hid = cfg.optJSONArray("hidden")
        if (hid != null) for (i in 0 until hid.length()) hidden.add(hid.getString(i))
        val seen = HashSet<String>()
        val apps = ArrayList<View>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
            for (ri in list.sortedBy { it.loadLabel(pm).toString().lowercase() }) {
                val ai = ri.activityInfo
                val pkg = ai.packageName
                if (pkg == packageName || pkg in hidden || !seen.add(pkg)) continue
                val icon = ai.loadBanner(pm) ?: ai.loadIcon(pm)
                apps.add(tile(ri.loadLabel(pm).toString(), icon, 192, 108) {
                    launchComp(ComponentName(pkg, ai.name).flattenToString())
                })
            }
        }
        content.addView(row("Приложения", apps))
    }

    private fun row(title: String, items: List<View>): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.clipChildren = false
        val t = TextView(this)
        t.text = title
        t.textSize = 18f
        t.setTextColor(Color.WHITE)
        t.setPadding(dp(10), dp(18), 0, 0)
        box.addView(t)
        val lin = LinearLayout(this)
        lin.clipChildren = false
        for (v in items) lin.addView(v)
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.clipChildren = false
        hs.addView(lin)
        box.addView(hs, LinearLayout.LayoutParams(-1, -2))
        return box
    }

    private fun tile(title: String, icon: Drawable?, w: Int, h: Int, onClick: (View) -> Unit): View {
        val card = FrameLayout(this)
        val bg = GradientDrawable()
        bg.cornerRadius = dp(10).toFloat()
        bg.setColor(Color.argb(150, 30, 36, 48))
        val fg = GradientDrawable()
        fg.cornerRadius = dp(10).toFloat()
        fg.setColor(Color.TRANSPARENT)
        fg.setStroke(dp(3), Color.TRANSPARENT)
        card.background = bg
        card.foreground = fg
        card.clipToOutline = true
        card.isFocusable = true
        card.isClickable = true
        val lp = LinearLayout.LayoutParams(dp(w), dp(h))
        lp.setMargins(dp(10), dp(12), dp(10), dp(12))
        card.layoutParams = lp
        if (icon != null) {
            val img = ImageView(this)
            img.setImageDrawable(icon)
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            card.addView(img, FrameLayout.LayoutParams(-1, -1))
        }
        val label = TextView(this)
        label.text = title
        label.tag = "label"
        label.textSize = 14f
        label.setTextColor(Color.WHITE)
        label.maxLines = 2
        label.ellipsize = TextUtils.TruncateAt.END
        label.gravity = Gravity.CENTER
        label.setPadding(dp(8), dp(4), dp(8), dp(4))
        if (icon != null) {
            label.setBackgroundColor(Color.argb(150, 0, 0, 0))
            card.addView(label, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        } else {
            card.addView(label, FrameLayout.LayoutParams(-1, -1))
        }
        card.setOnClickListener { onClick(card) }
        card.setOnFocusChangeListener { v, f ->
            fg.setStroke(dp(3), if (f) Color.WHITE else Color.TRANSPARENT)
            val s = if (f) 1.08f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
        }
        return card
    }

    private fun iconOf(b: JSONObject): Drawable? {
        val rel = b.optString("icon")
        if (rel.isEmpty()) return null
        val f = File(Store.dir(this), rel)
        if (!f.isFile) return null
        val bmp = BitmapFactory.decodeFile(f.absolutePath) ?: return null
        return BitmapDrawable(resources, bmp)
    }

    // ---------- кнопки ----------
    private fun runButton(b: JSONObject) {
        val d = b.optJSONObject("data") ?: return
        try {
            when (b.optString("type")) {
                "app" -> launchComp(d.optString("comp"))
                "hdmi" -> if (d.optString("mode") == "uri") passthrough(d.optString("id"))
                          else hdmi(d.optString("code").takeLast(1).toIntOrNull() ?: 1)
                "vpn" -> if (d.optString("mode") == "wg") wireguard(d) else {
                    launchComp(d.optString("comp"))
                    toast("Нажмите «Подключить» в VPN-приложении")
                }
                else -> toast("Эта кнопка работает только из ПК-приложения")
            }
        } catch (e: Exception) {
            toast("Не получилось: " + e.message)
        }
    }

    private fun launchComp(comp: String) {
        val i = if ("/" in comp) Intent(Intent.ACTION_MAIN).setComponent(ComponentName.unflattenFromString(comp))
                else packageManager.getLaunchIntentForPackage(comp)
        if (i == null) {
            toast("Приложение не найдено")
            return
        }
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun hdmi(n: Int) {
        val tim = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        val ports = tim.tvInputList
            .filter { it.type == TvInputInfo.TYPE_HDMI && it.isPassthroughInput }
            .sortedBy { it.id.takeLastWhile { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val info = ports.firstOrNull { it.loadLabel(this).toString().replace(" ", "").contains("HDMI$n", true) }
            ?: ports.getOrNull(n - 1)
        if (info == null) toast("Вход HDMI $n не найден") else passthrough(info.id)
    }

    private fun passthrough(id: String) {
        val uri = TvContract.buildChannelUriForPassthroughInput(id)
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun wireguard(d: JSONObject) {
        val act = if (d.optBoolean("up", true)) "UP" else "DOWN"
        val i = Intent("com.wireguard.android.action.SET_TUNNEL_$act")
        i.component = ComponentName("com.wireguard.android", "com.wireguard.android.model.TunnelManager\$IntentReceiver")
        i.putExtra("tunnel", d.optString("tunnel"))
        sendBroadcast(i)
        toast("WireGuard: команда отправлена")
    }
}
