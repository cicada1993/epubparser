package com.chineseall.epubparser.lib.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class ScreenUtil {

    public static int dpToPx(Context context, float dp) {
        DisplayMetrics metrics = getDisplayMetrics(context);
        return (int) (dp * metrics.density + 0.5f * (dp >= 0 ? 1 : -1));
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics metrics = getDisplayMetrics(context);
        return (int) (px / metrics.density);
    }

    public static float spToPx(Context context, float sp) {
        float fontScale = getDisplayMetrics(context).scaledDensity;
        return sp * fontScale + 0.5f;
    }

    public static int pxToSp(Context context, int px) {
        DisplayMetrics metrics = getDisplayMetrics(context);
        return (int) (px / metrics.scaledDensity);
    }

    /**
     * 获取手机显示App区域的大小（头部导航栏+ActionBar+根布局），不包括虚拟按钮
     *
     * @return
     */
    public static int[] getAppSize(Context context) {
        int[] size = new int[2];
        DisplayMetrics metrics = getDisplayMetrics(context);
        size[0] = metrics.widthPixels;
        size[1] = metrics.heightPixels;
        return size;
    }

    /**
     * 获取整个手机屏幕的大小(包括虚拟按钮)
     * 必须在onWindowFocus方法之后使用
     *
     * @param activity
     * @return
     */
    public static int[] getScreenSize(Activity activity) {
        int[] size = new int[2];
        View decorView = activity.getWindow().getDecorView();
        size[0] = decorView.getWidth();
        size[1] = decorView.getHeight();
        return size;
    }

    /**
     * 获取状态栏的高度
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }

    /**
     * 获取虚拟按键的高度
     */
    public static int getNavigationBarHeight(Context context) {
        int navigationBarHeight = 0;
        Resources resources = context.getResources();
        int id = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0 && hasNavigationBar(context)) {
            navigationBarHeight = resources.getDimensionPixelSize(id);
        }
        return navigationBarHeight;
    }

    /**
     * 是否存在虚拟按键
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    private static boolean hasNavigationBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            @SuppressLint("PrivateApi") Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception ignored) {
        }
        return hasNavigationBar;
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        display.getMetrics(dm);
        return dm;
    }
}
