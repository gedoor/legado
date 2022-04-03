package io.legado.app.ui.book.arrange

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.legado.app.R
import splitties.init.appCtx


@Composable
fun BatchChangeSourceDialog(state: MutableState<Boolean>) {
    val value = remember {
        mutableStateOf(0)
    }
    if (state.value) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    state.value = false
                }, content = {
                    Text(text = "取消")
                })
            },
            title = {
                Text(text = appCtx.getString(R.string.book_change_source))
            },
            text = {

            }
        )
    }
}