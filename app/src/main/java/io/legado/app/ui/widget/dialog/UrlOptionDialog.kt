package io.legado.app.ui.widget.dialog

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import io.legado.app.R
import io.legado.app.databinding.DialogUrlOptionEditBinding
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.checkbox.LabelledCheckBox
import io.legado.app.utils.GSON
import io.legado.app.utils.setLayout
import splitties.init.appCtx

class UrlOptionDialog(context: Context) : AlertDialog(context) {

    val binding = DialogUrlOptionEditBinding.inflate(layoutInflater)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.tvOk.setOnClickListener {
            dismiss()
        }
    }

}


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
                    Surface() {
                        UrlOptionView(urlOption = urlOption)
                    }
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
    val type = remember {
        mutableStateOf("")
    }
    urlOption.setType(type.value)
    val webJs = remember {
        mutableStateOf("")
    }
    urlOption.setWebJs(webJs.value)
    val js = remember {
        mutableStateOf("")
    }
    urlOption.setJs(js.value)

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
    ) {
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
            value = charset.value,
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
                headers.value = it
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
        TextField(
            value = type.value,
            onValueChange = {
                type.value = it
            },
            label = {
                Text(text = "type")
            }
        )
        TextField(
            value = webJs.value,
            onValueChange = {
                webJs.value = it
            },
            label = {
                Text(text = "webJs")
            }
        )
        TextField(
            value = js.value,
            onValueChange = {
                js.value = it
            },
            label = {
                Text(text = "js")
            }
        )
    }
}

@Preview
@Composable
fun PreviewUrlOption() {
    UrlOptionView(urlOption = AnalyzeUrl.UrlOption())
}