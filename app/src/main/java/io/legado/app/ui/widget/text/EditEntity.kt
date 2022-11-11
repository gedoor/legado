package io.legado.app.ui.widget.text

import splitties.init.appCtx

data class EditEntity(var key: String, var value: String?, var hint: String) {

    constructor(key: String, value: String?, hint: Int) : this(key, value, appCtx.getString(hint))

}