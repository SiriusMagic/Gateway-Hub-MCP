package com.zirakusa.gatewayhub.assistant

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.ui.HubColors
import kotlin.math.max
import kotlin.math.sin

/**
 * The brain as a tree: input → app → what the app does. Idle nodes breathe softly and the most used ones shine more; each
 * order sends a light along its path (Voz → Notas → Conteo). Whether it worked is marked apart (Hecho / Falló), not
 * joined as a node. It grows downwards: one row per action.
 */
@Composable fun BrainCanvas(state: AssistantState, modifier: Modifier = Modifier) {
    val res = LocalResources.current
    val nodes = remember { Graph.nodes { Talk.label(it, res) } }
    val byId = remember(nodes) { nodes.associateBy { it.id } }
    val edges = remember { Graph.edges() }
    val joined = remember(edges) { edges.map { it.from to it.to }.toSet() }
    val text = rememberTextMeasurer()
    val path = state.last?.path.orEmpty().filter { it in byId }
    val lastBranch = (0 until path.size - 1).lastOrNull { (path[it] to path[it + 1]) in joined }
    val run = remember { Animatable(0f) }
    LaunchedEffect(state.pulse) { run.snapTo(0f); if (path.isNotEmpty()) run.animateTo(path.size.toFloat(), tween(320 * path.size, easing = LinearEasing)) }
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val heat = state.heat
    val hottest = max(1, heat.values.maxOrNull() ?: 1)
    val ok = state.last?.ok == true
    val description = state.last?.let { t -> path.joinToString(" → ") { byId.getValue(it).label } + if (t.ok) " ✓" else " ✗" }.orEmpty()

    Canvas(modifier.semantics { contentDescription = description }) {
        val w = size.width; val h = size.height
        fun at(n: Node) = Offset(n.x * w, n.y * h)
        drawRect(Brush.radialGradient(listOf(Color(0xFF1E1A2B), HubColors.Background), center = Offset(w * 0.5f, h * 0.45f), radius = max(w, h) * 0.8f))
        edges.forEach { e ->
            val a = byId[e.from] ?: return@forEach; val b = byId[e.to] ?: return@forEach
            // Branches (app → action) are clearer than the many input lines.
            drawLine(HubColors.Line.copy(alpha = if (a.layer == 0) 0.10f else 0.35f), at(a), at(b), 1.dp.toPx())
        }
        // The light running along the path of the last order, green or red on its last branch.
        for (i in 0 until path.size - 1) {
            if ((path[i] to path[i + 1]) !in joined) continue
            val f = (run.value - i).coerceIn(0f, 1f)
            if (f <= 0f) break
            val a = at(byId.getValue(path[i])); val b = at(byId.getValue(path[i + 1]))
            val head = a + (b - a) * f
            val color = if (i == lastBranch) (if (ok) HubColors.Good else HubColors.Bad) else HubColors.Gold
            drawLine(color.copy(alpha = 0.25f), a, head, 9.dp.toPx(), StrokeCap.Round)
            drawLine(color, a, head, 2.5.dp.toPx(), StrokeCap.Round)
            if (f < 1f) drawCircle(Brush.radialGradient(listOf(Color.White, color.copy(alpha = 0f)), head, 14.dp.toPx()), 14.dp.toPx(), head)
        }
        nodes.forEach { n ->
            val c = at(n)
            val reached = path.indexOf(n.id).let { it >= 0 && run.value >= it }
            val glow = (heat[n.id] ?: 0).toFloat() / hottest
            val twinkle = 0.5f + 0.5f * sin(breath + n.y * 9f + n.layer)
            val end = lastBranch?.let { path[it + 1] } ?: path.firstOrNull()
            val base = if (reached && n.id == end) (if (ok) HubColors.Good else HubColors.Bad) else HubColors.Gold
            val r = (if (reached) 7f else if (n.layer == 1) 5f else 3.5f + 2f * glow).dp.toPx()
            drawCircle(base.copy(alpha = if (reached) 0.35f else 0.06f + 0.12f * twinkle + 0.15f * glow), r * 2.6f, c)
            drawCircle(if (reached) base else base.copy(alpha = 0.35f + 0.35f * glow + 0.15f * twinkle), r, c)
            val label = text.measure(n.label, TextStyle(color = if (reached) HubColors.Text else HubColors.Muted, fontSize = if (n.layer == 1) 12.sp else 11.sp))
            drawText(label, topLeft = Offset(c.x + r + 4.dp.toPx(), c.y - label.size.height / 2f))
        }
    }
}
