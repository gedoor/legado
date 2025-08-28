package io.legado.app.service

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

private lateinit var wss: WebSocket

class EdgeSpeakFetch {
    // 常量定义
    companion object {
        private const val TAG = "EdgeSpeakFetch"
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val BASE_URL = "speech.platform.bing.com"
        private const val WSS_PATH = "/consumer/speech/synthesize/readaloud/edge/v1"
        private const val SEC_MS_GEC_VERSION = "1-130.0.2849.68"
        private const val DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"

        // DRM 相关参数
        private const val WIN_EPOCH_SECONDS = 11644473600L
        private const val S_TO_NS = 1e9

        fun generateSecMsGec(clockSkewSeconds: Double): String {
            val now = Instant.now().epochSecond + clockSkewSeconds
            var ticks = now + WIN_EPOCH_SECONDS
            // 向下取整到最近的5分钟（300秒）
            ticks -= ticks.toLong() % 300
            // 转换为100纳秒单位
            ticks *= (S_TO_NS / 100)
            // 拼接待哈希字符串
            val strToHash = String.format(Locale.US, "%.0f%s", ticks, TRUSTED_CLIENT_TOKEN)
            // 计算SHA-256哈希
            return sha256(strToHash)
        }

        private fun sha256(input: String): String {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(input.toByteArray())
                return bytesToHex(hash).uppercase(Locale.getDefault())
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("SHA-256 algorithm not available", e)
            }
        }

