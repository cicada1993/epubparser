package com.chineseall.epubparser.lib.downloader;

import android.os.SystemClock;

import com.chineseall.epubparser.lib.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

class DldRunnable implements Runnable {
    private String url;
    private long blockSize;
    public long from; // 初始下载起点
    public long to; // 初始下载终点
    private long initSofar; // 已下载大小
    public int threadNo;
    private String filePath;
    private int taskId;
    private boolean isMultiThread;
    private RunnableCallback callback;
    private boolean isPause = false;
    private boolean isCancel = false;
    private boolean isComplete = false;
    private OkHttpClient dldClient;
    private ProgressListener progressListener;
    private RandomAccessFile randomAccessFile;
    private long startPosition; // 下载起始位置
    private long readBytes = 0L; // 已读
    private static final int BUFFER_SIZE = 1024 * 2;
    private Call call;

    public DldRunnable(String url, long blockSize, long from, long to,
                       int threadNo, String filePath, int taskId,
                       boolean isMultiThread, long initSofar, RunnableCallback callback) {
        this.url = url;
        this.blockSize = blockSize;
        this.from = from;
        this.to = to;
        this.threadNo = threadNo;
        this.initSofar = initSofar;
        this.filePath = filePath;
        this.taskId = taskId;
        this.isMultiThread = isMultiThread;
        this.callback = callback;
        init();
    }

    private long last = 0L;

    private void init() {
        if (isMultiThread) {
            initSofar += readBytes;
            startPosition = from + initSofar;
        } else {
            // 不支持多线程 每次都从头下载
            startPosition = from;
        }
        progressListener = new ProgressListener() {
            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                if (done) {
                    LogUtil.d("下载完成：" + bytesRead + "/" + contentLength);
                    complete();
                } else {
                    readBytes = bytesRead;
                    long now = SystemClock.elapsedRealtime();
                    if ((!isCancel && !isPause) && (now - last > 1000)) {
                        progress();
                    }
                }
            }
        };
        dldClient = DldManager.get().clientBuilder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                            .build();
                })
                .build();
    }

    @Override
    public void run() {
        isCancel = false;
        isPause = false;
        if (isComplete || sofar() >= (to - from)) {
            isComplete = true;
            complete();
            return;
        }
        isComplete = false;
        if (isMultiThread) {
            initSofar += readBytes;
            startPosition = from + initSofar;
        } else {
            startPosition = from;
        }
        LogUtil.d("下载信息：from " + from + " to" + to + " startPosition " + startPosition);
        readBytes = 0L;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("security","ChineseAll&*(")
                .header("Range", "bytes=" + startPosition + "-" + to)
                .build();
        call = dldClient.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                File destFile = new File(filePath);
                if (!destFile.exists()) {
                    destFile.createNewFile();
                }
                if (randomAccessFile == null) {
                    randomAccessFile = new RandomAccessFile(destFile, "rw");
                }
                FileChannel fileChannel = randomAccessFile.getChannel();
                MappedByteBuffer mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, startPosition, body.contentLength());
                byte[] buffer = new byte[BUFFER_SIZE];
                BufferedSource source = body.source();
                int len;
                while ((len = source.read(buffer)) != -1) {
                    if (isPause || isCancel) {
                        break;
                    }
                    mappedBuffer.put(buffer, 0, len);
                }
                fileChannel.close();
                randomAccessFile.close();
            } else {
                error("request failed: the code is " + response.code());
            }
            response.close();
        } catch (Exception e) {
            if (isPause || isCancel) {
                StringBuilder sb = new StringBuilder();
                sb.append(isPause ? "已暂停" : isCancel ? "已取消" : "");
                sb.append(" 异常类型：");
                sb.append(e.getClass().getSimpleName());
                sb.append(e.getMessage());
                LogUtil.d(sb.toString());
            } else {
                error("request failed: " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        }
    }

    public boolean isComplete() {
        return isComplete;
    }

    public long sofar() {
        return startPosition - from + readBytes;
    }

    public void pause() {
        isPause = true;
    }

    public void cancel() {
        isCancel = true;
    }

    /**
     * 进度回调
     */
    private void progress() {
        callback.onRunnableProgress(threadNo);
    }

    /**
     * 完成回调
     */
    private void complete() {
        isComplete = true;
        callback.onRunnableComplete(threadNo);
    }

    /**
     * 失败回调
     *
     * @param msg
     */
    private void error(String msg) {
        callback.onRunnableError(threadNo, msg);
    }

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    public String getInfo() {
        return "threadNo：" + threadNo + " from：" + from + " to：" + to + " sofar：" + initSofar;
    }

    interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }
}
