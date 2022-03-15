package io.legado.app.ui.widget.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
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
import io.legado.app.R
import io.legado.app.databinding.DialogUrlOptionEditBinding
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.checkbox.LabelledCheckBox
import io.legado.app.utils.GSON
import io.legado.app.utils.setLayout
import splitties.init.appCtx

class UrlOptionDialog(context: Context, private val success: (String) -> Unit) : Dialog(context) {

    val binding = DialogUrlOptionEditBinding.inflate(layoutInflater)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.tvOk.setOnClickListener {
            success.invoke(GSON.toJson(getUrlOption()))
            dismiss()
        }
    }

    private fun getUrlOption(): AnalyzeUrl.UrlOption {
        val urlOption = AnalyzeUrl.UrlOption()
        urlOption.useWebView(binding.cbUseWebView.isChecked)
        urlOption.setMethod(binding.editMethod.text.toString())
        urlOption.setCharset(binding.editCharset.text.toString())
        urlOption.setHeaders(binding.editHeaders.text.toString())
        urlOption.setBody(binding.editBody.text.toString())
        urlOption.setRetry(binding.editRetry.text.toString())
        urlOption.setType(binding.editType.text.toString())
        urlOption.setWebJs(binding.editWebJs.text.toString())
        urlOption.setJs(binding.editJs.text.toString())
        return urlOption
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
                    Surface {
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
