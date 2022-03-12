package io.legado.app.ui.widget

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable


@Composable
fun UrlOptionDialog(confirm: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                confirm.invoke("")
            }) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            TextButton(onClick = {}) { Text(text = "Cancel") }
        },
        title = {
            Text(text = "url参数")
        }
    )
}