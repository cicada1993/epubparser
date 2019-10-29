package com.chineseall.epubparser.lib.downloader;

import android.app.Application;

import com.chineseall.epubparser.lib.BuildConfig;
import com.chineseall.epubparser.lib.util.FileUtil;
import com.chineseall.epubparser.lib.util.LogUtil;

import org.litepal.LitePal;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class DldManager {
    private static volatile DldManager instance;
    private DldDispatcher dispatcher;
    private volatile static boolean hasInit = false;
    public static Application context;
    public static String folderPath;
    public static final int MAX_TASK_NUM = 3;
    private X509TrustManager trustManager;
    private SSLSocketFactory sslSocketFactory;
    private DldManager() {
        this.dispatcher = new DldDispatcher();
        this.trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{trustManager};
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            this.sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
    }

    public static void init(Application application) {
        if (!hasInit) {
            hasInit = true;
            context = application;
            folderPath = FileUtil.getAppCachePath(context) + "/elitedownload";
            LitePal.initialize(application);
        }
    }

    public static DldManager get() {
        if (!hasInit) {
            throw new RuntimeException("DldManager::get invoke init first");
        } else {
            if (instance == null) {
                synchronized (DldManager.class) {
                    if (instance == null) {
                        instance = new DldManager();
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 创建下载任务
     *
     * @param dldModel
     * @return
     */
    public DldTask createTask(DldModel dldModel) {
        if (dldModel == null) {
            LogUtil.d("DldModel cannot be null");
            return null;
        }
        return dispatcher.createTask(dldModel);
    }

    /**
     * 执行下载任务
     *
     * @param dldTask
     */
    public void execTask(DldTask dldTask) {
        dispatcher.execTask(dldTask);
    }

    /**
     * 暂停任务
     *
     * @param taskId
     */
    public void pause(int taskId) {
        dispatcher.pause(taskId);
    }

    /**
     * 暂停全部
     */
    public void pauseAll() {
        dispatcher.pauseAll();
    }

    /**
     * 完成任务
     *
     * @param taskId
     */
    public void complete(int taskId) {
        dispatcher.complete(taskId);
    }

    /**
     * 取消任务
     *
     * @param taskId
     */
    public void cancel(int taskId) {
        dispatcher.cancel(taskId);
    }

    /**
     * 取消全部
     */
    public void cancelAll() {
        dispatcher.cancelAll();
    }

    public OkHttpClient.Builder clientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(315, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .addInterceptor(
                        new HttpLoggingInterceptor().setLevel(
                                BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE)
                );
        return builder;
    }
}
