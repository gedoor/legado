package io.legado.app.ui.book.manage

import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import io.legado.app.R
import splitties.init.appCtx

@Composable
fun BatchChangeSourceDialog(
    state: MutableState<Boolean>,
    size: MutableState<Int>,
    position: MutableState<Int>,
    cancel: () -> Unit
) {
    if (state.value) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    cancel.invoke()
                    state.value = false
                }, content = {
                    Text(text = appCtx.getString(R.string.cancel))
                })
            },
            title = {
                Text(text = appCtx.getString(R.string.change_source_batch))
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${position.value}/${size.value}")
                    LinearProgressIndicator(
                        progress = position.value / size.value.toFloat()
                    )
                }
            }
        )
    }
}