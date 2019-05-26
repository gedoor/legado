package io.legado.app.utils

import com.jayway.jsonpath.ReadContext

fun ReadContext.readString(path: String): String? = this.read(path, String::class.java)

fun ReadContext.readBool(path: String): Boolean? = this.read(path, Boolean::class.java)

fun ReadContext.readInt(path: String): Int? = this.read(path, Int::class.java)

fun ReadContext.readLong(path: String): Long? = this.read(path, Long::class.java)

