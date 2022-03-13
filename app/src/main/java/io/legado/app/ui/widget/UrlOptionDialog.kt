package io.legado.app.ui.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.ui.theme.AppTheme


@Composable
fun UrlOptionDialog(openState: MutableState<Boolean>, confirm: (String) -> Unit) {
    AppTheme {
        if (openState.value) {
            val urlOption = AnalyzeUrl.UrlOption()
            AlertDialog(
                onDismissRequest = {
                    openState.value = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirm.invoke("")
                    }) {
                        Text(text = "OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openState.value = false
                    }) { Text(text = "Cancel") }
                },
                title = {
                    Text(text = "url参数")
                },
                text = {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.padding(3.dp)) {
                            Checkbox(
                                checked = urlOption.useWebView(),
                                onCheckedChange = {
                                    urlOption.webView = it
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}