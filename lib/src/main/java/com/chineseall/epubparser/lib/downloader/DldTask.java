package com.chineseall.epubparser.lib.downloader;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.core.util.Pair;

import com.chineseall.epubparser.lib.util.AppUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 下载任务
 */
public class DldTask implements Comparable<DldTask>, RunnableCallback {
    private final ReentrantLock lock;
    private DldModel dldModel;
    private int taskId;
    private int state = DldState.INVALID; // 下载状态
    private long fileLength = 0; // 文件大小
    private String fileName = null; // 文件名
    private boolean isMultiThread = false; // 是否支持多线程下载
    private boolean readSuccess = false; // 文件读取是否成功

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    public static final String ETAG = "Etag";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    public static final String CONTENT_RANGE = "Content-Range";

    public static final String TYPE_CHUNKED = "chunked";

    private static final Pattern CONTENT_DISPOSITION_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");
    // no note
    private static final Pattern CONTENT_DISPOSITION_NON_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*(.*)");

    private OkHttpClient okClient;
    private static final int DEFAULT_THREAD_NUM = 3;
    private int threadNum = -1;
    private long minProgressGap = 1000; // ms 最小进度回调间隔
    private boolean isNotify = false; // 是否显示通知栏
    private boolean isApkInstall = false; // 安装包自动安装
    private boolean isFresh = true; // 下载完成是否再次重新下载
    private boolean noProgress = false; // 下载过程中不发送进度消息
    private List<DldRunnable> list; // 下载任务列表
    private TaskRecord record; // 下载记录
    private Context context;

    public DldTask(DldModel dldModel) {
        this.context = DldManager.context;
        this.dldModel = dldModel;
        this.lock = new ReentrantLock();
        init();
    }

    private void init() {
        File folder = new File(dldModel.folder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.okClient = DldManager.get().clientBuilder().build();
        this.taskId = dldModel.key.hashCode();
        this.list = Collections.synchronizedList(new ArrayList<>());
        int index = dldModel.url.lastIndexOf('/') + 1;
        if (index > 0) {
            this.fileName = dldModel.url.substring(index);
        }
        File file = new File(dldModel.filePath);
        if (file.exists()) {
            record = DldRecorder.get().readRecord(taskId);
            if (record != null) {
                this.state = record.state;
                this.fileLength = record.fileLength;
                this.fileName = record.fileName;
                this.isMultiThread = record.isMultiThread;
                this.threadNum = record.threadNum;
                this.readSuccess = this.fileLength > 0;
                obtainRecord().appendPlot("从下载记录中恢复", true);
            }
        }
    }

    /**
     * 任务执行入口
     */
    public void setUp() {
        DldManager.get().execTask(this);
    }

    /**
     * 添加任务监听
     *
     * @param listener
     * @return
     */
    public DldTask listener(DldListener listener) {
        DldEventBus.get().addListener(getTaskId(), listener);
        return this;
    }

    /**
     * 指定下载线程数
     *
     * @param threadNum
     * @return
     */
    public DldTask threadNum(int threadNum) {
        if (this.threadNum == -1) {
            this.threadNum = threadNum;
        }
        return this;
    }

    /**
     * 是否显示通知栏
     *
     * @param isNotify
     * @return
     */
    public DldTask notify(boolean isNotify) {
        this.isNotify = isNotify;
        return this;
    }

    /**
     * 是否apk自动安装
     *
     * @param isApkInstall
     * @return
     */
    public DldTask apkInstall(boolean isApkInstall) {
        this.isApkInstall = isApkInstall;
        return this;
    }

    /**
     * 下载完成后是否重新下载
     *
     * @param isFresh
     * @return
     */
    public DldTask fresh(boolean isFresh) {
        this.isFresh = isFresh;
        return this;
    }

    /**
     * 下载过程中不回调进度
     *
     * @param noProgress
     * @return
     */
    public DldTask noProgress(boolean noProgress) {
        this.noProgress = noProgress;
        return this;
    }

    /**
     * 获取下载状态
     *
     * @return
     */
    public int getState() {
        return state;
    }

    public DldModel getDldModel() {
        return dldModel;
    }

    /**
     * 获取任务id
     *
     * @return
     */
    public int getTaskId() {
        return taskId;
    }

    public boolean isFresh() {
        return isFresh;
    }

    /**
     * 解析响应头中的文件名
     *
     * @param contentDisposition
     * @return
     */
    private String parseFileName(String contentDisposition) {
        String fileName = null;
        Matcher m = CONTENT_DISPOSITION_QUOTED_PATTERN.matcher(contentDisposition);
        if (m.find()) {
            fileName = m.group(1);
        } else {
            m = CONTENT_DISPOSITION_NON_QUOTED_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                fileName = m.group(1);
            }
        }
        if (fileName != null && fileName.contains("../")) {

        }
        return fileName;
    }

