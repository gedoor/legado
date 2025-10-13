package io.legado.app.help.gsyVideo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import io.legado.app.R

class LoadingDialog(private val context: Context) : Dialog(context, R.style.dialog_style) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    fun init() {
        val inflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.layout_loading_dialog, null)
        setContentView(view)

        setCanceledOnTouchOutside(false)
        setCancelable(false)
    }
}