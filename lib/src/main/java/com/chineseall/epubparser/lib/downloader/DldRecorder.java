package com.chineseall.epubparser.lib.downloader;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

import org.litepal.LitePal;

import java.util.List;

/**
 * 主要管理下载记录
 */
class DldRecorder {
    private static volatile DldRecorder instance;
    private HandlerThread writerThread;
    private WriterHandler writerHandler;
    private static final int MSG_WRITE = 0x01;
    private SparseArray<TaskRecord> records;

    private DldRecorder() {
        this.writerThread = new HandlerThread("DldRecorder");
        this.writerThread.start();
        this.writerHandler = new WriterHandler(writerThread.getLooper());
        this.records = new SparseArray<>();
    }

    public static DldRecorder get() {
        if (instance == null) {
            synchronized (DldRecorder.class) {
                if (instance == null) {
                    instance = new DldRecorder();
                }
            }
        }
        return instance;
    }

    /**
     * 保存或更新记录
     *
     * @param record
     */
    public void record(TaskRecord record) {
        synchronized (records) {
            records.put(record.taskId, record);
            // 更新数据库
            record.saveOrUpdate("taskId = ?", String.valueOf(record.taskId));
        }
    }

    /**
     * 移除下载记录
     *
     * @param taskId
     */
    public void remove(int taskId) {
        synchronized (records) {
            records.remove(taskId);
            // 从数据库移除
            LitePal.deleteAll(TaskRecord.class, "taskId = ?", String.valueOf(taskId));
        }
    }

    /**
     * 读取下载记录 先从内存中查找
     *
     * @param taskId
     * @return
     */
    public TaskRecord readRecord(int taskId) {
        synchronized (records) {
            TaskRecord record = records.get(taskId);
            if (record == null) {
                record = readRecordFromDB(taskId);
            }
            return record;
        }
    }

    /**
     * 从数据库读取下载记录
     *
     * @param taskId
     * @return
     */
    private TaskRecord readRecordFromDB(int taskId) {
        List<TaskRecord> records = LitePal.where("taskId = ?", String.valueOf(taskId)).find(TaskRecord.class);
        if (records != null && records.size() > 0) {
            TaskRecord record = records.get(0);
            record.decodeSlicesInfo();
            return record;
        }
        return null;
    }

    private void writeBytes() {

    }

    private class WriterHandler extends Handler {
        public WriterHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WRITE:
                    break;
            }
        }
    }
}