    private TaskRecord obtainRecord() {
        if (record == null) {
            record = new TaskRecord();
        }
        return record.base(taskId, fileLength, fileName, isMultiThread, threadNum).state(state);
    }

    private DldProgress obtainProgress() {
        DldProgress progress = DldProgress.obtain();
        progress.taskId = taskId;
        progress.state = state;
        progress.total = fileLength;
        return progress;
    }

    /**
     * 等待下载
     */
    void pending() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (state != DldState.PENDING) {
                state = DldState.PENDING;
                DldEventBus.get().postToMain(obtainProgress());
                obtainRecord().appendPlot("等待下载", false);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 开始下载
     */
    void start() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            state = DldState.START;
            DldEventBus.get().postToMain(obtainProgress());
            obtainRecord().appendPlot("开始下载", false);
            AutoAdjustThreadPool.get().execute(new Runnable() {
                @Override
                public void run() {
                    startAsync();
                }
            });
        } finally {
            lock.unlock();
        }
    }

    /**
     * 真正的下载开始
     */
    private void startAsync() {
        if (!readSuccess) {
            // 读取文件信息
            readSuccess = readFileInfo();
        }
        if (readSuccess) {
            createTaskList();
            runTaskList();
        }
    }

    /**
     * 创建任务列表
     */
    private void createTaskList() {
        if (!isMultiThread) {
            // 不支持多线程下载
            threadNum = 1;
        } else if (threadNum == -1) {
            threadNum = DEFAULT_THREAD_NUM;
        }
        obtainRecord().appendPlot("创建任务列表", false);
        obtainRecord().appendPlot("下载线程数：" + threadNum, false);
        list.clear();
        long blockSize = fileLength / threadNum;
        // 下载起点
        long tmpFrom = 0L;
        // 下载终点
        long tmpTo = 0L;
        // 已下载
        long tmpSofar = 0L;
        SparseArray<Long> slices = null;
        if (record != null) {
            slices = record.slices;
        }
        for (int i = 0; i < threadNum; i++) {
            tmpFrom = blockSize * i;
            tmpTo = (i == threadNum - 1) ? (fileLength - 1) : (tmpFrom + blockSize - 1);
            tmpSofar = slices != null ? slices.get(i, 0L) : 0L;
            DldRunnable runnable = new DldRunnable(
                    dldModel.url,
                    blockSize,
                    tmpFrom,
                    tmpTo,
                    i,
                    dldModel.filePath,
                    taskId,
                    isMultiThread,
                    tmpSofar,
                    this
            );
            list.add(runnable);
            obtainRecord().appendPlot(runnable.getInfo(), i == threadNum - 1);
        }
    }

    private void runTaskList() {
        for (DldRunnable runnable : list) {
            AutoAdjustThreadPool.get().execute(runnable);
        }
    }

    /**
     * 读取文件信息
     *
     * @return
     */
    private boolean readFileInfo() {
        state = DldState.CONNECT;
        DldEventBus.get().postToMain(obtainProgress());
        obtainRecord().appendPlot("读取文件信息", false);
        Pair<Boolean, String> headRes = headRequest();
        obtainRecord().appendPlot(headRes.second, false);
        if (headRes.first) {
            obtainRecord().appendPlot(
                    "文件大小：" + fileLength + " / " +
                            "文件名：" + fileName + " / " +
                            "是否支持多线程下载：" + isMultiThread,
                    true
            );
            return true;
        } else {
            Pair<Boolean, String> getRes = getRequest();
            obtainRecord().appendPlot(getRes.second, false);
            if (getRes.first) {
                obtainRecord().appendPlot(
                        "文件大小：" + fileLength + " / " +
                                "文件名：" + fileName + " / " +
                                "是否支持多线程下载：" + isMultiThread,
                        true
                );
                return true;
            } else {
                error(getRes.second);
                return false;
            }
        }
    }

    /**
     * head请求读取文件信息
     *
     * @return
     */
    private Pair<Boolean, String> headRequest() {
        boolean res;
        String msg;
        Request request = new Request.Builder()
                .head()
                .header("security", " ChineseAll&*(")
                .url(dldModel.url)
                .build();
        Call call = okClient.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                Headers headers = response.headers();
                String contentLength = headers.get(CONTENT_LENGTH);
                if (!TextUtils.isEmpty(headers.get(CONTENT_LENGTH))) {
                    fileLength = Long.parseLong(contentLength);
                }
                if (!TextUtils.isEmpty(headers.get(ACCEPT_RANGES))) {
                    isMultiThread = true;
                }
                String transferEncoding = headers.get(TRANSFER_ENCODING);
                if (TextUtils.equals(transferEncoding, TYPE_CHUNKED)) {
                    isMultiThread = false;
                }
                String contentDisposition = headers.get(CONTENT_DISPOSITION);
                if (!TextUtils.isEmpty(contentDisposition)) {
                    fileName = parseFileName(contentDisposition);
                }
                if (fileLength <= 0) {
                    res = false;
                    msg = "can not read file length";
                } else {
                    res = true;
                    msg = "head request success";
                }
            } else {
                res = false;
                msg = "head request failed: the code is " + response.code();
            }
            response.close();
        } catch (Exception e) {
            res = false;
            msg = "head request failed: " + e.getMessage();
        }
        return new Pair<>(res, msg);
    }

    /**
     * get 请求读取文件信息
     *
     * @return
     */
    private Pair<Boolean, String> getRequest() {
        boolean res;
        String msg;
        Request request = new Request.Builder()
                .head()
                .header("security", " ChineseAll&*(")
                .url(dldModel.url)
                .build();
        Call call = okClient.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                Headers headers = response.headers();
                String contentLength = headers.get(CONTENT_LENGTH);
                if (!TextUtils.isEmpty(headers.get(CONTENT_LENGTH))) {
                    fileLength = Long.parseLong(contentLength);
                }
                if (!TextUtils.isEmpty(headers.get(ACCEPT_RANGES))) {
                    isMultiThread = true;
                }
                String transferEncoding = headers.get(TRANSFER_ENCODING);
                if (TextUtils.equals(transferEncoding, TYPE_CHUNKED)) {
                    isMultiThread = false;
                }
                String contentDisposition = headers.get(CONTENT_DISPOSITION);
                if (!TextUtils.isEmpty(contentDisposition)) {
                    fileName = parseFileName(contentDisposition);
                }
                if (fileLength <= 0) {
                    res = false;
                    msg = "can not read file length";
                } else {
                    res = true;
                    msg = "get request success";
                }
            } else {
                res = false;
                msg = "get request failed: the code is " + response.code();
            }
            response.close();
        } catch (Exception e) {
            res = false;
            msg = "get request failed: " + e.getMessage();
        }
        return new Pair<>(res, msg);
    }

    /**
     * 下载进度
     */
    void progress() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            state = DldState.PROGRESS;
            long totalSofar = 0L;
            for (DldRunnable runnable : list) {
                totalSofar += runnable.sofar();
            }
            if (!noProgress) {
                DldProgress progress = obtainProgress();
                progress.sofar = totalSofar;
                DldEventBus.get().postToMain(progress);
            }
            if (isNotify) {
                DldNotifier.get()
                        .show(
                                taskId, fileName,
                                100f * totalSofar / fileLength, "下载中",
                                isApkInstall, dldModel.filePath
                        );
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 下载完成
     */
    void complete() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            state = DldState.COMPLETE;
            long tmpSofar;
            for (DldRunnable runnable : list) {
                tmpSofar = runnable.sofar();
                AutoAdjustThreadPool.get().remove(runnable);
                obtainRecord().slice(runnable.threadNo, tmpSofar);
            }
            DldEventBus.get().postToMain(obtainProgress());
            obtainRecord().appendPlot("下载完成", true);
            if (isFresh) {
                // 下载完成后删除下载记录
                DldRecorder.get().remove(taskId);
            } else {
                DldRecorder.get().record(obtainRecord());
            }
            if (isNotify) {
                DldNotifier.get()
                        .show(
                                taskId, fileName,
                                100f, "下载完成",
                                isApkInstall, dldModel.filePath
                        );
            }
            if (isApkInstall) {
                // 直接安装apk
                context.startActivity(AppUtil.getInstallIntent(context, new File(dldModel.filePath)));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 暂停下载
     */
    void pause() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            state = DldState.PAUSE;
            long sofar = 0L;
            long tmpSofar;
            for (DldRunnable runnable : list) {
                runnable.pause();
                tmpSofar = runnable.sofar();
                sofar += tmpSofar;
                AutoAdjustThreadPool.get().remove(runnable);

                obtainRecord().slice(runnable.threadNo, tmpSofar);
            }
            DldProgress progress = obtainProgress();
            progress.sofar = sofar;
            DldEventBus.get().postToMain(progress);
            DldRecorder.get().record(obtainRecord().appendPlot("暂停下载", true));
            if (isNotify) {
                DldNotifier.get()
                        .show(
                                taskId, fileName,
                                100f * sofar / fileLength, "已暂停",
                                isApkInstall, dldModel.filePath
                        );
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 下载出错
     *
     * @param msg 出错信息
     */
    void error(String msg) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (state != DldState.ERROR) {
                // 保证只发送一次错误
                state = DldState.ERROR;
                long sofar = 0L;
                long tmpSofar;
                // 暂停所有任务
                for (DldRunnable runnable : list) {
                    runnable.pause();
                    tmpSofar = runnable.sofar();
                    sofar += tmpSofar;
                    AutoAdjustThreadPool.get().remove(runnable);
                    obtainRecord().slice(runnable.threadNo, tmpSofar);
                }
                DldProgress progress = obtainProgress();
                progress.errMsg = msg;
                DldEventBus.get().postToMain(progress);
                DldRecorder.get().record(obtainRecord().appendPlot("下载失败：" + msg, true));
                if (isNotify) {
                    DldNotifier.get()
                            .show(
                                    taskId, fileName,
                                    100f * sofar / fileLength, "下载失败",
                                    isApkInstall, dldModel.filePath
                            );
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 取消下载
     */
    void cancel() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            state = DldState.CANCELED;
            for (DldRunnable runnable : list) {
                runnable.cancel();
                AutoAdjustThreadPool.get().remove(runnable);
            }
            if (isNotify) {
                DldNotifier.get().remove(taskId);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 线程下载进度回调
     *
     * @param threadNo
     */
    private long last = 0L;

    @Override
    public void onRunnableProgress(int threadNo) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (state != DldState.PROGRESS) {
                state = DldState.PROGRESS;
                obtainRecord().appendPlot("正在下载", false);
            }
            long now = SystemClock.elapsedRealtime();
            if (now - last >= minProgressGap) {
                progress();
                last = SystemClock.elapsedRealtime();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 线程下载完成回调
     *
     * @param threadNo
     */
    @Override
    public void onRunnableComplete(int threadNo) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            boolean isComplete = true;
            for (DldRunnable runnable : list) {
                if (!runnable.isComplete()) {
                    isComplete = false;
                    break;
                }
            }
            // 所有任务都完成才能回调下载完成
            if (isComplete) {
                DldManager.get().complete(taskId);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 线程下载出错回调
     *
     * @param threadNo
     * @param msg
     */
    @Override
    public void onRunnableError(int threadNo, String msg) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            error(msg);
        } finally {
            lock.unlock();
        }
    }

    // > 返回1 升序 >返回 -1 降序
    @Override
    public int compareTo(DldTask o) {
        return 0;
    }
}
