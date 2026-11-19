package com.zirakusa.gatewayhub.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.PackageScanner
import com.zirakusa.gatewayhub.data.ScannedApp

/** Finds compatible apps and their state without turning anything on (the hub only sends tools/list). */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun ScannerList(apps: List<ScannedApp>?, busy: Boolean, scan: () -> Unit, providers: ProvidersUi = ProvidersUi()) {
    HubCard {
        Text(stringResource(R.string.scanner_title), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(stringResource(R.string.scanner_hint), Modifier.padding(top = 4.dp), color = HubColors.Muted, fontSize = 13.sp)
        Button(scan, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = !busy) {
            Icon(Icons.Default.Radar, null, Modifier.size(18.dp)); Text(stringResource(R.string.scanner_run), Modifier.padding(start = 6.dp))
        }
    }
    apps?.forEach { ScannedCard(it, busy, providers) }
    if (apps?.isEmpty() == true) Text(stringResource(R.string.scanner_none), Modifier.padding(16.dp), color = HubColors.Muted)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ScannedCard(app: ScannedApp, busy: Boolean, providers: ProvidersUi) {
    val context = LocalContext.current
    val local = app.local; val remote = app.remote
    HubCard {
        Text(app.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(listOfNotNull(app.prefix?.let { "$it*" }, local?.packageName, local?.version?.takeIf { it.isNotBlank() }?.let { "v$it" })
            .joinToString(" · "), color = HubColors.Muted, fontSize = 12.sp)
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                local == null && remote?.androidPackage != null -> Pill(stringResource(R.string.scan_not_installed), HubColors.Muted)
                local != null -> Pill(stringResource(R.string.scan_installed), HubColors.Good)
            }
            if (local != null) Pill(stringResource(if (local.sameDeveloper) R.string.scan_verified else R.string.scan_unverified),
                if (local.sameDeveloper) HubColors.Good else HubColors.Bad)
            when {
                remote == null -> Pill(stringResource(R.string.scan_not_in_hub), HubColors.Gold)
                !remote.remote -> Pill(stringResource(R.string.app_ready), HubColors.Good)
                !remote.linked -> Pill(stringResource(R.string.app_unlinked), HubColors.Bad)
                remote.online -> Pill(pluralStringResource(R.plurals.scan_online, remote.tools, remote.tools), HubColors.Good)
                else -> Pill(stringResource(R.string.scan_offline), HubColors.Bad)
            }
        }
        remote?.detail?.takeIf { remote.remote }?.let { Text(it, Modifier.padding(top = 6.dp), color = HubColors.Muted, fontSize = 13.sp) }
        if (local != null) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton({
                // Opens the app's own "connect to the hub" screen; the app decides what to do (nothing is turned on here).
                try { context.startActivity(Intent(PackageScanner.ACTION).addCategory(Intent.CATEGORY_DEFAULT).setPackage(local.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: ActivityNotFoundException) {}
            }) { Text(stringResource(R.string.scan_open)) }
        }
        // Provider apps (local MCP bridge): link, execution packages and a test (only when the app offers the bridge).
        if (local != null && local.bridge && local.sameDeveloper && providers.actions != null) ProviderButtons(local, busy, providers)
    }
}
