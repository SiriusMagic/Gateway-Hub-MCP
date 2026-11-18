package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.data.AppSettings

/** First launch only: the language, asked in both languages. It can be changed later in Settings. */
@Composable fun WelcomeScreen(choose: (String) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = HubColors.Background, contentColor = HubColors.Text) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Hub, null, Modifier.size(64.dp), tint = HubColors.Gold)
            Text("Gateway Hub", Modifier.padding(top = 12.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Elige tu idioma\nChoose your language", Modifier.padding(top = 8.dp, bottom = 24.dp), color = HubColors.Muted, textAlign = TextAlign.Center)
            AppSettings.LANGUAGES.forEach { tag ->
                Button({ choose(tag) }, Modifier.fillMaxWidth().padding(vertical = 6.dp).height(50.dp)) {
                    Text(languageName(tag), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
