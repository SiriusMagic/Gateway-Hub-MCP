package com.zirakusa.gatewayhub.assistant

import android.content.Context
import androidx.core.content.edit

/** Lucía's settings: listening for «Oye Lucía», the name, only while charging, and spoken answers. */
class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    var wake: Boolean get() = p.getBoolean("wake", false); set(v) = p.edit { putBoolean("wake", v) }
    var name: WakeName get() = WakeName.from(p.getString("name", null)); set(v) = p.edit { putString("name", v.name) }
    var onlyCharging: Boolean get() = p.getBoolean("charging", false); set(v) = p.edit { putBoolean("charging", v) }
    var voiceAnswers: Boolean get() = p.getBoolean("voice", true); set(v) = p.edit { putBoolean("voice", v) }
}
