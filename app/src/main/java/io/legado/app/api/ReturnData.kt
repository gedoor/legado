package io.legado.app.api


class ReturnData {

    var isSuccess: Boolean = false
        private set

    var errorMsg: String = "未知错误,请联系开发者!"
        private set

    var data: Any? = null
        private set

    fun setErrorMsg(errorMsg: String): ReturnData {
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
