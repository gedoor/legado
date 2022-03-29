package io.legado.app.ui.book.audio

import android.view.View
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import io.legado.app.model.AudioPlay
import io.legado.app.service.AudioPlayService


@Composable
fun TimerDialog(state: MutableState<Boolean>, parent: View) {
    val intOffset = IntArray(2)
    parent.getLocationInWindow(intOffset)
    if (state.value) {
        val timeMinute = remember {
            mutableStateOf(AudioPlayService.timeMinute)
        }
        Dialog(onDismissRequest = { state.value = false }) {
            Slider(value = timeMinute.value.toFloat(), onValueChange = {
                timeMinute.value = it.toInt()
                AudioPlay.setTimer(it.toInt())
            }, valueRange = 0f..180f)
        }
    }
}