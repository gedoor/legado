package io.legado.app.lib.webdav

import okhttp3.Credentials
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class Authorization(
    val username: String,
    val password: String,
    val charset: Charset = StandardCharsets.ISO_8859_1
) {

    val data: String = Credentials.basic(username, password, charset)

    override fun toString(): String {
        return "$username:$password"
    }

}