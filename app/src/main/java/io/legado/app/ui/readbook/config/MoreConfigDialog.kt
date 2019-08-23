package io.legado.app.ui.readbook.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import io.legado.app.R

class MoreConfigDialog : DialogFragment() {

    private val readPreferTag = "readPreferenceFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LinearLayout(context)
        view.id = R.id.tag1
        container?.addView(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var preferenceFragment = childFragmentManager.findFragmentByTag(readPreferTag)
        if (preferenceFragment == null) preferenceFragment = ReadPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(view.id, preferenceFragment, readPreferTag)
            .commit()
    }
}