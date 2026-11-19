package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.content.ClipboardManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R

/** First screen: one paste with the hub address and the owner token. Nothing is saved until the hub accepts it. */
@Composable fun LoginScreen(state: UiState, connect: (String) -> Unit, createAccess: (String) -> Unit = {}, join: (String, String) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    var access by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = HubColors.Background, contentColor = HubColors.Text) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding().imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Default.Hub, null, Modifier.size(64.dp), tint = HubColors.Gold)
        Text(stringResource(R.string.login_title), Modifier.padding(top = 12.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.login_subtitle), Modifier.padding(top = 4.dp, bottom = 8.dp), color = HubColors.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        state.error?.let { Text(it.text(), Modifier.fillMaxWidth().padding(top = 4.dp), color = HubColors.Bad) }
        // New users start here; the paste field is only for someone who already has an access.
        CreateAccessCard(state.loading, createAccess)
        Text(stringResource(R.string.login_have_access), Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        OutlinedTextField(access, { access = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.login_access)) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            trailingIcon = { TextButton({
                access = context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            }) { Text(stringResource(R.string.paste)) } })
        Text(stringResource(R.string.login_access_hint, state.hubUrl.removePrefix("https://")), Modifier.fillMaxWidth().padding(top = 6.dp),
            color = HubColors.Muted, fontSize = 13.sp)
        OutlinedButton({ connect(access) }, Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp), enabled = !state.loading && access.isNotBlank()) {
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.login_connect), fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.login_token_hint), Modifier.fillMaxWidth().padding(top = 16.dp), color = HubColors.Muted, fontSize = 12.sp)
        JoinHubCard(state.loading, join)
    }
    }
}
