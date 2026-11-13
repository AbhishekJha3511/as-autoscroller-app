package expo.modules.autoscrollcontroller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class AutoScrollService : AccessibilityService() {

    companion object {
        @Volatile var instance: AutoScrollService? = null
        private const val START_DELAY_MS = 700L
        private const val MIN_GAP_MS = 500L
        private const val RETRY_MS = 600L
        private const val BASE_PX_PER_SEC = 4000f
        private val CTRL_KEYS = setOf("c_size", "c_alpha", "c_side", "c_peek", "c_visible")
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var wm: WindowManager

    // controller UI
    private var root: LinearLayout? = null
    private var playBtn: TextView? = null
    private var lp = WindowManager.LayoutParams()
    private var collapsed = false          // true = only a small edge handle is shown

    // scroll state
    var running = false
        private set
    private var inFlight = false
    private var session = 0
    private var lastEnd = 0L
    private var failCount = 0
    private var lastDelaySec = -1

    private val tick = Runnable { onTick() }
    private val rebuildRun = Runnable { rebuild() }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in CTRL_KEYS) {
            // size / transparency / side / peek changed -> redraw controller live
            handler.removeCallbacks(rebuildRun)
            handler.postDelayed(rebuildRun, 60)
        } else if (key == "delay" && prefs.delaySec != lastDelaySec) {
            lastDelaySec = prefs.delaySec
            if (running && !inFlight) {
                schedule((lastEnd + delayMs() - SystemClock.uptimeMillis()).coerceAtLeast(100L))
            }
        }
    }

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { stopScrolling() }
    }

    override fun onServiceConnected() {
        instance = this
        prefs = Prefs(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lastDelaySec = prefs.delaySec
        prefs.sp.unregisterOnSharedPreferenceChangeListener(prefListener)
        prefs.sp.registerOnSharedPreferenceChangeListener(prefListener)
        runCatching { unregisterReceiver(screenOff) }
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        collapsed = false
        rebuild()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        stopScrolling()
        prefs.sp.unregisterOnSharedPreferenceChangeListener(prefListener)
        runCatching { unregisterReceiver(screenOff) }
        handler.removeCallbacks(rebuildRun)
        removeController()
        instance = null
        return super.onUnbind(intent)
    }

    // ---------- scheduling ----------

    private fun delayMs() = max(MIN_GAP_MS, prefs.delaySec * 1000L)

    private fun schedule(ms: Long) {
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, ms)
    }

    fun startScrolling() {
        if (running) return
        running = true
        session++
        failCount = 0
        lastEnd = SystemClock.uptimeMillis()
        setKeepAwake(true)
        updateIcon()
        schedule(max(START_DELAY_MS, delayMs()))   // first swipe also waits the full delay
    }

    fun stopScrolling() {
        running = false
        session++
        handler.removeCallbacks(tick)
        setKeepAwake(false)
        updateIcon()
    }

    private fun onTick() {
        if (!running) return
        if (inFlight) { schedule(100L); return }
        swipe(session)
    }

    private fun finish(sid: Int, cancelled: Boolean) {
        inFlight = false
        if (!running || sid != session) return
        if (cancelled) { if (++failCount >= 5) { stopScrolling(); return } } else failCount = 0
        lastEnd = SystemClock.uptimeMillis()
        schedule(if (cancelled) RETRY_MS else delayMs())
    }

    // ---------- swipe ----------

    private fun screenSize(): Pair<Float, Float> {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        return dm.widthPixels.toFloat() to dm.heightPixels.toFloat()
    }

    private fun swipe(sid: Int) {
        val (w, h) = screenSize()
        val dir = prefs.direction
        val horizontal = dir == "LEFT" || dir == "RIGHT"
        val frac = min(prefs.areaPct / 100f, if (horizontal) 0.8f else 0.9f)
        val len = (if (horizontal) w else h) * frac
        val half = len / 2f

        val jitter = (Random.nextFloat() - 0.5f) * 0.04f
        val cx = w / 2f + if (horizontal) 0f else w * jitter
        val cy = h / 2f + if (horizontal) h * jitter else 0f

        val path = Path()
        when (dir) {   // DOWN = next video (finger moves up), UP = previous video
            "UP" -> { path.moveTo(cx, cy - half); path.lineTo(cx, cy + half) }
            "LEFT" -> { path.moveTo(cx + half, cy); path.lineTo(cx - half, cy) }
            "RIGHT" -> { path.moveTo(cx - half, cy); path.lineTo(cx + half, cy) }
            else -> { path.moveTo(cx, cy + half); path.lineTo(cx, cy - half) }
        }

        val velocity = BASE_PX_PER_SEC * (prefs.speedX10 / 10f)
        val duration = (len / velocity * 1000f).toLong().coerceIn(50L, 1500L)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        inFlight = true
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) = finish(sid, false)
            override fun onCancelled(g: GestureDescription?) = finish(sid, true)
        }, null)
        if (!ok) finish(sid, true)
    }

    // ---------- edge controller ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Bring the controller back after ✕ (called from the app). */
    fun showController() {
        prefs.ctrlVisible = true
        collapsed = false
        handler.removeCallbacks(rebuildRun)
        rebuild()
    }

    private fun closeController() {
        stopScrolling()
        prefs.ctrlVisible = false
        handler.removeCallbacks(rebuildRun)
        removeController()
    }

    private fun removeController() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        playBtn = null
    }

    private fun rebuild() {
        removeController()
        if (!prefs.ctrlVisible) return
        buildController()
    }

    private fun buildController() {
        val s = dp(prefs.ctrlSize)
        val right = prefs.ctrlSide != "LEFT"
        val textSp = prefs.ctrlSize * 0.42f
        val w = if (collapsed) (s * 0.5f).toInt() else s
        val h = if (collapsed) (s * 1.1f).toInt() else s * 3

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = prefs.ctrlAlpha / 100f
            background = GradientDrawable().apply {
                setColor(Color.rgb(30, 30, 30))
                cornerRadius = if (collapsed) w * 0.5f else s / 2f
            }
        }

        fun btn(label: String, color: Int, bw: Int, bh: Int, onTap: () -> Unit): TextView {
            val t = TextView(this).apply {
                text = label
                textSize = textSp
                setTextColor(color)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(bw, bh)
            }
            attachTouch(t, onTap)
            panel.addView(t)
            return t
        }

        if (collapsed) {
            // small edge handle: tap to expand
            btn(if (right) "‹" else "›", Color.WHITE, w, h) { collapsed = false; rebuild() }
        } else {
            playBtn = btn(if (running) "❚❚" else "▶", Color.WHITE, s, s) {
                if (running) stopScrolling() else startScrolling()
            }
            btn(if (right) "»" else "«", Color.WHITE, s, s) { collapsed = true; rebuild() }   // hide
            btn("✕", Color.rgb(255, 138, 128), s, s) { closeController() }                    // close
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (running) WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON else 0)
        lp = WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or (if (right) Gravity.END else Gravity.START)
            val peek = if (collapsed) 0.3f else prefs.ctrlPeek / 100f
            x = -(w * peek).toInt()                       // negative = pushed off the screen edge
            y = prefs.controllerY.coerceIn(0, max(0, screenSize().second.toInt() - h))
        }
        root = panel
        runCatching { wm.addView(panel, lp) }
    }

    /** Shared by every button: drag up/down to move the whole controller, tap to act. */
    private fun attachTouch(v: View, onTap: () -> Unit) {
        var downY = 0f
        var startY = 0
        var moved = false
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; startY = lp.y; moved = false; v.alpha = 0.6f }
                MotionEvent.ACTION_MOVE -> {
                    val dy = e.rawY - downY
                    if (abs(dy) > dp(8)) moved = true
                    if (moved) {
                        val maxY = max(0, screenSize().second.toInt() - lp.height)
                        lp.y = (startY + dy).toInt().coerceIn(0, maxY)
                        root?.let { runCatching { wm.updateViewLayout(it, lp) } }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    v.alpha = 1f
                    if (moved) prefs.controllerY = lp.y else handler.post { onTap() }
                }
                MotionEvent.ACTION_CANCEL -> v.alpha = 1f
            }
            true
        }
    }

    private fun setKeepAwake(on: Boolean) {
        val v = root ?: return
        lp.flags = if (on) lp.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        else lp.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        runCatching { wm.updateViewLayout(v, lp) }
    }

    private fun updateIcon() {
        playBtn?.text = if (running) "❚❚" else "▶"
    }
}
