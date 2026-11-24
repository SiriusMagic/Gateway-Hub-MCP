package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.assistant.local.LocalTurns
import com.zirakusa.gatewayhub.assistant.local.StoneRequests
import org.json.JSONArray
import kotlin.math.sqrt

/**
 * The assistant's name and how it is woken: «oye lucía» or «hola lucía». The recognizer listens with a tiny grammar of
 * the two phrases plus look-alike words (decoys), so «luisa», «lucas» or «la luz» land on those and never wake it.
 * Measured with the Spanish model: 4/4 detections and 0 false wakes over 19 tricky phrases for each name. Pure.
 */
enum class WakeName(val word: String, val shown: String, val decoys: List<String>) {
    LUCIA("lucía", "Lucía", listOf("luisa", "lucas", "lucha", "luz", "lucho", "luciana", "lucidez", "lucio", "día")),
    LUNA("luna", "Luna", listOf("lunes", "una", "luz", "uña", "lana", "duna", "cuna", "luisa")),
    NOVA("nova", "Nova", listOf("nueva", "nuevo", "nueve", "noche", "nona", "boba", "nada", "novia")),
    KIRA("kira", "Kira", listOf("quiero", "mira", "tira", "quita", "kilo", "ira", "quiere", "clara"));

    val phrases get() = listOf("oye $word", "hola $word")
    /** The grammar for the recognizer (JSON list). */
    fun grammar(): String = JSONArray(phrases + listOf("oye", "hola", "hoy", word) + decoys + "[unk]").toString()
    /** Woken only by the whole phrase, never by the name alone. */
    fun heard(text: String) = phrases.any { Regex("(^| )$it( |$)").containsMatchIn(text.lowercase()) }

    companion object { fun from(key: String?) = entries.firstOrNull { it.name == key } ?: LUCIA }
}

/**
 * Lets sound reach the recognizer only when it is clearly above the room's background noise, which it keeps learning.
 * In silence nothing is decoded: that is what keeps «Oye Lucía» light on the battery. A short tail after the voice
 * stops keeps the last syllables. Pure; 16‑bit samples.
 */
class EnergyGate(private val ratio: Double = 3.0, private val floorMin: Double = 120.0, private val tailChunks: Int = 6) {
    var floor = floorMin; private set
    private var tail = 0

    fun rms(samples: ShortArray, count: Int = samples.size): Double {
        if (count == 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) { val s = samples[i].toDouble(); sum += s * s }
        return sqrt(sum / count)
    }

    /** True when this chunk should be decoded. */
    fun open(level: Double): Boolean {
        val loud = level > floor * ratio
        // The room's noise: learnt quickly in silence and slowly under sound, so a steady fan or TV ends up as «silence»
        // while a voice, which comes and goes, still stands out.
        val weight = if (loud) 0.005 else 0.05
        floor = (floor * (1 - weight) + level * weight).coerceAtLeast(floorMin)
        if (loud) { tail = tailChunks; return true }
        if (tail > 0) { tail--; return true }
        return false
    }
}

/**
 * The words the assistant understands, for the recognizer of orders: with a short list the small model gets them right
 * far more often than free speech (measured: «pon mis recomendaciones del día» went from nonsense to right). Contacts'
 * names are added so «llama a Ana» works. Accents as the model writes them. Pure.
 */
