package expo.modules.autoscrollcontroller

import android.content.Context

class Prefs(ctx: Context) {
    val sp = ctx.getSharedPreferences("autoscroll", Context.MODE_PRIVATE)

    /** seconds between swipes, 0..180 */
    var delaySec: Int
        get() = sp.getInt("delay", 5)
        set(v) = sp.edit().putInt("delay", v).apply()

    /** % of screen scrolled per swipe, 10..100 */
    var areaPct: Int
        get() = sp.getInt("area", 50)
        set(v) = sp.edit().putInt("area", v).apply()

    /** speed multiplier x10 (5 = 0.5x, 10 = 1x, 30 = 3x) */
    var speedX10: Int
        get() = sp.getInt("speed", 10)
        set(v) = sp.edit().putInt("speed", v).apply()

    /** DOWN, UP, LEFT, RIGHT */
    var direction: String
        get() = sp.getString("dir", "DOWN") ?: "DOWN"
        set(v) = sp.edit().putString("dir", v).apply()

    var controllerY: Int
        get() = sp.getInt("cy", 600)
        set(v) = sp.edit().putInt("cy", v).apply()

    // ---- controller customisation ----
    /** button size in dp, 36..80 */
    var ctrlSize: Int
        get() = sp.getInt("c_size", 48)
        set(v) = sp.edit().putInt("c_size", v).apply()

    /** opacity %, 20..100 */
    var ctrlAlpha: Int
        get() = sp.getInt("c_alpha", 85)
        set(v) = sp.edit().putInt("c_alpha", v).apply()

    /** % of the controller hidden off the screen edge, 0..60 */
    var ctrlPeek: Int
        get() = sp.getInt("c_peek", 25)
        set(v) = sp.edit().putInt("c_peek", v).apply()

    /** RIGHT or LEFT */
    var ctrlSide: String
        get() = sp.getString("c_side", "RIGHT") ?: "RIGHT"
        set(v) = sp.edit().putString("c_side", v).apply()

    /** false after the user pressed ✕ (bring it back from the app) */
    var ctrlVisible: Boolean
        get() = sp.getBoolean("c_visible", true)
        set(v) = sp.edit().putBoolean("c_visible", v).apply()

    fun resetCustomize() {
        sp.edit().putInt("c_size", 48).putInt("c_alpha", 85).putInt("c_peek", 25)
            .putString("c_side", "RIGHT").apply()
    }
}
