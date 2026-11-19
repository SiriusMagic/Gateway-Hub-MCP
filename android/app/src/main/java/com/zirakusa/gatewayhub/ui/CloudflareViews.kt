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

/** Login: no hub_ token? The app deploys or updates the hub with the Cloudflare token (used once, never kept). */
@Composable fun CreateAccessCard(loading: Boolean, createAccess: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    HubCard(Modifier.padding(top = 24.dp)) {
        Text(stringResource(R.string.cf_login_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(stringResource(R.string.cf_login_hint), Modifier.padding(vertical = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        CreateCloudflareTokenButton()
        Spacer(Modifier.height(8.dp))
        CloudflareField(token) { token = it }
        Button({ createAccess(token); token = "" }, Modifier.fillMaxWidth().padding(top = 10.dp).height(52.dp), enabled = !loading && token.isNotBlank()) {
            Text(stringResource(R.string.cf_create_access), fontWeight = FontWeight.Bold)
        }
    }
}

/** Settings › Cloudflare: update the hub to this app's version, or make a new owner token. Nothing is stored. */
@Composable fun CloudflareSection(loading: Boolean, vm: HubActions) {
    var token by remember { mutableStateOf("") }
    var rotate by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.cf_title))
    HubCard {
        Text(stringResource(R.string.cf_hint), Modifier.padding(bottom = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        CloudflareField(token) { token = it }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ vm.updateHub(token); token = "" }, enabled = !loading && token.isNotBlank()) { Text(stringResource(R.string.cf_deploy)) }
            CreateCloudflareTokenButton()
        }
        TextButton({ rotate = true }, enabled = !loading && token.isNotBlank()) { Text(stringResource(R.string.cf_rotate_owner)) }
    }
    if (rotate) AlertDialog(onDismissRequest = { rotate = false }, title = { Text(stringResource(R.string.cf_rotate_owner)) },
        text = { Text(stringResource(R.string.cf_rotate_confirm)) },
        confirmButton = { TextButton({ rotate = false; vm.rotateOwner(token); token = "" }) { Text(stringResource(R.string.cf_rotate_owner)) } },
        dismissButton = { TextButton({ rotate = false }) { Text(stringResource(R.string.cancel)) } })
}
