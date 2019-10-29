package com.chineseall.epubparser.lib.downloader;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

class DldEventBus {
    private static volatile DldEventBus instance;
    private MHandler mainHandler;
    private SparseArray<DldListener> listeners;
    private static final int MSG_PROGRESS = 0x01;

    private DldEventBus() {
        this.mainHandler = new MHandler(Looper.getMainLooper());
        this.listeners = new SparseArray<>();
    }

    public static DldEventBus get() {
        if (instance == null) {
            synchronized (DldEventBus.class) {
                if (instance == null) {
                    instance = new DldEventBus();
                }
            }
        }
        return instance;
    }

    public void addListener(int taskId, DldListener listener) {
        listeners.put(taskId, listener);
    }

    public void removeListener(int taskId) {
        listeners.remove(taskId);
    }

    public void postToMain(DldProgress progress) {
        Message message = Message.obtain();
        message.what = MSG_PROGRESS;
        message.obj = progress;
        mainHandler.sendMessage(message);
    }

    private void progressData(DldProgress progress) {
        switch (progress.state) {
            case DldState.INVALID:
                break;
            case DldState.PENDING:
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.valueAt(i).onPending(progress.taskId);
                }
                break;
            case DldState.START:
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.valueAt(i).onStart(progress.taskId);
                }
                break;
            case DldState.CONNECT:
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.valueAt(i).onConnect(progress.taskId);
                }
                break;
            case DldState.PROGRESS:
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.valueAt(i).onProgress(progress.taskId, progress.sofar, progress.total);
                }
                break;
            case DldState.COMPLETE:
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.valueAt(i).onComplete(progress.taskId, progress.total);
                }
                break;
            case DldState.PAUSE:
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.valueAt(i).onPause(progress.taskId, progress.sofar, progress.total);
                }
                break;
            case DldState.ERROR:
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.valueAt(i).onError(progress.taskId, progress.errMsg);
                }
                break;
            default:
                break;
        }
    }

    private class MHandler extends Handler {
        public MHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS:
                    progressData((DldProgress) msg.obj);
                    break;
                default:
                    break;
            }
        }
    }
}
