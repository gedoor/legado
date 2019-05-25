package io.legado.app.lib.webdav.http

object HttpAuth {

    var auth: Auth? = null
        private set

    fun setAuth(user: String, password: String) {
        auth = Auth(user, password)
    }

    class Auth internal constructor(val user: String, val pass: String)

}