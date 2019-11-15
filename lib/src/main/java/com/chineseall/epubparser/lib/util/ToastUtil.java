package com.chineseall.epubparser.lib.util;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ToastUtil {
    private static ExecutorService executors = Executors.newFixedThreadPool(5, new ThreadFactory() {
        private AtomicInteger tag = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("Toast-" + tag.getAndIncrement());
            return thread;
        }
    });

    public static void show(Context context, String msg) {
        if ("main".equalsIgnoreCase(Thread.currentThread().getName())) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        } else {
            executors.submit(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            });
        }
    }
}
