package io.legado.app.utils

import com.jayway.jsonpath.ReadContext

fun ReadContext.readString(path: String) = this.read(path, String::class.java)

fun ReadContext.readBool(path: String) = this.read(path, Boolean::class.java)

fun ReadContext.readInt(path: String) = this.read(path, Int::class.java)