package io.legado.app.help.storage

import cn.hutool.crypto.symmetric.AES
import io.legado.app.help.config.LocalConfig
import io.legado.app.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)