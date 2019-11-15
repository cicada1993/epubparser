package com.chineseall.epubparser;

import java.util.ArrayList;
import java.util.List;

// 用于外部处理扫码结果
public class ScanResultBus {
    private static List<Consumer> consumers;

    public static void register(Consumer consumer) {
        if (consumers == null) {
            consumers = new ArrayList<>();
        }
        if (consumers.contains(consumer)) {
            // 已经存在
        } else {
            consumers.add(consumer);
        }
    }

    public static void unregister(Consumer consumer) {
        if (consumers != null) {
            consumers.remove(consumer);
        }
    }

    public static void handleResult(String result) {
        if (consumers != null) {
            for (Consumer consumer : consumers) {
                if (consumer != null) {
                    consumer.onScanResult(result);
                }
            }
        }
    }

    public interface Consumer {
        void onScanResult(String result);
    }
}
