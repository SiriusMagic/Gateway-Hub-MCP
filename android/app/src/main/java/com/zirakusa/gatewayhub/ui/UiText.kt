package com.zirakusa.gatewayhub.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.HubException

/** A message for the screen, resolved in the app's language when it is shown (the hub's own words stay as sent). */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val text: String) : UiText
    data class Lines(val lines: List<UiText>) : UiText

    fun resolve(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray())
        is Raw -> text
        is Lines -> lines.joinToString("\n") { it.resolve(context) }
    }
}

@Composable fun UiText.text(): String { LocalConfiguration.current; return resolve(LocalContext.current) }

/** Codes this app produces itself (or the hub's HTTP layer), translated; anything else keeps the hub's message. */
private val CODES = mapOf(
    "NETWORK" to R.string.err_network, "UNAUTHORIZED" to R.string.err_unauthorized, "RATE_LIMITED" to R.string.err_rate_limited,
    "INVALID_URL" to R.string.err_invalid_url, "EMPTY" to R.string.err_empty, "STONE_LINK" to R.string.err_stone_link,
    "AGENT_LINK" to R.string.err_agent_link, "NO_TOKEN" to R.string.err_no_token,
    "CF_INVALID" to R.string.err_cf_invalid, "CF_NETWORK" to R.string.err_cf_network, "CF_INACTIVE" to R.string.err_cf_inactive, "CF_FORBIDDEN" to R.string.err_cf_forbidden,
    "BRIDGE_UNSUPPORTED" to R.string.bridge_unsupported, "BRIDGE_DENIED" to R.string.bridge_denied, "BRIDGE_LOST" to R.string.bridge_lost,
    "BRIDGE_TIMEOUT" to R.string.bridge_timeout, "BRIDGE_TOO_LARGE" to R.string.bridge_too_large, "RESPONSE_TOO_LARGE" to R.string.bridge_too_large,
    "BRIDGE_BAD_ANSWER" to R.string.bridge_bad_answer, "REDIRECT_OFF" to R.string.bridge_redirect_off, "API_OFF" to R.string.bridge_api_off,
    "NOT_GATEWAY" to R.string.bridge_denied, "NOT_SAME_DEVELOPER" to R.string.bridge_denied, "BUSY" to R.string.bridge_busy,
    "NOT_LINKED" to R.string.provider_not_linked, "DISCONNECTED_BY_USER" to R.string.provider_cut,
    "PACKAGE_PENDING" to R.string.package_pending_error, "PACKAGE_TOO_MANY" to R.string.package_too_many,
    "CF_EMPTY" to R.string.err_cf_empty, "CF_MISSING" to R.string.err_cf_missing, "NO_HUB" to R.string.err_no_hub,
)
private val LINK_CODES = mapOf(
    "NOT_SUPPORTED" to R.string.link_not_supported, "NO_ANSWER" to R.string.link_no_answer,
    "NO_PERMISSION" to R.string.link_no_permission, "FAILED" to R.string.link_failed,
)

fun HubException.toUiText(): UiText = CODES[code]?.let { UiText.Res(it) } ?: UiText.Raw(message ?: code)

/** Why an app did not give its link: its own words, or ours for our codes. */
fun linkFailure(label: String, error: String?, message: String?): UiText =
    UiText.Res(R.string.link_failure, listOf(label, message?.let { UiText.Raw(it) } ?: LINK_CODES[error]?.let { UiText.Res(it) } ?: UiText.Raw(error ?: "?")))
