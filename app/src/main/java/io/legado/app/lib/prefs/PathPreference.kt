package io.legado.app.lib.prefs

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

class PathPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var resultLauncher: ActivityResultLauncher<Uri?>? = null

    init {
        // Set a listener for when the preference is clicked
        setOnPreferenceClickListener {
            // Launch the directory picker
            resultLauncher?.launch(null)
            true
        }
    }

    fun registerResultLauncher(launcher: ActivityResultLauncher<Uri?>) {
        resultLauncher = launcher
    }

    fun onPathSelected(uri: Uri?) {
        uri?.let {
            val uriString = it.toString()
            if (!uriString.isNullOrEmpty()) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                appCtx.contentResolver.takePersistableUriPermission(it, takeFlags)
                persistString(uriString)
                summary = uriString
                appCtx.putPrefString(key, uriString) // Save to SharedPreferences
            } else {
                appCtx.toastOnUi(R.string.invalid_directory)
            }
        }
    }

    override fun onAttached() {
        super.onAttached()
        // Initialize summary with current value
        summary = appCtx.getPrefString(key)
    }
}