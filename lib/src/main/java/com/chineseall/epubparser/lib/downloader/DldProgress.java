package com.chineseall.epubparser.lib.downloader;

class DldProgress {
    public int taskId = -1;
    @DldState
    public int state = DldState.INVALID;
    public String errMsg;
    public long total = -1L;
    public long sofar = -1L;

    private static final int MAX_POOL_SIZE = 500;
    private static int poolSize = 0;
    private static DldProgress pool = null;
    private DldProgress next = null;
    private static final Object lock = new Object();

    private DldProgress() {

    }

    public static DldProgress obtain() {
        synchronized (lock) {
            if (pool != null) {
                DldProgress m = pool;
                pool = m.next;
                m.next = null;
                poolSize--;
                m.reset();
                return m;
            }
        }
        return new DldProgress();
    }

    private void reset() {
        taskId = -1;
        state = DldState.INVALID;
        errMsg = null;
        total = -1L;
        sofar = -1L;
    }

    public void recycle() {
        reset();
        synchronized (lock) {
            if (poolSize < MAX_POOL_SIZE) {
                next = pool;
                pool = this;
                poolSize++;
            }
        }
    }
}
