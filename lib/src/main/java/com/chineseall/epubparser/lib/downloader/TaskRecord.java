package com.chineseall.epubparser.lib.downloader;

import android.text.TextUtils;
import android.util.SparseArray;

import com.chineseall.epubparser.lib.util.LogUtil;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

/**
 * 保存下载记录
 */
class TaskRecord extends LitePalSupport {
    public int taskId;
    public long fileLength;
    public String fileName;
    public boolean isMultiThread;
    public int threadNum;
    @DldState
    public int state;
    @Column(ignore = true)
    public SparseArray<Long> slices;
    private String slicesInfo;

    @Column(ignore = true)
    private StringBuffer plot;

    public TaskRecord() {

    }

    public TaskRecord base(int taskId, long fileLength, String fileName, boolean isMultiThread, int threadNum) {
        this.taskId = taskId;
        this.fileLength = fileLength;
        this.fileName = fileName;
        this.isMultiThread = isMultiThread;
        this.threadNum = threadNum;
        return this;
    }

    public TaskRecord state(@DldState int state) {
        this.state = state;
        return this;
    }

    public TaskRecord slice(int threadNo, long sofar) {
        if (slices == null) {
            slices = new SparseArray<>();
        }
        slices.put(threadNo, sofar);
        encodeSlicesInfo();
        return this;
    }

    public TaskRecord appendPlot(String info, boolean isPrint) {
        if (plot == null) {
            plot = new StringBuffer();
        }
        plot.append(info);
        plot.append("\n");
        if (isPrint) {
            LogUtil.d(plot.toString());
        }
        return this;
    }

    private void encodeSlicesInfo() {
        slicesInfo = null;
        if (slices != null) {
            int size = slices.size();
            if (size > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(slices.keyAt(i));
                    sb.append("=");
                    sb.append(slices.valueAt(i));
                }
                slicesInfo = sb.toString();
            }
        }
    }

    public void decodeSlicesInfo() {
        if (slices != null) {
            slices.clear();
        } else {
            slices = new SparseArray<>();
        }
        if (!TextUtils.isEmpty(slicesInfo)) {
            String[] sliceArray = slicesInfo.split(",");
            for (int i = 0; i < sliceArray.length; i++) {
                String[] slice = sliceArray[i].split("=");
                slices.put(Integer.parseInt(slice[0]), Long.parseLong(slice[1]));
            }
        }
    }

}
