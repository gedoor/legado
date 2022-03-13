package io.legado.app.ui.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.checkbox.LabelledCheckBox
import io.legado.app.utils.GSON
import splitties.init.appCtx


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
                        openState.value = false
                        confirm.invoke(GSON.toJson(urlOption))
                    }) {
                        Text(text = appCtx.getString(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openState.value = false
                    }) { Text(text = appCtx.getString(R.string.cancel)) }
                },
                title = {
                    Text(text = "url参数")
                },
                text = {
                    UrlOptionView(urlOption = urlOption)
                }
            )
        }
    }
}

@Composable
fun UrlOptionView(urlOption: AnalyzeUrl.UrlOption) {
    val useWebView = remember {
        mutableStateOf(urlOption.useWebView())
    }
    urlOption.useWebView(useWebView.value)
    val method = remember {
        mutableStateOf(urlOption.getMethod() ?: "")
    }
    urlOption.setMethod(method.value)
    Column(Modifier.padding(6.dp)) {
        Row(Modifier.padding(3.dp)) {
            LabelledCheckBox(
                checked = useWebView.value,
                onCheckedChange = {
                    useWebView.value = it
                },
                label = "useWebView"
            )
        }
        TextField(
            value = method.value,
            onValueChange = {
                method.value = it
            },
            label = {
                Text(text = "Method")
            }
        )
    }
}