package com.zirakusa.gatewayhub.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.CloudflareApi

/** One line on the saved Cloudflare token: none, its end date and days left, expired, or no end date. */
@Composable fun CloudflareStatus(info: CloudflareInfo?) {
    val days = info?.daysLeft()
    val (text, color) = when {
        info == null -> stringResource(R.string.cf_none) to HubColors.Muted
        days == null -> stringResource(R.string.cf_no_expiry) to HubColors.Good
        days < 0 -> stringResource(R.string.cf_expired) to HubColors.Bad
        else -> pluralStringRes(R.plurals.cf_expires_in, days.toInt(), formatDate(info.expiresAt, "")) to if (days <= 2) HubColors.Gold else HubColors.Good
    }
    Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable private fun pluralStringRes(id: Int, count: Int, date: String) =
    LocalContext.current.resources.getQuantityString(id, count, count, date)

@Composable fun CreateCloudflareTokenButton() {
    val context = LocalContext.current
    OutlinedButton({ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, CloudflareApi.CREATE_URL.toUri())) } }) {
        Text(stringResource(R.string.cf_create_token))
    }
}

/** A secret field for the Cloudflare token (never shown). */
@Composable fun CloudflareField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.cf_token)) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false))
}

/** Login: no hub_ token? Make one with the person's Cloudflare token. */
@Composable fun CreateAccessCard(info: CloudflareInfo?, loading: Boolean, createAccess: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    HubCard(Modifier.padding(top = 24.dp)) {
        Text(stringResource(R.string.cf_login_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(stringResource(R.string.cf_login_hint), Modifier.padding(vertical = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        CreateCloudflareTokenButton()
        if (info != null) CloudflareStatus(info)
        Spacer(Modifier.height(8.dp))
        CloudflareField(token) { token = it }
        Button({ createAccess(token) }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = !loading && (token.isNotBlank() || info != null)) {
            Text(stringResource(R.string.cf_create_access), fontWeight = FontWeight.Bold)
        }
    }
}

/** Settings › Cloudflare: update the token (it is shared with Stone), make a new owner token, forget. */
@Composable fun CloudflareSection(info: CloudflareInfo?, loading: Boolean, vm: HubActions) {
    var token by remember { mutableStateOf("") }
    var rotate by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.cf_title))
    HubCard {
        CloudflareStatus(info)
        Text(stringResource(R.string.cf_hint), Modifier.padding(vertical = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        CloudflareField(token) { token = it }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ vm.saveCloudflare(token); token = "" }, enabled = !loading && token.isNotBlank()) { Text(stringResource(R.string.cf_update)) }
            CreateCloudflareTokenButton()
        }
        if (info != null) {
            TextButton({ rotate = true }, enabled = !loading) { Text(stringResource(R.string.cf_rotate_owner)) }
            TextButton(vm::forgetCloudflare) { Text(stringResource(R.string.cf_forget), color = HubColors.Bad) }
        }
    }
    if (rotate) AlertDialog(onDismissRequest = { rotate = false }, title = { Text(stringResource(R.string.cf_rotate_owner)) },
        text = { Text(stringResource(R.string.cf_rotate_confirm)) },
        confirmButton = { TextButton({ rotate = false; vm.createAccess("") }) { Text(stringResource(R.string.cf_rotate_owner)) } },
        dismissButton = { TextButton({ rotate = false }) { Text(stringResource(R.string.cancel)) } })
}

/** Shown on every tab when the Cloudflare token ends in 2 days or less. */
@Composable fun CloudflareExpiryBanner(info: CloudflareInfo?) {
    val days = info?.daysLeft() ?: return
    if (days > 2) return
    HubCard { CloudflareStatus(info); Text(stringResource(R.string.cf_banner), color = HubColors.Text, fontSize = 13.sp) }
}
