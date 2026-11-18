package com.zirakusa.gatewayhub.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** An app on this phone that declares support for the Gateway Hub (only what it declares; nothing is started). */
data class LocalApp(val packageName: String, val label: String, val version: String, val prefix: String?, val sameDeveloper: Boolean,
    /** content:// authority that gives the hub its link (Redirección hub); null when the app has none. */
    val linkAuthority: String? = null,
    /** It offers the local MCP bridge (a service with [LocalBridge.ACTION]). */
    val bridge: Boolean = false)

/** What the hub says about one app (from /api/admin/scan). */
data class RemoteStatus(val prefix: String, val title: String, val androidPackage: String?, val remote: Boolean,
    val linked: Boolean, val online: Boolean, val tools: Int, val detail: String)

/** One row of the scanner: the app on the phone and/or the hub's module. */
data class ScannedApp(val title: String, val prefix: String?, val local: LocalApp?, val remote: RemoteStatus?)

fun interface LocalAppSource { fun installed(): List<LocalApp> }

/**
 * Finds compatible apps with one targeted query (the ACTION intent declared in <queries>), without
 * the "see all apps" permission. An app is "same developer" only when Android confirms it is signed
 * with the same key as this app: a look-alike cannot pass for Stone.
 */
class PackageScanner(private val context: Context) : LocalAppSource {
    override fun installed(): List<LocalApp> {
        val pm = context.packageManager
        val found = pm.queryIntentActivities(Intent(ACTION).addCategory(Intent.CATEGORY_DEFAULT), PackageManager.GET_META_DATA)
        return found.mapNotNull { it.activityInfo }.distinctBy { it.packageName }.map { info ->
            val pkg = info.packageName
            val prefix = info.metaData?.getString(META_PREFIX)?.takeIf { p -> Regex("^[a-z][a-z0-9]{0,15}_$").matches(p) }
            val version = runCatching { pm.getPackageInfo(pkg, 0).versionName }.getOrNull().orEmpty()
            // The authority must belong to that same package: an app cannot point the hub at another app's provider.
            val authority = info.metaData?.getString(META_LINK)?.takeIf { it.startsWith("$pkg.") && Regex("^[A-Za-z0-9._]+$").matches(it) }
            val bridge = pm.queryIntentServices(Intent(LocalBridge.ACTION).setPackage(pkg), 0).isNotEmpty()
            LocalApp(pkg, info.applicationInfo.loadLabel(pm).toString().take(60), version, prefix,
                pm.checkSignatures(context.packageName, pkg) == PackageManager.SIGNATURE_MATCH, authority, bridge)
        }
    }

    companion object {
        /** Declared by compatible apps (Stone: MainActivity) together with [META_PREFIX]. */
        const val ACTION = "com.zirakusa.gatewayhub.action.APP"
        const val META_PREFIX = "com.zirakusa.gatewayhub.module"
        const val META_LINK = "com.zirakusa.gatewayhub.link"
    }
}

/** Joins phone apps and hub modules by package (or prefix): installed-only, hub-only, or both. */
object ScanMerge {
    fun merge(local: List<LocalApp>, remote: List<RemoteStatus>): List<ScannedApp> {
        val used = mutableSetOf<String>()
        val rows = remote.map { r ->
            val app = local.firstOrNull { it.packageName == r.androidPackage } ?: local.firstOrNull { it.prefix != null && it.prefix == r.prefix }
            app?.let { used += it.packageName }
            ScannedApp(r.title, r.prefix, app, r)
        }
        return rows + local.filter { it.packageName !in used }.map { ScannedApp(it.label, it.prefix, it, null) }
    }
}
