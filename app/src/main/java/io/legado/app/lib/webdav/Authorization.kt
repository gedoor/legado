package io.legado.app.lib.webdav

import okhttp3.Credentials
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import io.legado.app.data.entities.Server
import io.legado.app.data.entities.Server.TYPE
import io.legado.app.data.appDb
import io.legado.app.exception.NoStackTraceException

data class Authorization(
    val username: String,
    val password: String,
    val charset: Charset = StandardCharsets.ISO_8859_1
) {

    var name = "Authorization"
        private set

    var data: String = Credentials.basic(username, password, charset)
        private set

    override fun toString(): String {
        return "$username:$password"
    }

    constructor(serverID: Long?): this("","") {
        serverID ?: throw NoStackTraceException("Unexpected server ID")
        appDb.serverDao.get(serverID!!)?.run {
            when (type) {
                TYPE.WEBDAV -> data = Credentials.basic(config.username, config.password, charset)
                TYPE.ALIYUN -> {
                    TODO("not implemented")
                }
                TYPE.GOOGLEYUN -> {
                   TODO("not implemented")
                }
            }
        }
    }

}