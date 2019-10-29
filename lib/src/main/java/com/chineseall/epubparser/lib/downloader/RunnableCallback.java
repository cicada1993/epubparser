package com.chineseall.epubparser.lib.downloader;

/**
 * 子任务中回调相关状态
 */
interface RunnableCallback {
    void onRunnableProgress(int threadNo);

    void onRunnableComplete(int threadNo);

    void onRunnableError(int threadNo, String msg);
}
