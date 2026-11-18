package com.zirakusa.gatewayhub.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun HubCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HubColors.Card)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable fun Pill(text: String, color: Color) {
    Text(text, Modifier.border(1.dp, color, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp),
        color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 12.dp, bottom = 4.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HubColors.Text)
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
/** A date in the phone's time zone, or [never] for 0. */
fun formatDate(ms: Long, never: String, zone: ZoneId = ZoneId.systemDefault()): String =
    if (ms <= 0) never else dateFormat.format(Instant.ofEpochMilli(ms).atZone(zone))

/** Copies a secret; Android 13+ hides it from the clipboard preview. */
fun copySecret(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    if (Build.VERSION.SDK_INT >= 33) clip.description.extras = PersistableBundle().apply {
        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}
