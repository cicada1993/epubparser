package com.chineseall.epubparser.lib.downloader;

/**
 * 下载回调
 */
public interface DldListener {

    void onPending(int taskId);

    void onStart(int taskId);

    void onConnect(int taskId);

    void onProgress(int taskId, long readBytes, long totalBytes);

    void onComplete(int taskId, long totalBytes);

    void onPause(int taskId, long readBytes, long totalBytes);

    void onError(int taskId, String errMsg);
}
