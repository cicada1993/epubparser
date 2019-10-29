package com.chineseall.epubparser.lib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.chineseall.epubparser.lib.BuildConfig;
import com.chineseall.epubparser.lib.util.NetWorkUtil;

/**
 * 带进度条的webview
 */
public class CommonWebView extends WebView {
    private WebViewClientListener mWebViewClientListener;
    private ChromeClientListener mChromeClientListener;

    public CommonWebView(Context context) {
        this(context, null);
    }

    public CommonWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        WebSettings settings = getSettings();
        // 数据缓存
        settings.setAppCacheEnabled(true);
        if (NetWorkUtil.isAvailable(context)) {
            // 有网络时 不用缓存
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            // 没网络时 使用缓存
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        // 存储一些简单的用key/value对即可解决的数据，根据作用范围的不同，有Session Storage和Local Storage两种，
        // 分别用于会话级别的存储（页面关闭即消失）和本地化存储（除非主动
        //删除，否则数据永远不会过期）
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // 允许webview使用File协议
        settings.setAllowFileAccess(true);
        // 允许webview访问系统内容提供者
        settings.setAllowContentAccess(true);
        // 设置是否允许通过 file url 加载的 Js代码读取其他的本地文件
        settings.setAllowFileAccessFromFileURLs(true);
        // 设置是否允许通过 file url 加载的 Javascript 可以访问其他的源(包括http、https等源)
        settings.setAllowUniversalAccessFromFileURLs(true);
        // 允许webview 与 js进行交互
        settings.setJavaScriptEnabled(true);
        // 支持js打开新的窗口
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        //WebView 是否支持多窗口，如果设置为 true，需要重写
        //WebChromeClient#onCreateWindow(WebView, boolean, boolean, Message) 函数，默认为 false
        settings.setSupportMultipleWindows(true);
        // 支持<meta>标签的viewport属性
        settings.setUseWideViewPort(true);
        // 缩放至屏幕大小
        settings.setLoadWithOverviewMode(true);
        // WebView里的字体就不会随系统字体大小设置发生变化
        settings.setTextZoom(100);
        // 默认不支持缩放
        settings.setSupportZoom(false);
        // 不显示缩放工具
        settings.setBuiltInZoomControls(false);
        // 隐藏缩放按钮
        settings.setDisplayZoomControls(false);

        //告诉WebView先不要自动加载图片，等页面finish后再发起图片加载。
        settings.setLoadsImagesAutomatically(true);
        settings.setDefaultTextEncodingName("utf-8");
        // http 和 https 混合请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        setHorizontalScrollBarEnabled(false);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 接受三方cookie
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        }
        setWebChromeClient(loadWebChromeClient());
        setWebViewClient(loadWebViewClient());
    }

    protected WebViewClient loadWebViewClient() {
        return new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (mWebViewClientListener != null) {
                    mWebViewClientListener.onPageFinished(view, url);
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    if (handleCommonUrl(url)) {
                        return true;
                    }
                    if (mWebViewClientListener != null && mWebViewClientListener.shouldOverrideUrlLoading(view, url)) {
                        return true;
                    }
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (handleCommonUrl(request.getUrl().toString())) {
                        return true;
                    }
                    if (mWebViewClientListener != null && mWebViewClientListener.shouldOverrideUrlLoading(view, request)) {
                        return true;
                    }
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (mWebViewClientListener != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    WebResourceResponse response = mWebViewClientListener.shouldInterceptRequest(view, url);
                    if (response != null) {
                        return response;
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (mWebViewClientListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    WebResourceResponse response = mWebViewClientListener.shouldInterceptRequest(view, request);
                    if (response != null) {
                        return response;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
            }
        };
    }

    protected boolean handleCommonUrl(String url) {
        try {
            if (url.startsWith("weixin://") || url.startsWith("alipays://") ||
                    url.startsWith("mailto://") || url.startsWith("tel:")
                //其他自定义的scheme
            ) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                getContext().startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            //防止crash (如果手机上没有安装处理某个scheme开头的url的APP, 会导致crash)
            return false;
        }
        return false;
    }

    protected WebChromeClient loadWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                return super.onJsConfirm(view, url, message, result);
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
            }
        };
    }

    public void setWebViewClientListener(WebViewClientListener listener) {
        mWebViewClientListener = listener;
    }


    public void setChromeClientListener(ChromeClientListener listener) {
        mChromeClientListener = listener;
    }

    public interface WebViewClientListener {
        void onPageFinished(WebView view, String url);

        // 拦截url api 24之前
        boolean shouldOverrideUrlLoading(WebView view, String url);

        // 拦截url api 24之后
        @TargetApi(Build.VERSION_CODES.N)
        boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request);

        // 拦截资源请求 api 21之前
        WebResourceResponse shouldInterceptRequest(WebView view, String url);

        // 拦截资源请求 api 21之后
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request);
    }

    public interface ChromeClientListener {

    }

    public void clearClientListeners() {
        mWebViewClientListener = null;
        mChromeClientListener = null;
    }
}
