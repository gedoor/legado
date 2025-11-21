package io.legado.app.help.glide.progress

typealias OnProgressListener = (isComplete: Boolean, percentage: Int, bytesRead: Long, totalBytes: Long) -> Unit
