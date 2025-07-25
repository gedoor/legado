package io.legado.app.service;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class EdgeUtil {
    // 常量定义（与Golang完全一致）
    private static final String TrustedClientToken = "6A5AA1D4EAFF4E9FB37E23D68491D6F4";
    private static final String BaseURL = "speech.platform.bing.com";
    private static final String WSSPath = "/consumer/speech/synthesize/readaloud/edge/v1";
    private static final String SecMsGecVersion = "1-130.0.2849.68";
    private static final String DefaultVoice = "zh-CN-XiaoxiaoNeural";

    // DRM 相关参数（与Golang完全一致）
    private static final long WIN_EPOCH_SECONDS = 11644473600L;
    private static final double S_TO_NS = 1e9;

    // 管道流：用于实时传递音频数据
    private PipedInputStream audioInputStream;
    private PipedOutputStream audioOutputStream;

    public void AssetAudioPipedStream(Context context) {
        try {
            // 初始化管道流（输入流与输出流关联）
            audioOutputStream = new PipedOutputStream();
            audioInputStream = new PipedInputStream(audioOutputStream, 8192); // 缓冲区8KB
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 供外部获取音频输入流（类似 response.body.byteStream()）
    public InputStream getAudioStream() {
        return audioInputStream;
    }

    // 关闭流资源
    private void closeStreams() {
        try {
            System.out.println("关闭流: ");
            audioOutputStream.close(); // 关闭输出流，输入流会读取到-1（结束标志）
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWsUrl() {
        System.out.println("重新生成wsUrl");
        // 时钟偏移（与Golang一致）
        double clockSkewSeconds = 0.0;

        // 生成Sec-MS-GEC token（核心逻辑与Golang一致）
        String secMsGec = generateSecMsGec(clockSkewSeconds);
        System.out.println("Sec-MS-GEC Token: " + secMsGec);

        // 生成ConnectionId（与Golang一致：无破折号UUID）
        String connectionId = connectID();

        // 调整参数顺序为：ConnectionId -> Sec-MS-GEC -> Sec-MS-GEC-Version -> TrustedClientToken
        String queryParams = String.format(
                "ConnectionId=%s&Sec-MS-GEC=%s&Sec-MS-GEC-Version=%s&TrustedClientToken=%s",
                connectionId,
                secMsGec,
                SecMsGecVersion,
                TrustedClientToken
        );


        return String.format("wss://%s%s?%s", BaseURL, WSSPath, queryParams);
    }

    public String processRate(int rate) {
        // 尝试将输入转换为整数，并限制在0-100范围内
        int rateValue;
        try {
            rateValue = rate;
            // 确保值在0-100之间
            rateValue = Math.max(0, Math.min(100, rateValue));
        } catch (NumberFormatException e) {
            // 转换失败时使用默认值25
            rateValue = 25;
        }

        // 计算相对于25的偏移量
        int rateOffset = rateValue - 25;

        // 生成带符号的百分比字符串
        String customRate;
        if (rateOffset > 0) {
            customRate = "+" + rateOffset + "%";
        } else {
            customRate = rateOffset + "%";
        }

        return customRate;
    }

    public PipedInputStream run(String wsUrl,String speakText, int rate, String voice) throws Exception {
        System.out.println("Connecting to: " + wsUrl);
        // 配置OkHttp客户端
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(50, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(50, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // 创建WebSocket请求
        Request request = new Request.Builder().url(wsUrl).build();

        // 建立WebSocket连接
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket连接已建立");
                try {
                    // 发送speech.config消息（格式与Golang完全一致）
                    sendSpeechConfig(webSocket);

                    String ssml = mkSSML(speakText, DefaultVoice, "+0Hz", processRate(rate), "+0%");
                    sendSSMLMessage(webSocket, ssml);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
//                System.out.println("Text message: " + text);
                // 检测turn.end并关闭连接（与Golang逻辑一致）
                if (text.contains("turn.end")) {
                    System.out.println("发现 END break 结束 Close");
                    closeStreams();
                    webSocket.cancel();
                } else {
                    System.out.println("继续");
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                try {
                    // 处理二进制音频数据（与Golang解析逻辑一致）
                    byte[] message = bytes.toByteArray();
                    if (message.length < 2) {
                        System.out.println("binary message too short");
                        return;
                    }

                    // 解析头部长度（前2字节big endian，与Golang一致）
                    int headerLength = ((message[0] & 0xFF) << 8) | (message[1] & 0xFF);
                    if (headerLength > message.length) {
                        System.out.println("invalid header length");
                        return;
                    }

                    // 提取音频数据（跳过头部，与Golang一致）
                    byte[] audioData = new byte[message.length - headerLength - 2];
                    System.arraycopy(message, headerLength + 2, audioData, 0, audioData.length);

                    if (audioData.length > 0) {
                        System.out.println("写入管道输出流");
                        audioOutputStream.write(audioData);
                        audioOutputStream.flush(); // 立即刷新，让输入流能读取到
                    } else {
                        System.out.println("empty audio data");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    closeStreams();
                }
            }
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("WebSocket已关闭: " + reason);
                closeStreams();
            }
        });
        // 立即返回输入流，此时数据可能还在传输中（与HTTP流行为一致）
        return audioInputStream;
    }

    public static String generateSecMsGec(double clockSkewSeconds) {
        // 获取当前UTC时间戳（秒）
        double now = Instant.now().getEpochSecond() + clockSkewSeconds;

        // 转换为Windows文件时间（从1601-01-01开始的秒数）
        double ticks = now + WIN_EPOCH_SECONDS;

        // 向下取整到最近的5分钟（300秒）
        ticks = ticks - (long) ticks % 300;

        // 转换为100纳秒单位
        ticks = ticks * (S_TO_NS / 100);

        // 拼接待哈希字符串
        String strToHash = String.format("%.0f%s", ticks, TrustedClientToken);

        // 计算SHA-256哈希
        return sha256(strToHash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return bytesToHex(hash).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        // 低版本手动实现字节转十六进制
        System.out.println("低版本手动实现字节转十六进制: ");
        // 旧版本 Android，手动实现字节转十六进制字符串
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);

    }

    // 构造SSML文本（与Golang格式完全一致）
    private String mkSSML(String text, String voice, String pitch, String rate, String volume) {
        return String.format(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                        "<voice name='%s'>" +
                        "<prosody pitch='%s' rate='%s' volume='%s'>%s</prosody>" +
                        "</voice>" +
                        "</speak>",
                voice, pitch, rate, volume, text
        );
    }

    // 发送speech.config消息（格式与Golang完全一致）
    private void sendSpeechConfig(WebSocket webSocket) throws IOException {
        String speechConfig = "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"true\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}";

        // 时间格式严格匹配Golang的time.RFC1123
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());

        String speechConfigMsg = String.format(
                "X-Timestamp:%s\r\nContent-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n%s\r\n",
                timestamp, speechConfig
        );
        webSocket.send(speechConfigMsg);
    }

    // 发送SSML消息（格式与Golang完全一致）
    private void sendSSMLMessage(WebSocket webSocket, String ssml) throws IOException {
        String requestId = connectID();
        // 时间格式严格匹配Golang的"Mon Jan 2 2006 15:04:05 GMT-0700 (MST)"
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d yyyy HH:mm:ss zzz", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());

        String ssmlMsg = String.format(
                "X-RequestId:%s\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:%sZ\r\nPath:ssml\r\n\r\n%s",
                requestId, timestamp, ssml
        );
        webSocket.send(ssmlMsg);
    }

    // 生成无破折号的UUID（与Golang一致）
    private String connectID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


}