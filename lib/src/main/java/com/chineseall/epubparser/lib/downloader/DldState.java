package com.chineseall.epubparser.lib.downloader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 下载任务状态
 */
@Retention(RetentionPolicy.SOURCE)
public @interface DldState {
    int INVALID = 0; // 初始
    int PENDING = 1; // 等待
    int START = 2; // 开始
    int CONNECT = 3; // 连接
    int PROGRESS = 4; // 进度
    int COMPLETE = 5; // 完成
    int PAUSE = 6; // 暂停
    int ERROR = 7; // 失败
    int CANCELED = 8; // 取消
}
