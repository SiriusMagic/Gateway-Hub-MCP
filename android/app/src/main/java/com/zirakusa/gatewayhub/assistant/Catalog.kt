package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.BuildConfig

/** A family app the assistant knows: its ids (release and test builds), its name and what people call it. */
data class FamilyApp(val id: String, val name: String, val packages: List<String>, val aliases: List<String>,
    /** A screen inside another app's package (Contactos lives in Teléfono): opened by its class, not the package's icon. */
    val activity: String? = null)

/** The family, and which app does each action. Pure. */
object Catalog {
    private const val STONE_PREVIEW = "com.zirakusa.stone.kotlin.preview.lucia"
    private val regularStone = listOf("com.zirakusa.stone", "com.zirakusa.stone.kotlin.preview")
    private val isLuciaPreview = BuildConfig.APPLICATION_ID == "com.zirakusa.gatewayhub.lucia.preview"
    private val stonePackages = if (isLuciaPreview) listOf(STONE_PREVIEW) + regularStone else regularStone
    internal val stoneMcpPackages = if (isLuciaPreview) listOf(STONE_PREVIEW) else regularStone

    val apps = listOf(
        FamilyApp("stone", "Stone", stonePackages, listOf("stone", "musica", "reproductor", "canciones")),
        FamilyApp("video", "Stone Video", listOf("com.zirakusa.stone.video"), listOf("stone video", "videos", "video")),
        FamilyApp("luma", "Luma", listOf("app.luma.gallery"), listOf("luma", "galeria", "fotos", "imagenes")),
        FamilyApp("camara", "Cámara", listOf("com.zirakusa.camara"), listOf("camara")),
        FamilyApp("reloj", "Reloj", listOf("com.zirakusa.reloj"), listOf("reloj", "alarmas", "cronometro", "temporizador")),
        FamilyApp("calendario", "Calendario", listOf("com.zirakusa.calendario"), listOf("calendario", "agenda")),
        FamilyApp("clima", "Clima", listOf("com.zirakusa.clima"), listOf("clima", "tiempo")),
        FamilyApp("notas", "Notas", listOf("com.zirakusa.notas"), listOf("notas", "tareas", "apuntes")),
        FamilyApp("filem", "Filem", listOf("com.zirakusa.filem"), listOf("filem", "archivos", "descargas")),
        FamilyApp("calculadora", "Calculadora", listOf("com.zirakusa.calculadora"), listOf("calculadora")),
        // Since Teléfono 0.3.0 Contactos is inside Teléfono; the old separate app still works when installed.
        FamilyApp("contactos", "Contactos", listOf("com.zirakusa.telefono", "com.zirakusa.contactos"), listOf("contactos", "agenda de contactos"),
            activity = "com.zirakusa.contactos.MainActivity"),
        FamilyApp("telefono", "Teléfono", listOf("com.zirakusa.telefono"), listOf("telefono", "llamadas", "marcador")),
        FamilyApp("sms", "SMS", listOf("com.zirakusa.sms"), listOf("sms", "mensajes")),
        FamilyApp("qr", "QR", listOf("com.zirakusa.qr"), listOf("qr", "lector de qr", "codigos")),
        FamilyApp("redes", "Redes", listOf("com.zirakusa.redes"), listOf("redes", "analizar redes", "wifi", "internet")),
        FamilyApp("brujula", "Brújula", listOf("com.zirakusa.brujula"), listOf("brujula")),
        FamilyApp("nivel", "Nivel", listOf("com.zirakusa.nivel"), listOf("nivel", "nivel de burbuja")),
        FamilyApp("grabadora", "Grabadora", listOf("com.zirakusa.grabadora"), listOf("grabadora", "grabaciones", "notas de voz")),
        FamilyApp("inicio", "Inicio", listOf("com.zirakusa.launcher"), listOf("inicio", "launcher")),
    )

    /** The app that does an action (null: the assistant itself answers). */
    fun appFor(a: Action): FamilyApp? = when (a) {
        Action.PLAY_MUSIC, Action.PLAY_RECOMMENDED, Action.MEDIA -> byId("stone")
        Action.ALARM, Action.TIMER -> byId("reloj")
        Action.CALL, Action.ANSWER, Action.HANG_UP -> byId("telefono")
        Action.MESSAGE -> byId("sms")
        Action.WEATHER -> byId("clima")
        Action.AGENDA -> byId("calendario")
        Action.PHOTO -> byId("camara")
        Action.SCAN -> byId("qr")
        Action.SPEED -> byId("redes")
        Action.COUNT, Action.NOTE -> byId("notas")
        else -> null
    }

    fun byId(id: String) = apps.first { it.id == id }
    /** The app whose own icon a package is (Teléfono, not the Contactos screen inside it). */
    fun byPackage(pkg: String) = apps.firstOrNull { pkg in it.packages && it.activity == null } ?: apps.firstOrNull { pkg in it.packages }

    /** The app named in a phrase: the longest alias that appears as whole words («stone video» before «stone»). */
    fun find(text: String): FamilyApp? = apps.flatMap { a -> a.aliases.map { it to a } }.sortedByDescending { it.first.length }
        .firstOrNull { (alias, _) -> Regex("(^| )${Regex.escape(alias)}( |$)").containsMatchIn(text) }?.second
}