        private fun bytesToHex(bytes: ByteArray): String {
            // 旧版本 Android，手动实现字节转十六进制字符串
            val hexArray = "0123456789ABCDEF".toCharArray()
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }
    }

    private var lastTime: Long = 0
    private var isReconnect = false
    private lateinit var lastWss: WebSocket
    private var audioOutputStream = PipedOutputStream()
    private var audioInputStream = PipedInputStream(audioOutputStream, 8192)
    private var client = OkHttpClient.Builder()
        .connectTimeout(50, TimeUnit.SECONDS)
        .readTimeout(80, TimeUnit.SECONDS)
        .writeTimeout(50, TimeUnit.SECONDS)
        .build()


    private fun getWssConnect(ssml: String) {
        if (::wss.isInitialized) {
            try {
                wss.close(1000, "正常关闭")
                wss.cancel()
                Log.i(TAG, "关闭 WebsocketConnect")
            } catch (e: Exception) {
                Log.i(TAG, "关闭 WebsocketConnect Exception: $e")
                e.printStackTrace()
            }
        }
        Log.i(TAG, "重新生成 WebsocketConnect")
        val clockSkewSeconds = 0.0
        val secMsGec = generateSecMsGec(clockSkewSeconds)
        val connectionId = connectID()
        val queryParams = String.format(
            "ConnectionId=%s&Sec-MS-GEC=%s&Sec-MS-GEC-Version=%s&TrustedClientToken=%s",
            connectionId,
            secMsGec,
            SEC_MS_GEC_VERSION,
            TRUSTED_CLIENT_TOKEN
        )

        val wsUrl = String.format("wss://%s%s?%s", BASE_URL, WSS_PATH, queryParams)
        val request = Request.Builder().url(wsUrl).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket onOpen")
                wss = webSocket
                isReconnect = false
                sendSpeechConfig(wss)
                sendSSMLMessage(wss, ssml)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // 检测turn.end并关闭流
                if (text.contains("turn.end")) {
                    Log.i(TAG, "收到turn.end 关闭流")
                    audioOutputStream.close()
                    lastTime = System.currentTimeMillis()
                    lastWss = webSocket

                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    // 处理二进制音频数据（与Golang解析逻辑一致）
                    val message = bytes.toByteArray()
                    if (message.size < 2) {
                        Log.i(TAG, "WebSocket onMessage binary message too short")
                        return
                    }

                    // 解析头部长度（前2字节big endian，与Golang一致）
                    val headerLength =
                        ((message[0].toInt() and 0xFF) shl 8) or (message[1].toInt() and 0xFF)
                    if (headerLength > message.size) {
                        Log.i(TAG, "WebSocket onMessage invalid header length")
                        return
                    }

                    // 提取音频数据（跳过头部，与Golang一致）
                    val audioData = ByteArray(message.size - headerLength - 2)
                    System.arraycopy(
                        message,
                        headerLength + 2,
                        audioData,
                        0,
                        audioData.size
                    )
                    // 音频数据
                    if (audioData.isNotEmpty()) {
                        try {
                            audioOutputStream.write(audioData)
                            audioOutputStream.flush()
                        } catch (e: IOException) {
                            Log.e(TAG, "缓存音频失败", e)
                        }
                    }

                } catch (e: Exception) {
                    Log.i(TAG, "WebSocket onMessage Catch" + e.printStackTrace())
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket onClosed$code$reason")
                isReconnect = true
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                isReconnect = true
                Log.i(TAG, "WebSocket onFailure: $t $response")

            }

        }
        client.newWebSocket(request, listener)
    }

    // 移除文本中的特殊字符和表情，保留常用文章标点符号
    private fun removeSpecialCharacters(text: String): String {
        val pattern = Pattern.compile("[^\\w\\s\u4e00-\u9fff，。！？；：、（）《》【】“”‘’]")
        val matcher = pattern.matcher(text)
        return matcher.replaceAll("")
    }

    private fun initStream() {
        try {
            audioOutputStream = PipedOutputStream()
            audioInputStream = PipedInputStream(audioOutputStream, 8192) // 缓冲区8KB
        } catch (e: Exception) {
            Log.i(TAG, "初始化管道流失败")
        }
    }


    fun synthesizeText(
        speakText: String,
        rate: Int,
        voice: String = DEFAULT_VOICE
    ): InputStream {
        initStream()
        Log.i(TAG, "speakText: $speakText")
        try {
            val speakTextStr = removeSpecialCharacters(speakText)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastTime
            // 判断是否超过毫秒
            val ssml = mkSSML(speakTextStr, voice, processRate(rate))

            if (timeDiff < 500 && !isReconnect) {
                Log.i(TAG, "复用使用上次lastWss")
                wss = lastWss
                sendSSMLMessage(wss, ssml)
            } else {
                getWssConnect(ssml)
                Log.i(TAG, "重新生成websocket, sendSpeechConfig")
            }
        } catch (e: Exception) {
            Log.i(TAG, "sendSSMLMessage:$e")
        }
        return audioInputStream
    }

    // 构造SSML文本
    private fun mkSSML(
        text: String,
        voice: String,
        rate: String,
        pitch: String = "+0Hz",
        volume: String = "+0%"
    ): String {
        return String.format(
            "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                    "<voice name='%s'>" +
                    "<prosody pitch='%s' rate='%s' volume='%s'>%s</prosody>" +
                    "</voice>" +
                    "</speak>",
            voice, pitch, rate, volume, text
        )
    }

    // 发送speech.config消息
    private fun sendSpeechConfig(wss: WebSocket) {
        Log.i(TAG, "准备写入sendSpeechConfig")
        val speechConfig =
            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"true\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"

        // 时间格式严格匹配Golang的time.RFC1123
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date())

        val speechConfigMsg = String.format(
            "X-Timestamp:%s\r\nContent-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n%s\r\n",
            timestamp, speechConfig
        )
        wss.send(speechConfigMsg)
    }

    // 发送SSML消息
    private fun sendSSMLMessage(wss: WebSocket, ssml: String) {
        Log.i(TAG, "准备写入SSML")
        val requestId = connectID()
        val sdf = SimpleDateFormat("EEE MMM d yyyy HH:mm:ss zzz", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date())

        val ssmlMsg = String.format(
            "X-RequestId:%s\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:%sZ\r\nPath:ssml\r\n\r\n%s",
            requestId, timestamp, ssml
        )
        Log.i(TAG, "WebSocket sendSSMLMessage")
        wss.send(ssmlMsg)
    }

    // 生成无破折号的UUID
    private fun connectID(): String {
        return UUID.randomUUID().toString().replace("-".toRegex(), "")
    }

    // 生成带符号的百分比字符串
    private fun processRate(rate: Int): String {
        val rateOffset = rate - 12
        val customRate = if (rateOffset > 0) {
            "+$rateOffset%"
        } else {
            "$rateOffset%"
        }
        return customRate
    }

    fun release() {
        if (::lastWss.isInitialized) lastWss.cancel()
        try {
            audioOutputStream.close()
            Log.i(TAG, "管道流已关闭")
        } catch (e: IOException) {
            Log.e(TAG, "关闭管道流失败", e)
        }
    }
}