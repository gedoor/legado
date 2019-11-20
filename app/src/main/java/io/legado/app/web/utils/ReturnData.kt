package io.legado.app.web.utils


class ReturnData {

    var isSuccess: Boolean = false

    var errorCode: Int = 0

    private var errorMsg: String? = null

    private var data: Any? = null

    init {
        this.isSuccess = false
        this.errorMsg = "未知错误,请联系开发者!"
    }

    fun getErrorMsg(): String? {
        return errorMsg
    }

    fun setErrorMsg(errorMsg: String?): ReturnData {
        this.isSuccess = false
        this.errorMsg = errorMsg
        return this
    }

    fun getData(): Any? {
        return data
    }

    fun setData(data: Any): ReturnData {
        this.isSuccess = true
        this.errorMsg = ""
        this.data = data
        return this
    }
}
