package io.legado.app.ui.book.audio

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.legado.app.lib.theme.accentColor
import io.legado.app.model.AudioPlay
import io.legado.app.service.AudioPlayService
import splitties.init.appCtx


@Composable
fun TimerDialog(state: MutableState<Boolean>, parent: View) {
    val intOffset = IntArray(2)
    parent.getLocationInWindow(intOffset)
    if (state.value) {
        val timeMinute = remember {
            mutableStateOf(AudioPlayService.timeMinute)
        }
        Dialog(onDismissRequest = { state.value = false }) {
            Card(Modifier.fillMaxWidth()) {
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = timeMinute.value.toFloat(), onValueChange = {
                        timeMinute.value = it.toInt()
                        AudioPlay.setTimer(it.toInt())
                    },
                    valueRange = 0f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(appCtx.accentColor),
                        activeTrackColor = Color(appCtx.accentColor)
                    )
                )
            }
        }
    }
}