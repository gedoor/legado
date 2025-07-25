package io.legado.app.service;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EdgeTest {

    private PipedInputStream pipedInput;
    private PipedOutputStream pipedOutput;
    private ExecutorService executor; // 用于异步写入的线程池
    private Context context;

    public void AssetAudioPipedStream(Context context) {
        this.context = context;
        try {
            // 初始化管道流（输入流与输出流关联）
            pipedOutput = new PipedOutputStream();
            pipedInput = new PipedInputStream(pipedOutput, 8192); // 缓冲区8KB
            executor = Executors.newSingleThreadExecutor(); // 单线程池处理异步写入
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始从assets读取MP3并异步写入管道流（每1秒写入一次）
     *
     * @return 管道输入流，供外部读取
     */
    public PipedInputStream startStreaming() {
        executor.execute(() -> {
            InputStream assetIn = null;
            try {
                // 1. 从assets获取MP3输入流
                assetIn = context.getAssets().open("output.mp3");

                // 2. 每次读取1024字节（模拟分片传输）
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = assetIn.read(buffer)) != -1) {
                    // 写入管道输出流
                    pipedOutput.write(buffer, 0, bytesRead);
                    pipedOutput.flush(); // 立即刷新，让输入流能读取到

                    // 每写入一次等待1秒，模拟异步延迟
                    Thread.sleep(200);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 3. 关闭资源
                try {
                    if (assetIn != null) assetIn.close();
                    pipedOutput.close(); // 关闭输出流，通知输入流传输结束
                } catch (IOException e) {
                    e.printStackTrace();
                }
                executor.shutdown(); // 关闭线程池
            }
        });
        // 立即返回输入流，此时可能还未开始写入（模拟网络流的异步性）
        return pipedInput;
    }

}