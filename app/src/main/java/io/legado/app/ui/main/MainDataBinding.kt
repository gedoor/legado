package io.legado.app.ui.main

import android.view.View
import androidx.databinding.ViewDataBinding

class MainDataBinding(bindingComponent: Any?, root: View?, localFieldCount: Int) :
    ViewDataBinding(bindingComponent, root, localFieldCount) {
    /**
     * Set a value value in the Binding class.
     *
     *
     * Typically, the developer will be able to call the subclass's set method directly. For
     * example, if there is a variable `x` in the Binding, a `setX` method
     * will be generated. However, there are times when the specific subclass of ViewDataBinding
     * is unknown, so the generated method cannot be discovered without reflection. The
     * setVariable call allows the values of variables to be set without reflection.
     *
     * @param variableId the BR id of the variable to be set. For example, if the variable is
     * `x`, then variableId will be `BR.x`.
     * @param value The new value of the variable to be set.
     * @return `true` if the variable is declared or used in the binding or
     * `false` otherwise.
     */
    override fun setVariable(variableId: Int, value: Any?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * @hide
     */
    override fun executeBindings() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Called when an observed object changes. Sets the appropriate dirty flag if applicable.
     * @param localFieldId The index into mLocalFieldObservers that this Object resides in.
     * @param object The object that has changed.
     * @param fieldId The BR ID of the field being changed or _all if
     * no specific field is being notified.
     * @return true if this change should cause a change to the UI.
     * @hide
     */
    override fun onFieldChange(localFieldId: Int, `object`: Any?, fieldId: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Invalidates all binding expressions and requests a new rebind to refresh UI.
     */
    override fun invalidateAll() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Returns whether the UI needs to be refresh to represent the current data.
     *
     * @return true if any field has changed and the binding should be evaluated.
     */
    override fun hasPendingBindings(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}