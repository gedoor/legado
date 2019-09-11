package io.legado.app.lib.theme.prefs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.preference.Preference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.applyTint

class ATEEditTextPreference(context: Context?, attrs: AttributeSet?) : EditTextPreference(
    context,
    attrs
),
    DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private var builder: AlertDialog.Builder? = null

    private var dialog: AlertDialog? = null

    private var editText: EditText? = null

    /** Which button was clicked.  */
    private var mWhichButtonClicked: Int = 0

    override fun onClick() {
        if (dialog != null && dialog!!.isShowing) return

        showDialog(null)
    }

    protected fun showDialog(state: Bundle?) {
        val context = context

        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE

        builder = AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setIcon(dialogIcon)
            .setPositiveButton(positiveButtonText, this)
            .setNegativeButton(negativeButtonText, this)

        val builder = this.builder!!

        val contentView = onCreateDialogView()
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(dialogMessage)
        }

        // Create the dialog
        dialog = builder.create()

        val dialog = this.dialog!!
        if (state != null) {
            dialog.onRestoreInstanceState(state)
        }
        requestInputMethod(dialog)
        dialog.setOnDismissListener(this)
        dialog.show()
        dialog.applyTint()
    }

    protected fun onCreateDialogView(): View? {
        if (dialogLayoutResource == 0) {
            return null
        }

        val inflater = LayoutInflater.from(context)
        return inflater.inflate(dialogLayoutResource, null)
    }

    private fun requestInputMethod(dialog: Dialog?) {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }


    protected fun onBindDialogView(view: View) {
        editText = view.findViewById(android.R.id.edit)

        checkNotNull(editText) { "Dialog view must contain an EditText with id" + " @android:id/edit" }

        view.findViewById<TextView>(android.R.id.message).visibility = View.GONE

        val editText = this.editText!!

        ATH.setTint(editText, ThemeStore.accentColor(context))

        editText.requestFocus()
        editText.setText(text)
        // Place cursor at the end
        editText.setSelection(editText.length())
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        mWhichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface?) {
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)

        text = editText?.text.toString()
        callChangeListener(text)
    }

    protected fun onDialogClosed(positiveResult: Boolean) {

    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        if (dialog == null || !dialog!!.isShowing) {
            return superState
        }

        val myState = SavedState(superState)
        myState.isDialogShowing = true
        myState.dialogBundle = dialog!!.onSaveInstanceState()
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle)
        }
    }

    private class SavedState : Preference.BaseSavedState {
        internal var isDialogShowing: Boolean = false
        internal var dialogBundle: Bundle? = null

        @SuppressLint("ParcelClassLoader")
        constructor(source: Parcel) : super(source) {
            isDialogShowing = source.readInt() == 1
            dialogBundle = source.readBundle()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(if (isDialogShowing) 1 else 0)
            dest.writeBundle(dialogBundle)
        }

        constructor(superState: Parcelable) : super(superState) {}

        companion object {

            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}