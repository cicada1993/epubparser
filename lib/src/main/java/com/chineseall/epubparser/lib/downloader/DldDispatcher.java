package com.chineseall.epubparser.lib.downloader;

class DldDispatcher {
    private DldTaskPool taskPool = new DldTaskPool(DldManager.MAX_TASK_NUM);

    public DldTask createTask(DldModel dldModel) {
        if (dldModel != null) {
            return new DldTask(dldModel);
        }
        return null;
    }

    public void execTask(DldTask task) {
        taskPool.execTask(task);
    }

    public void pause(int taskId) {
        taskPool.pause(taskId);
    }

    public void pauseAll() {
        taskPool.pauseAll();
    }

    public void complete(int taskId) {
        taskPool.complete(taskId);
    }

    public void cancel(int taskId) {
        taskPool.cancel(taskId);
    }

    public void cancelAll() {
        taskPool.cancelAll();
    }
}
