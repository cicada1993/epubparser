package com.chineseall.epubparser.lib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chineseall.epubparser.lib.R;
import com.chineseall.epubparser.lib.downloader.AutoAdjustThreadPool;
import com.chineseall.epubparser.lib.downloader.DldListener;
import com.chineseall.epubparser.lib.downloader.DldManager;
import com.chineseall.epubparser.lib.downloader.DldModel;
import com.chineseall.epubparser.lib.downloader.DldTask;
import com.chineseall.epubparser.lib.util.FileUtil;
import com.chineseall.epubparser.lib.util.LogUtil;
import com.chineseall.epubparser.lib.util.NetWorkUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 思源字体文本控件
 */
public class SerifTextView extends AppCompatTextView {
    private static final String FONT_HOST = "http://www.fonts.net.cn";
    // 七种思源字体类型
    public static final String BOLD = "Bold";
    public static final String EXTRA_LIGHT = "ExtraLight";
    public static final String HEAVY = "Heavy";
    public static final String LIGHT = "Light";
    public static final String MEDIUM = "Medium";
    public static final String REGULAR = "Regular";
    public static final String SEMI_BOLD = "SemiBold";
    private static final String FONT_ID = "32381422163";
    private static final String FONT_FOLDER = "/font/serif";
    private static final String FONT_UNZIP_FOLDER = "/font/serif/unzip";
    private DldTask task;
    private int fontType = 6;
    public static HashMap<String, Typeface> serifTypefaces = new HashMap<>();

    public SerifTextView(Context context) {
        this(context, null);
    }

