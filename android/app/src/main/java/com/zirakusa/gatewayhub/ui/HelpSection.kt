package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R

/** Help: how to connect an AI, a third-party app, and what the environment states and modes mean. */
@Composable fun HelpSection() {
    SectionTitle(stringResource(R.string.help_title))
    HelpCard(R.string.help_ai_title, R.string.help_ai_body)
    HelpCard(R.string.help_third_title, R.string.help_third_body)
    HelpCard(R.string.help_dev_title, R.string.help_dev_body)
    HelpCard(R.string.help_env_title, R.string.help_env_body)
}

@Composable private fun HelpCard(title: Int, body: Int) {
    var open by remember { mutableStateOf(false) }
    HubCard(Modifier.clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = HubColors.Muted)
        }
        if (open) Text(stringResource(body), Modifier.padding(top = 8.dp), color = HubColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
