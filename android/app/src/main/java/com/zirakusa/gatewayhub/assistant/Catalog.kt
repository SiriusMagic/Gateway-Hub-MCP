package com.zirakusa.gatewayhub.assistant

/** A family app the assistant knows: its ids (release and test builds), its name and what people call it. */
data class FamilyApp(val id: String, val name: String, val packages: List<String>, val aliases: List<String>)

/** The family, and which app does each action. Pure. */
object Catalog {
    val apps = listOf(
        FamilyApp("stone", "Stone", listOf("com.zirakusa.stone", "com.zirakusa.stone.kotlin.preview"), listOf("stone", "musica", "reproductor", "canciones")),
        FamilyApp("video", "Stone Video", listOf("com.zirakusa.stone.video"), listOf("stone video", "videos", "video")),
        FamilyApp("luma", "Luma", listOf("app.luma.gallery"), listOf("luma", "galeria", "fotos", "imagenes")),
        FamilyApp("camara", "Cámara", listOf("com.zirakusa.camara"), listOf("camara")),
        FamilyApp("reloj", "Reloj", listOf("com.zirakusa.reloj"), listOf("reloj", "alarmas", "cronometro", "temporizador")),
        FamilyApp("calendario", "Calendario", listOf("com.zirakusa.calendario"), listOf("calendario", "agenda")),
        FamilyApp("clima", "Clima", listOf("com.zirakusa.clima"), listOf("clima", "tiempo")),
        FamilyApp("notas", "Notas", listOf("com.zirakusa.notas"), listOf("notas", "tareas", "apuntes")),
        FamilyApp("filem", "Filem", listOf("com.zirakusa.filem"), listOf("filem", "archivos", "descargas")),
        FamilyApp("calculadora", "Calculadora", listOf("com.zirakusa.calculadora"), listOf("calculadora")),
        FamilyApp("contactos", "Contactos", listOf("com.zirakusa.contactos"), listOf("contactos", "agenda de contactos")),
        FamilyApp("telefono", "Teléfono", listOf("com.zirakusa.telefono"), listOf("telefono", "llamadas", "marcador")),
        FamilyApp("sms", "SMS", listOf("com.zirakusa.sms"), listOf("sms", "mensajes")),
        FamilyApp("qr", "QR", listOf("com.zirakusa.qr"), listOf("qr", "lector de qr", "codigos")),
        FamilyApp("redes", "Redes", listOf("com.zirakusa.redes"), listOf("redes", "analizar redes", "wifi", "internet")),
        FamilyApp("brujula", "Brújula", listOf("com.zirakusa.brujula"), listOf("brujula")),
        FamilyApp("nivel", "Nivel", listOf("com.zirakusa.nivel"), listOf("nivel", "nivel de burbuja")),
        FamilyApp("inicio", "Inicio", listOf("com.zirakusa.launcher"), listOf("inicio", "launcher")),
    )

    /** The app that does an action (null: the assistant itself answers). */
    fun appFor(a: Action): FamilyApp? = when (a) {
        Action.PLAY_MUSIC, Action.PLAY_RECOMMENDED, Action.MEDIA -> byId("stone")
        Action.ALARM, Action.TIMER -> byId("reloj")
        Action.CALL -> byId("telefono")
        Action.MESSAGE -> byId("sms")
        Action.WEATHER -> byId("clima")
        Action.AGENDA -> byId("calendario")
        Action.PHOTO -> byId("camara")
        Action.SCAN -> byId("qr")
        Action.SPEED -> byId("redes")
        Action.COUNT -> byId("notas")
        else -> null
    }

    fun byId(id: String) = apps.first { it.id == id }
    fun byPackage(pkg: String) = apps.firstOrNull { pkg in it.packages }

    /** The app named in a phrase: the longest alias that appears as whole words («stone video» before «stone»). */
    fun find(text: String): FamilyApp? = apps.flatMap { a -> a.aliases.map { it to a } }.sortedByDescending { it.first.length }
        .firstOrNull { (alias, _) -> Regex("(^| )${Regex.escape(alias)}( |$)").containsMatchIn(text) }?.second
}
