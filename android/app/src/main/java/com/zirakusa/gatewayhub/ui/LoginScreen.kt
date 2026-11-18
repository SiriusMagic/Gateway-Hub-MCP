package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R

/** First screen: hub address + owner token. Nothing is saved until the hub accepts the token. */
@Composable fun LoginScreen(state: UiState, connect: (String, String) -> Unit) {
    var url by remember { mutableStateOf(state.hubUrl) }
    var token by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = HubColors.Background, contentColor = HubColors.Text) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding().imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Default.Hub, null, Modifier.size(64.dp), tint = HubColors.Gold)
        Text(stringResource(R.string.login_title), Modifier.padding(top = 12.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.login_subtitle), Modifier.padding(top = 4.dp, bottom = 24.dp), color = HubColors.Muted)
        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.login_url)) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth().padding(top = 12.dp),
            label = { Text(stringResource(R.string.login_token)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false))
        Text(stringResource(R.string.login_token_hint), Modifier.fillMaxWidth().padding(top = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        state.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = HubColors.Bad) }
        Button({ connect(url, token) }, Modifier.fillMaxWidth().padding(top = 20.dp).height(50.dp),
            enabled = !state.loading && url.isNotBlank() && token.isNotBlank()) {
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.login_connect), fontWeight = FontWeight.Bold)
        }
    }
}
}
