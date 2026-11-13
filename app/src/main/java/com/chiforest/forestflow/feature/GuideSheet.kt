package com.chiforest.forestflow.feature

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Guide
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestSheet
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.SheetTitle
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type

/** Step-by-step "how it works", opened after every sign-in and from the ? button. */
@Composable
fun GuideSheet(s: AppState, dispatch: (Action) -> Unit) {
    val c = FF.c
    val role = s.session?.role ?: Role.Student
    val steps = Guide.steps(role)
    val i = s.guideStep.coerceIn(0, steps.lastIndex)
    val step = steps[i]
    val last = i == steps.lastIndex
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle(
            if (role == Role.Student) "How Forest Flow works" else "How the console works",
            "Step ${i + 1} of ${steps.size}",
        ) { dispatch(Action.CloseSheet) }
        Pill(step.where, c.red, c.redTint, icon = Ic.MapPin)
        Spacer(Modifier.height(10.dp))
        Text(step.title, style = Type.display(22.sp, 800), color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(step.body, style = Type.body(15.sp), color = c.muted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            steps.indices.forEach { n ->
                Box(Modifier.padding(horizontal = 3.dp).size(if (n == i) 18.dp else 7.dp, 7.dp).clip(CircleShape).background(if (n == i) c.red else c.line))
            }
        }
        Spacer(Modifier.height(16.dp))
        step.tab?.let { t ->
            ForestButton("Take me there", { dispatch(Action.GuideGo(t)) }, Modifier.fillMaxWidth(), style = BtnStyle.Tonal, icon = Ic.Navigation)
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (i == 0) ForestButton("Skip", { dispatch(Action.CloseSheet) }, Modifier.weight(1f), style = BtnStyle.Outline)
            else ForestButton("Back", { dispatch(Action.GuideStep(-1)) }, Modifier.weight(1f), style = BtnStyle.Outline)
            if (last) ForestButton("Got it", { dispatch(Action.CloseSheet) }, Modifier.weight(1f), style = BtnStyle.Primary, icon = Ic.Check)
            else ForestButton("Next", { dispatch(Action.GuideStep(1)) }, Modifier.weight(1f), style = BtnStyle.Primary)
        }
        Spacer(Modifier.width(1.dp))
    }
}
