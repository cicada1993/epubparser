package com.chineseall.epubparser.lib.downloader;

import android.util.SparseArray;

import java.io.File;

class DldTaskPool {
    private int maxNum; // 最大任务数
    private SparseArray<DldTask> runningTasks; // 执行中的任务
    private SparseArray<DldTask> waitingTasks; // 等待中的任务

    public DldTaskPool(int maxNum) {
        this.maxNum = maxNum;
        this.runningTasks = new SparseArray<>();
        this.waitingTasks = new SparseArray<>();
    }

    /**
     * 执行任务
     *
     * @param task
     */
    public void execTask(DldTask task) {
        synchronized (runningTasks) {
            DldModel model = task.getDldModel();
            File file = new File(model.filePath);
            if ((file.exists() || task.getState() == DldState.COMPLETE) && !task.isFresh()) {
                // 已经下载完成 不需要重新下载
                task.complete();
                return;
            }
            int taskId = task.getTaskId();
            pause(taskId);
            if (runningTasks.size() == maxNum) {
                waitingTasks.put(task.getTaskId(), task);
                task.pending();
            } else {
                runningTasks.put(task.getTaskId(), task);
                task.pending();
                task.start();
            }
        }
    }

    /**
     * 完成
     *
     * @param taskId
     */
    public void complete(int taskId) {
        synchronized (runningTasks) {
            DldTask task = runningTasks.get(taskId);
            if (task != null) {
                task.complete();
                runningTasks.remove(taskId);
            }
            if (waitingTasks.size() > 0) {
                execTask(waitingTasks.get(0));
            }
        }
    }

    /**
     * 暂停
     *
     * @param taskId
     */
    public void pause(int taskId) {
        synchronized (runningTasks) {
            DldTask runTask = runningTasks.get(taskId);
            if (runTask != null) {
                runTask.pause();
                runningTasks.remove(taskId);
            }
            DldTask waitTask = waitingTasks.get(taskId);
            if (waitTask != null) {
                waitTask.pause();
                waitingTasks.remove(taskId);
            }
        }
    }

    /**
     * 暂停全部
     */
    public void pauseAll() {
        synchronized (runningTasks) {
            int runSize = runningTasks.size();
            int waitSize = waitingTasks.size();
            int[] ids = new int[runSize];
            DldTask tmp;
            for (int i = 0; i < runSize; i++) {
                tmp = runningTasks.valueAt(i);
                tmp.pause();
                ids[i] = tmp.getTaskId();
            }
            for (int j = 0; j < waitSize; j++) {
                tmp = waitingTasks.valueAt(j);
                tmp.pause();
            }

            for (int k = 0; k < ids.length; k++) {
                runningTasks.remove(ids[k]);
            }
        }
    }

    /**
     * 取消
     *
     * @param taskId
     */
    public void cancel(int taskId) {
        synchronized (runningTasks) {
            DldTask runTask = runningTasks.get(taskId);
            if (runTask != null) {
                runTask.cancel();
                runningTasks.remove(taskId);
            }
            DldTask waitTask = waitingTasks.get(taskId);
            if (waitTask != null) {
                waitTask.cancel();
                runningTasks.remove(taskId);
            }
        }
    }

    /**
     * 取消全部
     */
    public void cancelAll() {
        synchronized (runningTasks) {
            int runSize = runningTasks.size();
            int waitSize = waitingTasks.size();
            int[] ids = new int[runSize + waitSize];
            int[] flags = new int[runSize + waitSize];
            DldTask tmp;
            for (int i = 0; i < runningTasks.size(); i++) {
                tmp = runningTasks.valueAt(i);
                tmp.cancel();
                ids[i] = tmp.getTaskId();
                flags[i] = 1;
            }
            for (int j = 0; j < waitingTasks.size(); j++) {
                tmp = waitingTasks.valueAt(j);
                tmp.cancel();
                ids[runSize + j] = tmp.getTaskId();
                flags[runSize + j] = 0;
            }
            for (int k = 0; k < ids.length; k++) {
                if (flags[k] == 1) {
                    runningTasks.remove(ids[k]);
                } else {
                    waitingTasks.remove(ids[k]);
                }
            }
        }
    }
}