    public SerifTextView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SerifTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setIncludeFontPadding(false);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs,
                    R.styleable.SerifTextView);
            fontType = typedArray.getInt(R.styleable.SerifTextView_fontType, 6);
        }
        loadQuiet();
    }

    public void loadQuiet() {
        File file = isExist();
        if (file == null) {
            LogUtil.d("字体文件不存在");
            loadFontUrl();
        } else {
            LogUtil.d("字体文件已存在");
            onFontSuccess();
        }
    }

    private void loadFontUrl() {
        if (NetWorkUtil.isWifiConnected(getContext())) {
            AutoAdjustThreadPool.get().execute(new Runnable() {
                @Override
                public void run() {
                    OkHttpClient client = DldManager.get().clientBuilder().build();
                    FormBody formBody = new FormBody.Builder()
                            .add("id", FONT_ID)
                            .add("source", "font")
                            .build();
                    Request request = new Request.Builder()
                            .url(FONT_HOST + "/font-download.html")
                            .post(formBody)
                            .build();
                    try {
                        Response response = client.newCall(request).execute();
                        String result = response.body().string();
                        JSONObject jsonObject = JSON.parseObject(result);
                        String status = jsonObject.getString("status");
                        if (TextUtils.equals("OK", status)) {
                            String url = jsonObject.getString("url");
                            LogUtil.d("下载字体：" + url);
                            download(url);
                        }
                    } catch (IOException e) {

                    }
                }
            });
        } else {
            onFontFailed();
        }
    }

    private void download(String url) {
        String fontDir = FileUtil.getSDCardAppFilePath(getContext()) + FONT_FOLDER + "/";
        DldModel model = new DldModel.Builder().key(FONT_ID).url(url).folder(fontDir).build();
        task = DldManager.get().createTask(model);
        task.listener(
                new DldListener() {
                    @Override
                    public void onPending(int taskId) {

                    }

                    @Override
                    public void onStart(int taskId) {

                    }

                    @Override
                    public void onConnect(int taskId) {

                    }

                    @Override
                    public void onProgress(int taskId, long readBytes, long totalBytes) {

                    }

                    @Override
                    public void onComplete(int taskId, long totalBytes) {
                        LogUtil.d("下载完成");
                        unzipFont();
                    }

                    @Override
                    public void onPause(int taskId, long readBytes, long totalBytes) {

                    }

                    @Override
                    public void onError(int taskId, String errMsg) {

                    }
                })
                .fresh(false)
                .noProgress(true)
                .setUp();
    }

    private void unzipFont() {
        AutoAdjustThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                LogUtil.d("解压字体");
                String fontUnzipDir = FileUtil.getSDCardAppFilePath(getContext()) + FONT_UNZIP_FOLDER + "/";
                DldModel model = task.getDldModel();
                if (FileUtil.unzip(model.filePath, fontUnzipDir)) {
                    LogUtil.d("解压完成");
                    onFontSuccess();
                }
            }
        });
    }

    private void onFontSuccess() {
        File fontRealDir = isExist();
        if (fontRealDir != null) {
            File[] fontFiles = fontRealDir.listFiles();
            for (int i = 0; i < fontFiles.length; i++) {
                File fontFile = fontFiles[i];
                cacheTypeface(fontFile);
            }
        }
        setFontType(fontType);
    }

    private void cacheTypeface(File fontFile) {
        String fileName = fontFile.getName();
        boolean isHit = true;
        String key = null;
        if (fileName.contains(SEMI_BOLD)) {
            key = BOLD;
        } else if (fileName.contains(EXTRA_LIGHT)) {
            key = EXTRA_LIGHT;
        } else if (fileName.contains(HEAVY)) {
            key = HEAVY;
        } else if (fileName.contains(LIGHT)) {
            key = LIGHT;
        } else if (fileName.contains(MEDIUM)) {
            key = MEDIUM;
        } else if (fileName.contains(REGULAR)) {
            key = REGULAR;
        } else if (fileName.contains(BOLD)) {
            key = SEMI_BOLD;
        } else {
            isHit = false;
        }
        if (isHit && !TextUtils.isEmpty(key)) {
            Typeface typeface = Typeface.createFromFile(fontFile);
            serifTypefaces.put(key, typeface);
        }
    }

    private void onFontFailed() {

    }

    /**
     * 字体文件是否存在
     *
     * @return
     */
    private File isExist() {
        String unzipPath = FileUtil.getSDCardAppFilePath(getContext()) + FONT_UNZIP_FOLDER + "/";
        File unzipDir = new File(unzipPath);
        if (unzipDir.exists() && unzipDir.isDirectory()) {
            File[] files = unzipDir.listFiles();
            if (files.length > 0) {
                File fontRealDir = files[0];
                if (fontRealDir.isDirectory()) {
                    File[] fontFiles = fontRealDir.listFiles();
                    if (fontFiles.length > 0) {
                        return fontRealDir;
                    }
                }
            }
        }
        return null;
    }

    public void setFontType(int type) {
        this.fontType = type;
        String fontTypeStr = formatFontType(type);
        // 先去缓存查找
        Typeface cache = serifTypefaces.get(fontTypeStr);
        if (cache != null) {
            setTypeface(cache);
            LogUtil.d("缓存字体生效");
        } else {
            File fontRealDir = isExist();
            if (fontRealDir != null) {
                File[] fontFiles = fontRealDir.listFiles();
                for (int i = 0; i < fontFiles.length; i++) {
                    File fontFile = fontFiles[i];
                    if (fontFile.getName().contains(fontTypeStr)) {
                        Typeface typeface = Typeface.createFromFile(fontFile);
                        setTypeface(typeface);
                        serifTypefaces.put(fontTypeStr, typeface);
                        LogUtil.d("字体生效");
                        break;
                    }
                }
            }
        }
    }

    public String formatFontType(int type) {
        switch (type) {
            case 1:
                return BOLD;
            case 2:
                return EXTRA_LIGHT;
            case 3:
                return HEAVY;
            case 4:
                return LIGHT;
            case 5:
                return MEDIUM;
            case 6:
                return REGULAR;
            case 7:
                return SEMI_BOLD;
            default:
                return REGULAR;
        }
    }
}
