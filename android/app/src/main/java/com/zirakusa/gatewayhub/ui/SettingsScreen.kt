package com.zirakusa.gatewayhub.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.BuildConfig
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.ActivityReport
import com.zirakusa.gatewayhub.data.AppSettings
import com.zirakusa.gatewayhub.data.HubState
import com.zirakusa.gatewayhub.data.ThemeChoice

/** Settings tab: MCP environment and modes, privacy, appearance, language, permissions and help. */
@Composable fun SettingsList(state: UiState, hub: HubState, vm: HubActions) {
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsAllowed = it }
    val askNotifications = { if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { runCatching { context.contentResolver.openOutputStream(it)?.use { out -> out.write(ActivityReport.json(state.hubUrl, hub).toByteArray()) } } }
    }
    val s = state.settings
    EnvironmentSection(hub.environment, s, state.loading, vm, askNotifications)

    PhonesSection(hub.devices, state.pairing, state.hubUrl, state.loading, vm)
    CloudflareSection(state.loading, vm)

    SectionTitle(stringResource(R.string.privacy_title))
    HubCard {
        SwitchRow(stringResource(R.string.block_screenshots), stringResource(R.string.block_screenshots_hint), s.blockScreenshots) { on ->
            vm.updateSettings { it.copy(blockScreenshots = on) }
        }
    }

    SectionTitle(stringResource(R.string.theme_title))
    HubCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ThemeChoice.entries.forEach { t ->
                FilterChip(s.theme == t, { vm.updateSettings { it.copy(theme = t) } }, { Text(stringResource(PALETTES.getValue(t).label)) })
            }
        }
    }

    SectionTitle(stringResource(R.string.language_title))
    HubCard {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppSettings.LANGUAGES.forEach { l ->
                FilterChip(s.language == l, { vm.updateSettings { it.copy(language = l) } }, { Text(languageName(l)) })
            }
        }
    }

    SectionTitle(stringResource(R.string.permissions_title))
    HubCard {
        PermissionRow(stringResource(R.string.perm_internet), stringResource(R.string.perm_internet_hint), true, null)
        PermissionRow(stringResource(R.string.perm_notifications), stringResource(R.string.perm_notifications_hint), notificationsAllowed,
            if (Build.VERSION.SDK_INT >= 33) askNotifications else null)
        PermissionRow(stringResource(R.string.perm_files), stringResource(R.string.perm_files_hint), true, null)
        OutlinedButton({ export.launch("gateway-hub-report.json") }, Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.export_report)) }
        TextButton({ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }) {
            Text(stringResource(R.string.open_android_settings))
        }
    }

    HelpSection()
    Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME), Modifier.padding(top = 12.dp), color = HubColors.Muted, fontSize = 12.sp)
}

@Composable private fun PermissionRow(title: String, hint: String, granted: Boolean, request: (() -> Unit)?) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(hint, color = HubColors.Muted, fontSize = 13.sp)
        }
        if (granted) Pill(stringResource(R.string.perm_granted), HubColors.Good)
        else if (request != null) TextButton(request) { Text(stringResource(R.string.perm_allow)) }
        else Pill(stringResource(R.string.perm_denied), HubColors.Bad)
    }
}

/** Each language in its own words, so it can be found whatever the current one is. */
fun languageName(tag: String) = when (tag) { "en" -> "English"; else -> "Español" }

fun notificationsAllowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
