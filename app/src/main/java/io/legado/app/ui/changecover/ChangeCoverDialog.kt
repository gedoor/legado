package io.legado.app.ui.changecover

import android.util.DisplayMetrics
import androidx.fragment.app.DialogFragment


class ChangeCoverDialog : DialogFragment() {


    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }


}