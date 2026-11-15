package cl.vialapp.app.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.google.gson.JsonElement
import com.google.gson.JsonObject

private fun JsonObject.string(name: String): String =
    get(name)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()

private fun JsonObject.children(): List<JsonElement> =
    get("children")?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty()

private fun typeOf(node: JsonElement): String = if (node.isJsonObject) node.asJsonObject.string("type") else ""

private val imagePath = Regex("^/learning/[a-z0-9-]{1,40}/[a-f0-9]{20}\\.(png|webp|jpeg|svg)$")
private val blockTypes = setOf("p", "div", "aside", "ul", "li", "figure", "figcaption", "details", "summary", "image", "speed")

/** Render the same structured lesson content as the web app; never execute HTML or scripts. */
@Composable
internal fun LearningBlocks(blocks: List<JsonElement>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val meaningful = blocks.filterNot { it.isJsonPrimitive && it.asString.isBlank() }
        val inline = mutableListOf<JsonElement>()
        for (node in meaningful) {
            if (typeOf(node) in blockTypes) {
                if (inline.isNotEmpty()) {
                    LearningParagraph(inline.toList())
                    inline.clear()
                }
                LearningBlock(node)
            } else {
                inline.add(node)
            }
        }
        if (inline.isNotEmpty()) LearningParagraph(inline.toList())
    }
}

@Composable
private fun LearningParagraph(nodes: List<JsonElement>, muted: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val text = remember(nodes) {
        buildAnnotatedString {
            fun add(n: JsonElement) {
                if (n.isJsonNull) return
                if (n.isJsonPrimitive) { append(n.asString); return }
                if (n.isJsonArray) { n.asJsonArray.forEach { add(it) }; return }
                val obj = n.asJsonObject
                if (obj.string("type") in setOf("strong", "b", "summary")) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { obj.children().forEach { add(it) } }
                } else {
                    obj.children().forEach { add(it) }
                }
            }
            nodes.forEach { add(it) }
        }
    }
    if (text.text.isNotBlank()) Text(text, color = if (muted) c.onSurfaceVariant else c.onSurface,
        fontSize = if (muted) 12.sp else 15.sp, lineHeight = if (muted) 18.sp else 23.sp)
}

@Composable
private fun LearningBlock(node: JsonElement) {
    if (!node.isJsonObject) { LearningParagraph(listOf(node)); return }
    val obj = node.asJsonObject
    val c = MaterialTheme.colorScheme
    val children = obj.children()
    val tone = obj.string("tone")
    when (obj.string("type")) {
        "image" -> LearningIllustration(obj.string("src"), obj.string("alt"))
        "speed" -> SpeedExercise(obj.string("mode") == "reaction")
        "p" -> LearningParagraph(children)
        "figcaption" -> LearningParagraph(children, muted = true)
        "li" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", color = c.primary, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) { LearningBlocks(children) }
        }
        "aside" -> Surface(color = c.primaryContainer, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("RECUERDA", color = c.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LearningBlocks(children)
            }
        }
        "details" -> {
            var expanded by remember(node) { mutableStateOf(false) }
            val heading = children.firstOrNull { typeOf(it) == "summary" }
            Surface(color = c.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (expanded) "Ocultar detalle" else "Ver detalle")
                    }
                    heading?.let { LearningParagraph(it.asJsonObject.children()) }
                    if (expanded) LearningBlocks(children.filter { typeOf(it) != "summary" })
                }
            }
        }
        else -> when {
            tone == "source" -> LearningParagraph(children, muted = true)
            tone.contains("fact-stat") || tone == "cost-item" || tone == "topic-pill" -> Surface(
                color = c.surfaceContainerHigh, shape = RoundedCornerShape(12.dp)
            ) { Column(Modifier.fillMaxWidth().padding(10.dp)) { LearningBlocks(children) } }
            obj.string("type") == "summary" -> LearningParagraph(listOf(node))
            else -> LearningBlocks(children)
        }
    }
}

@Composable
private fun LearningIllustration(path: String, alt: String) {
    if (!imagePath.matches(path)) {
        if (alt.isNotBlank()) Text(alt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        return
    }
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    var zoom by remember(path) { mutableStateOf(false) }
    var failed by remember(path) { mutableStateOf(false) }
    val model = remember(context, path) {
        ImageRequest.Builder(context).data("https://vialapp.cl" + path).build()
    }
    Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 260.dp).clickable { zoom = true }.padding(8.dp),
            contentAlignment = Alignment.Center) {
            if (failed) Text("Ilustración no disponible", color = Color(0xFF475569))
            else AsyncImage(model, contentDescription = alt, modifier = Modifier.fillMaxWidth().height(190.dp),
                contentScale = ContentScale.Fit, onError = { failed = true })
        }
    }
    if (alt.isNotBlank()) Text(alt, color = c.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
    if (zoom && !failed) AlertDialog(onDismissRequest = { zoom = false },
        title = { Text("Ilustración") },
        text = { AsyncImage(model, contentDescription = alt, modifier = Modifier.fillMaxWidth().height(330.dp), contentScale = ContentScale.Fit) },
        confirmButton = { TextButton(onClick = { zoom = false }) { Text("Cerrar") } })
}

@Composable
private fun SpeedExercise(reaction: Boolean) {
    val c = MaterialTheme.colorScheme
    var speed by remember(reaction) { mutableIntStateOf(30) }
    Surface(color = c.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (reaction) "Explora la distancia de reacción" else "Explora la velocidad de impacto",
                fontWeight = FontWeight.Bold, color = c.onPrimaryContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (reaction) listOf(30, 50, 90) else listOf(30, 45, 65)).forEach { value ->
                    FilterChip(selected = speed == value, onClick = { speed = value }, label = { Text("$value km/h", fontSize = 12.sp) })
                }
            }
            val explanation = if (reaction) "≈ ${speed / 10 * 3} m de reacción"
                else when (speed) {
                    30 -> "90% de probabilidad de sobrevivir"
                    45 -> "Menos del 50% de probabilidad de sobrevivir"
                    else -> "La muerte es el resultado más probable"
                }
            Text(explanation, color = c.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(if (reaction) "Regla aproximada del libro para un segundo de reacción, antes de frenar."
                else "Estimación del libro para un peatón atropellado, no una predicción individual.",
                color = c.onPrimaryContainer, fontSize = 12.sp)
        }
    }
}
