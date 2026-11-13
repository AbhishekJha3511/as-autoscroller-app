
package expo.modules.autoscrollcontroller

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class AutoScrollControllerModule : Module() {
  private val main = Handler(Looper.getMainLooper())

  private val ctx
    get() = appContext.reactContext
      ?: throw IllegalStateException("React context is not available")

  override fun definition() = ModuleDefinition {
    Name("AutoScrollController")

    Function("isServiceEnabled") {
      AutoScrollService.instance != null
    }

    Function("isRunning") {
      AutoScrollService.instance?.running ?: false
    }

    Function("openAccessibilitySettings") {
      ctx.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }

    Function("getConfig") {
      val p = Prefs(ctx)
      mapOf<String, Any>(
        "delaySec" to p.delaySec,
        "areaPct" to p.areaPct,
        "speedX10" to p.speedX10,
        "direction" to p.direction
      )
    }

    Function("getCustomize") {
      val p = Prefs(ctx)
      mapOf<String, Any>(
        "size" to p.ctrlSize,
        "alpha" to p.ctrlAlpha,
        "peek" to p.ctrlPeek,
        "side" to p.ctrlSide
      )
    }

    Function("setCustomize") { size: Int, alpha: Int, peek: Int, side: String ->
      val p = Prefs(ctx)
      p.ctrlSize = size
      p.ctrlAlpha = alpha
      p.ctrlPeek = peek
      p.ctrlSide = side
    }

    // Bring the controller back after the user presses X
    Function("showController") {
      main.post {
        AutoScrollService.instance?.showController()
      }
      Unit
    }

    Function("setConfig") { delaySec: Int, areaPct: Int, speedX10: Int, direction: String ->
      val p = Prefs(ctx)
      p.delaySec = delaySec
      p.areaPct = areaPct
      p.speedX10 = speedX10
      p.direction = direction
    }

    // Run service operations on the main thread
    Function("start") {
      main.post {
        AutoScrollService.instance?.startScrolling()
      }
      Unit
    }

    Function("stop") {
      main.post {
        AutoScrollService.instance?.stopScrolling()
      }
      Unit
    }
  }
}
