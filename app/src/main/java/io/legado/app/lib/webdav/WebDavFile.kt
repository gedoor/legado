package io.legado.app.lib.webdav

import java.util.*

/**
 * webDavFile
 */
class WebDavFile(
    urlStr: String,
    authorization: Authorization,
    val displayName: String,
    val urlName: String,
    val size: Long,
    val contentType: String,
    val lastModify: Date
) : WebDav(urlStr, authorization)