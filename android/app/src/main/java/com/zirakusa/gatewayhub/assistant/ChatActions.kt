package com.zirakusa.gatewayhub.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
private fun copyMessage(text: String): () -> Unit {
    val context = LocalContext.current
    val label = stringResource(R.string.chat_copy)
    return {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(label, text))
        if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun CopyableMessage(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var menu by remember(text) { mutableStateOf(false) }
    val copy = copyMessage(text)
    val label = stringResource(R.string.chat_copy)
    Box(modifier.pointerInput(text) { detectTapGestures(onLongPress = { menu = true }) }.semantics {
        customActions = listOf(CustomAccessibilityAction(label) { copy(); true })
    }) {
        content()
        DropdownMenu(menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(label) },
                onClick = { menu = false; copy() },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
            )
        }
    }
}

@Composable
internal fun CopyReplyButton(text: String) {
    IconButton(copyMessage(text)) {
        Icon(Icons.Default.ContentCopy, stringResource(R.string.chat_copy_reply), tint = HubColors.Muted)
    }
}

@Composable
internal fun SaveChatButton(state: AssistantState) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var snapshot by rememberSaveable { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val chat = snapshot
        snapshot = null
        if (uri != null && chat != null) scope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException()
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(chat) }
                    true
                } catch (_: IOException) {
                    false
                } catch (_: SecurityException) {
                    false
                }
            }
            Toast.makeText(context, if (saved) R.string.chat_saved else R.string.chat_save_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val speaker = state.name.shown
    val user = stringResource(R.string.chat_you)
    IconButton({
        snapshot = state.turns.joinToString("\n\n") { turn ->
            "$user\n" + turn.heard + "\n\n$speaker\n" + Talk.text(turn.reply, resources)
        }
        save.launch("Chat-" + speaker + "-" + java.time.LocalDate.now() + ".txt")
    }, enabled = state.turns.isNotEmpty() && snapshot == null) {
        Icon(Icons.Default.Save, stringResource(R.string.chat_save), tint = HubColors.Muted)
    }
}
