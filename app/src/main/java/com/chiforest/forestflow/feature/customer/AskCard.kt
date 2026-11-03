package com.chiforest.forestflow.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.ai.DrinkAssistant
import com.chiforest.forestflow.ai.DrinkOption
import com.chiforest.forestflow.ai.Recommendation
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.ProductArt
import com.chiforest.forestflow.ui.components.softCard
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type
import kotlinx.coroutines.launch

/** Provided once at the app root so screens do not need the assistant passed through every function. */
val LocalDrinkAssistant = staticCompositionLocalOf<DrinkAssistant?> { null }

private val EXAMPLES = listOf("Something sweet", "No sugar", "Sour and refreshing", "Surprise me")

/** "Ask for a drink": the student describes what they feel like and gets one in-stock pick with a short reason. */
@Composable
fun AskCard(options: List<DrinkOption>, watching: Boolean, onNotify: () -> Unit) {
    val c = FF.c
    val assistant = LocalDrinkAssistant.current ?: return
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Recommendation?>(null) }

    fun ask(text: String) {
        if (loading || text.isBlank()) return
        focus.clearFocus()
        loading = true
        scope.launch {
            result = assistant.recommend(text, options)
            loading = false
        }
    }

    Column(Modifier.fillMaxWidth().softCard(RoundedCornerShape(22.dp)).padding(14.dp).testTag("askCard")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.Sparkles, null, tint = c.red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ask for a drink", style = Type.title(15.sp, 800), color = c.ink, modifier = Modifier.weight(1f))
            Pill(if (assistant.usesModel) "AI" else "Quick match", if (assistant.usesModel) c.blue else c.muted, if (assistant.usesModel) c.blueTint else c.surface2)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(c.surface2).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it.take(120) },
                singleLine = true,
                textStyle = Type.body(15.sp, 500).copy(color = c.ink),
                cursorBrush = SolidColor(c.red),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { ask(query) }),
                modifier = Modifier.weight(1f).semantics { contentDescription = "What do you feel like drinking" }.testTag("askField"),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("What do you feel like?", style = Type.body(15.sp), color = c.faint, maxLines = 1)
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EXAMPLES.forEach { e ->
                Text(
                    e, style = Type.body(12.sp, 600), color = c.muted,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(c.surface2)
                        .clickable { query = e; ask(e) }.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ForestButton(
            if (loading) "Thinking…" else "Ask", { ask(query) }, Modifier.fillMaxWidth(),
            small = true, style = BtnStyle.Dark, icon = Ic.Sparkles, enabled = !loading && query.isNotBlank(),
        )

        result?.let { r ->
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.redTint).padding(12.dp).testTag("askResult"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val o = r.option
                if (o != null) {
                    ProductArt(o.key, Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (o != null) {
                        Text(o.name, style = Type.title(15.sp, 800), color = c.ink)
                    }
                    Text(r.reason, style = Type.body(12.sp), color = c.muted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (o != null) Pill("${o.stock} left", c.green, c.greenTint, dot = true)
                        Pill(
                            if (r.source == Recommendation.Source.Ai) "AI pick" else "Quick match",
                            if (r.source == Recommendation.Source.Ai) c.blue else c.muted,
                            if (r.source == Recommendation.Source.Ai) c.blueTint else c.surface,
                        )
                    }
                    if (o == null && !watching) {
                        Spacer(Modifier.height(8.dp))
                        ForestButton("Notify Me", onNotify, small = true, style = BtnStyle.Tonal, icon = Ic.Bell)
                    }
                }
            }
        }
    }
}
