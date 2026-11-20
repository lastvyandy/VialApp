package cl.vialapp.app.ui

// GUÍA DEL ARCHIVO:
// Orientación de pantallas: algunos juegos giran, pero estudiar no debería obligar a girar el teléfono.

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Normal phone screens are portrait; larger screens retain system rotation. */
internal object AppOrientation {
    private fun normal(activity: Activity): Int =
        if (activity.resources.configuration.smallestScreenWidthDp >= 600)
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    fun restore(activity: Activity) {
        val target = normal(activity)
        if (activity.requestedOrientation != target) activity.requestedOrientation = target
    }

    fun landscape(activity: Activity) {
        val target = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (activity.requestedOrientation != target) activity.requestedOrientation = target
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val next = current.baseContext
        if (next === current) return null
        current = next
    }
    return current as? Activity
}

/** Own landscape for the entire active session, including retries and level changes. */
@Composable
internal fun GameLandscape(active: Boolean) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity, active) {
        if (activity != null) {
            if (active) AppOrientation.landscape(activity) else AppOrientation.restore(activity)
        }
        onDispose {
            if (activity != null && active) AppOrientation.restore(activity)
        }
    }
}
