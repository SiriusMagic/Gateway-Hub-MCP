package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

@Composable internal fun FeedbackButtons(turn: Turn, state: AssistantState) {
    val id = turn.evidence?.id ?: return
    if (state.feedback == null || turn.musicChoice != null || turn.reply.asksToCall || turn.reply.say == Say.ASK_CALL_CONTACT ||
        turn.reply.say in setOf(Say.CONFIRM, Say.NEED_CALL_PERMISSION)) return
    val vote = state.votes[id]
    IconButton({ state.vote(turn, IntentVote.LIKE) }) {
        Icon(Icons.Default.ThumbUp, stringResource(R.string.intent_like), tint = if (vote == IntentVote.LIKE) HubColors.Gold else HubColors.Muted)
    }
    IconButton({
        if (vote != IntentVote.CORRECTED) state.vote(turn, IntentVote.DISLIKE)
        state.contactNotice = ""
        state.correction = turn
    }) {
        Icon(Icons.Default.ThumbDown, stringResource(R.string.intent_dislike), tint = if (vote != null && vote != IntentVote.LIKE) HubColors.Gold else HubColors.Muted)
    }
}

@Composable internal fun FeedbackDialogs(state: AssistantState) {
    state.correction?.let { turn ->
        var wrong by remember(turn) { mutableStateOf("") }
        var expected by remember(turn) { mutableStateOf("") }
        val alias = turn.evidence?.let { it.plan.steps[it.step].command["who"] }
            ?.takeIf { name -> name.any(Char::isLetter) }
        AlertDialog(
            onDismissRequest = { state.correction = null },
            title = { Text(stringResource(R.string.intent_dislike)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(wrong, { wrong = it.take(800) }, label = { Text(stringResource(R.string.intent_wrong)) })
                    OutlinedTextField(expected, { expected = it.take(800) }, label = { Text(stringResource(R.string.intent_expected)) })
                    if (alias != null && turn.command.action == Action.CALL) {
                        TextButton({ state.chooseContact(alias) }) { Text(stringResource(R.string.intent_alias_choose, alias)) }
                    }
                    if (state.contactNotice.isNotEmpty()) Text(state.contactNotice)
                }
            },
            confirmButton = { TextButton({
                state.vote(turn, if (expected.isBlank()) IntentVote.DISLIKE else IntentVote.CORRECTED, wrong.trim(), expected.trim())
                state.correction = null
            }) { Text(stringResource(R.string.intent_save)) } },
            dismissButton = { TextButton({ state.correction = null }) { Text(stringResource(R.string.local_cancel)) } }
        )
    }
    if (state.editAliases) AlertDialog(
        onDismissRequest = { state.editAliases = false },
        title = { Text(stringResource(R.string.intent_aliases)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (state.savedAliases.isEmpty()) Text(stringResource(R.string.intent_alias_empty))
                for (name in state.savedAliases) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f))
                    TextButton({ state.chooseContact(name) }) { Text(stringResource(R.string.intent_alias_edit)) }
                    TextButton({ state.removeAlias(name) }) { Text(stringResource(R.string.intent_alias_remove)) }
                }
                if (state.contactNotice.isNotEmpty()) Text(state.contactNotice)
            }
        },
        confirmButton = { TextButton({ state.editAliases = false }) { Text(stringResource(R.string.intent_done)) } }
    )
    if (state.feedbackFailed) Text(stringResource(R.string.intent_save_failed), color = HubColors.Bad)
}

@Composable internal fun FeedbackSettings(state: AssistantState) {
    if (state.feedback == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(state.shareFeedback, state::updateSharing)
        Text(stringResource(R.string.intent_share))
    }
    Text(stringResource(R.string.intent_share_details), color = HubColors.Muted)
    TextButton({
        state.contactNotice = ""
        state.savedAliases = state.feedback?.aliases().orEmpty()
        state.editAliases = true
    }) { Text(stringResource(R.string.intent_aliases)) }
}
