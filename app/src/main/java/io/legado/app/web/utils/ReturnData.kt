package io.legado.app.web.utils


class ReturnData {

    var isSuccess: Boolean = false
        private set

    var errorMsg: String? = null
        private set

    var data: Any? = null
        private set

    init {
        this.isSuccess = false
        this.errorMsg = "未知错误,请联系开发者!"
    }

    fun setErrorMsg(errorMsg: String?): ReturnData {
        this.isSuccess = false
        this.errorMsg = errorMsg
        return this
    }

    fun setData(data: Any): ReturnData {
        this.isSuccess = true
        this.errorMsg = ""
        this.data = data
        return this
    }
}