object Vocabulary {
    val base = """
        reproduce reproducir pon ponme toca música canciones canción algo de la el los las un una mis mi del día recomendaciones
        recomendado recomendada mix mezcla descubrimiento pausa para sigue continúa reanuda siguiente anterior salta alarma alarmas
        despierta levántame a las y media cuarto menos en punto mañana tarde noche madrugada temporizador minutos minuto segundos
        hora horas avísame recuérdame cuenta regresiva llama llámale llamar marca márcale al mensaje manda mándale envía envíale
        escribe escríbele que diciendo sms qué tiempo hace clima va llover lloverá temperatura frío calor tengo hoy agenda eventos
        citas buenos buenas días tardes noches resumen cómo es dime fecha abre abrir ábreme inicia muéstrame cámara foto selfie
        toma haz hazme saca escanea lee qr código códigos prueba velocidad internet rápido notas tareas archivos descargas galería
        fotos imágenes luma stone video videos reloj cronómetro calendario calculadora contactos teléfono llamadas mensajes redes
        wifi brújula nivel inicio música cero uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince
        dieciséis diecisiete dieciocho diecinueve veinte veinticinco treinta cuarenta cincuenta sesenta noventa por favor oye hola
        contar conteo modo vamos nuevo empieza quiero ayúdame casa volumen por ciento porciento sube súbele baja bájale subir bajar
        máximo mínimo mitad tope todo alto bajo fuerte suave más menos con hasta aumenta disminuye poco cambia
        contesta atiende cuelga rechaza corta llamada anota apunta nota linterna luz enciende apaga prende reinicia repite
        empezar principio otra vez quita dale play cuál cuáles cuántas cuántos cuándo suena próxima próximo próximos tarea
        agrega añade crea hecha completada marca termina busca número celular quién quiénes cumple cumpleaños años llamó
        perdidas sin leer nuevos cuánto son calcula raíz elevado dividido entre multiplicado espacio libre queda almacenamiento
        recientes últimos conexión estado conectado hacia dónde mirando norte dirección nivelado derecho inclinación feriados
        festivos evento cita reunión pronóstico semana lunes martes miércoles jueves viernes sábado domingo pasado este esta
        aleatorio mezcla orden repetir bucle siempre lista listas playlist sonando suena canta escuchando gusta favoritos favorita
        graves agudos ecualizador normal sonido detén conversaciones escribió archivo documento imágenes playa marca agua
        cuadrícula activa desactiva mes cuándo próxima viene
        estás tal triste feliz siento sola solo sí no confirmo confirma cancela cancelar consulta web google investiga genera
        imagen dibuja entonces multiplicación
    """.trim().split(Regex("\\s+")).toSet()

    /** The grammar (JSON list): the base words, the app names and the people's names (each word of them). */
    fun grammar(people: List<String> = emptyList()): String {
        val names = people.flatMap { it.lowercase().split(Regex("[^\\p{L}]+")) }.filter { it.length >= 2 }
        val apps = Catalog.apps.flatMap { a -> a.aliases.flatMap { it.split(' ') } + a.name.lowercase() }
        return JSONArray((base + apps + names).sorted() + "[unk]").toString()
    }
}

/**
 * Two recognizers hear each order: one limited to [Vocabulary] (reliable for orders) and one free (it keeps words outside
 * the list, like what to write in a message). This picks what the brain gets. Pure.
 */
object Hearing {
    fun pick(limited: String, free: String, conversational: Boolean = false): String {
        val l = limited.replace("[unk]", "").replace(Regex("\\s+"), " ").trim()
        val f = free.trim()
        if (conversational && (LocalTurns.chatQuery(f) != null || LocalTurns.hubQuery(f) != null ||
            StoneRequests.matches(f, Parser.parse(f)))) return f
        // Several orders in one breath («llama a Luis y dime la hora»): the free recognizer keeps all of them.
        if (Intents.split(f) { Parser.parse(it).action != Action.UNKNOWN }.size > 1) return f
        val byList = Parser.parse(l)
        val byFree = Parser.parse(f)
        if (byList.action == Action.UNKNOWN && byFree.action != Action.UNKNOWN) return f
        if (byFree.action == Action.WEB_SEARCH && byList.action == Action.WEB_SEARCH && !byFree["query"].isNullOrBlank()) return f
        if (byFree["calculation"] != null && byList["calculation"] == null &&
            (byList.action == Action.VOLUME || byList.action == byFree.action)) return f
        // A family app's tool or a note with free words (a task, a city, a name): the free recognizer has them, when it heard
        // the same order.
        if (byList.action in setOf(Action.TOOL, Action.NOTE)) {
            if (byFree.action == byList.action && byFree["tool"] == byList["tool"]) return f
        }
        // A call or a message to someone the list doesn't know («llama a [unk]»): the free recognizer's name, closer to what
        // was said; the contact is then matched by how it sounds.
        if (byList.action in setOf(Action.CALL, Action.MESSAGE) && "[unk]" in limited) {
            if (byFree.action == byList.action && !byFree["who"].isNullOrBlank()) return f
        }
        // A message: the order from the list, the words to send from the free one.
        if (byList.action == Action.MESSAGE) {
            val said = Regex("(?:^| )(?:que|diciendo|diciendole|diciéndole) (.+)$").find(f)?.groupValues?.get(1)
            if (said != null && byList["text"].isNullOrBlank()) return "$l que $said"
        }
        if (conversational && byList.action == Action.UNKNOWN && f.isNotBlank()) return f
        return l.ifEmpty { f }
    }
}
