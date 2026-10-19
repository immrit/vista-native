package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.abs

/**
 * Stores and retrieves the measured software keyboard height.
 * Telegram-grade: persists across app restarts, adapts dynamically to portrait vs landscape,
 * and falls back to responsive defaults if the software keyboard has not yet been opened.
 */
object KeyboardHeightStore {
    private const val PREFS_NAME = "vista_chat_keyboard_prefs"
    private const val KEY_PORTRAIT = "kbd_height_portrait_dp"
    private const val KEY_LANDSCAPE = "kbd_height_landscape_dp"

    private const val DEFAULT_PORTRAIT_DP = 290f
    private const val DEFAULT_LANDSCAPE_DP = 190f

    private const val MIN_PORTRAIT_DP = 160f
    private const val MAX_PORTRAIT_DP = 480f

    private const val MIN_LANDSCAPE_DP = 100f
    private const val MAX_LANDSCAPE_DP = 300f

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getKeyboardHeightDp(context: Context, isLandscape: Boolean, screenHeightDp: Float): Float {
        val prefs = prefs(context)
        val key = if (isLandscape) KEY_LANDSCAPE else KEY_PORTRAIT
        val defaultVal = if (isLandscape) {
            (screenHeightDp * 0.45f).coerceIn(MIN_LANDSCAPE_DP, MAX_LANDSCAPE_DP)
        } else {
            (screenHeightDp * 0.38f).coerceIn(DEFAULT_PORTRAIT_DP, MAX_PORTRAIT_DP)
        }
        val saved = prefs.getFloat(key, defaultVal)
        val min = if (isLandscape) MIN_LANDSCAPE_DP else MIN_PORTRAIT_DP
        val max = if (isLandscape) {
            (screenHeightDp * 0.65f).coerceAtLeast(MAX_LANDSCAPE_DP)
        } else {
            (screenHeightDp * 0.55f).coerceAtLeast(MAX_PORTRAIT_DP)
        }
        return saved.coerceIn(min, max)
    }

    fun saveKeyboardHeightDp(context: Context, isLandscape: Boolean, heightDp: Float, screenHeightDp: Float) {
        val min = if (isLandscape) MIN_LANDSCAPE_DP else MIN_PORTRAIT_DP
        val max = if (isLandscape) {
            (screenHeightDp * 0.65f).coerceAtLeast(MAX_LANDSCAPE_DP)
        } else {
            (screenHeightDp * 0.55f).coerceAtLeast(MAX_PORTRAIT_DP)
        }
        if (heightDp in min..max) {
            val key = if (isLandscape) KEY_LANDSCAPE else KEY_PORTRAIT
            val current = prefs(context).getFloat(key, 0f)
            if (abs(current - heightDp) >= 2f) {
                prefs(context).edit().putFloat(key, heightDp).apply()
            }
        }
    }
}
