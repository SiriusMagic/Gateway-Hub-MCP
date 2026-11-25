package com.zirakusa.gatewayhub

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.ui.HubColors

/**
 * Arka's launcher icon, «Anillos» (default) or «Órbitas»: each an `activity-alias` of [MainActivity]; exactly one is
 * enabled. Alias names never change (Android keeps the enabled state by name).
 */
enum class ArkaIcon(val alias: String, val label: Int, val preview: Int, val background: Long) {
    RINGS("Home", R.string.arka_icon_rings, R.mipmap.ic_launcher_rings_foreground, 0xFF103131),
    ORBITS("HomeOrbitas", R.string.arka_icon_orbits, R.mipmap.ic_launcher_orbits_foreground, 0xFF142E2F);

    private fun component(context: Context) = ComponentName(context.packageName, "com.zirakusa.gatewayhub.$alias")
    fun isEnabled(context: Context): Boolean = when (context.packageManager.getComponentEnabledSetting(component(context))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> this == RINGS            // as the manifest says
        else -> false
    }

    companion object {
        var current by mutableStateOf(RINGS); private set

        /** Reads the enabled one; if none is (an odd update), «Anillos» comes back so Arka never loses its icon. */
        fun load(context: Context) { val on = entries.firstOrNull { it.isEnabled(context) }; if (on == null) apply(context, RINGS) else current = on }

        /** Enables [icon] first and then disables the other, without closing the app. Launchers refresh on their own. */
        fun apply(context: Context, icon: ArkaIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(icon.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(it.component(context), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            current = icon
        }
    }
}

/** Ajustes de Lucía › «Icono de Arka»: a tap changes the icon on the home screen. */
@Composable fun ArkaIconCard() {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(HubColors.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.arka_icon_title), color = HubColors.Text, fontSize = 17.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ArkaIcon.entries.forEach { icon ->
                val on = icon == ArkaIcon.current
                val name = stringResource(icon.label)
                Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = name) { ArkaIcon.apply(context, icon) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Color(icon.background))
                        .border(if (on) 3.dp else 0.dp, if (on) HubColors.Gold else Color.Transparent, RoundedCornerShape(18.dp))) {
                        Image(painterResource(icon.preview), null, Modifier.size(64.dp))
                    }
                    Text(name, Modifier.padding(top = 6.dp), color = if (on) HubColors.Gold else HubColors.Muted, fontSize = 13.sp)
                }
            }
        }
        Text(stringResource(R.string.arka_icon_hint), color = HubColors.Muted, fontSize = 13.sp)
    }
}
