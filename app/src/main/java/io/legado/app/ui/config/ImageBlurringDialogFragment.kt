package io.legado.app.ui.config

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import io.legado.app.R
import io.legado.app.databinding.FragmentImageBlurringDialogBinding

class ImageBlurringDialogFragment(private val prefName: String, private val block: () -> Unit): DialogFragment() {

    companion object {
        private const val DEFAULT_VALUE = 0
        private const val TAG = "ImageBlurringDialogFragment"
    }

    private val requireContext get() = requireContext()
    private var _fragmentImageBlurringDialogBinding: FragmentImageBlurringDialogBinding? = null
    private val fragmentImageBlurringDialog get() = _fragmentImageBlurringDialogBinding!!
    private lateinit var sharedPreference: SharedPreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _fragmentImageBlurringDialogBinding = FragmentImageBlurringDialogBinding.inflate(layoutInflater)
        sharedPreference = PreferenceManager.getDefaultSharedPreferences(requireContext)
        sharedPreference.getInt(prefName, DEFAULT_VALUE).apply {
            fragmentImageBlurringDialog.seekBar.progress = this
            fragmentImageBlurringDialog.textViewValue.text = this.toString()
        }
        fragmentImageBlurringDialog.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fragmentImageBlurringDialog.textViewValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        return AlertDialog.Builder(requireContext)
            .setTitle(R.string.background_image_blurring_radius)
            .setView(fragmentImageBlurringDialog.root)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                sharedPreference.edit().putInt(prefName, fragmentImageBlurringDialog.seekBar.progress).commit()
                block()
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
            .create()
    }

    fun show(fragmentManager: FragmentManager) = show(fragmentManager, TAG)

}