package com.zirakusa.gatewayhub.assistant

import android.content.Context
import androidx.core.content.edit

/** Lucía's settings: listening for «Oye Lucía», the name, only while charging, spoken answers and «Permitir MCP local». */
class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    var wake: Boolean get() = p.getBoolean("wake", false); set(v) = p.edit { putBoolean("wake", v) }
    var name: WakeName get() = WakeName.from(p.getString("name", null)); set(v) = p.edit { putString("name", v.name) }
    var onlyCharging: Boolean get() = p.getBoolean("charging", false); set(v) = p.edit { putBoolean("charging", v) }
    var mode: AnswerMode get() = AnswerMode.from(p.getString("mode", null)); set(v) = p.edit { putString("mode", v.name) }
    var rulesWhenLocked: Boolean get() = p.getBoolean("rules_locked", true); set(v) = p.edit { putBoolean("rules_locked", v) }
    var interpreter: Interpreter get() = Interpreter.from(p.getString("interpreter", null)); set(v) = p.edit { putString("interpreter", v.name) }
    var voiceAnswers: Boolean get() = p.getBoolean("voice", true); set(v) = p.edit { putBoolean("voice", v) }
    /** Arka › MCP › «Permitir MCP local»: Lucía may run the family apps' tools on the phone. Off by default. */
    var localMcp: Boolean get() = p.getBoolean("local_mcp", false); set(v) = p.edit { putBoolean("local_mcp", v) }
}
