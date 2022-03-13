package io.legado.app.ui.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType
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
        mutableStateOf("")
    }
    urlOption.setMethod(method.value)
    val charset = remember {
        mutableStateOf("")
    }
    urlOption.setCharset(charset.value)
    val headers = remember {
        mutableStateOf("")
    }
    urlOption.setHeaders(headers.value)
    val body = remember {
        mutableStateOf("")
    }
    urlOption.setBody(body.value)
    val retry = remember {
        mutableStateOf("")
    }
    urlOption.setRetry(retry.value)
    Column {
        Row {
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
                Text(text = "method")
            }
        )
        TextField(
            value = method.value,
            onValueChange = {
                charset.value = it
            },
            label = {
                Text(text = "charset")
            }
        )
        TextField(
            value = headers.value,
            onValueChange = {
                charset.value = it
            },
            label = {
                Text(text = "headers")
            }
        )
        TextField(
            value = retry.value,
            onValueChange = {
                retry.value = it
            },
            label = {
                Text(text = "retry")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}