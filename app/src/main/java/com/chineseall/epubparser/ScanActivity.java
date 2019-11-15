package com.chineseall.epubparser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.king.zxing.CaptureActivity;
import com.king.zxing.DecodeFormatManager;
import com.king.zxing.camera.CameraConfigurationUtils;
import com.king.zxing.camera.open.OpenCamera;

/**
 * 扫码界面
 */
public class ScanActivity extends CaptureActivity implements View.OnClickListener {
    private ImageView ivFlash;

    public static void start(Context context) {
        Intent intent = new Intent(context, ScanActivity.class);
        context.startActivity(intent);
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_scan;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ivFlash = findViewById(R.id.ivFlash);
        if (!hasTorch()) {
            ivFlash.setVisibility(View.GONE);
        }
        ivFlash.setOnClickListener(this);
        getCaptureHelper().playBeep(false)//播放音效
                .vibrate(true)//震动
                .decodeFormats(DecodeFormatManager.QR_CODE_FORMATS);
    }

    @Override
    public boolean onResultCallback(String result) {
        Intent intent = new Intent();
        intent.putExtra("result", result);
        setResult(Activity.RESULT_OK, intent);
        finish();
        ScanResultBus.handleResult(result);
        return true;
    }

    private void clickFlash(View v) {
        boolean isSelected = v.isSelected();
        if (setTorch(!isSelected)) {
            v.setSelected(!isSelected);
        }
    }

    /**
     * 开启或关闭闪光灯（手电筒）
     *
     * @param on {@code true}表示开启，{@code false}表示关闭
     */
    public boolean setTorch(boolean on) {
        OpenCamera openCamera = getCameraManager().getOpenCamera();
        if (openCamera != null) {
            Camera camera = openCamera.getCamera();
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                CameraConfigurationUtils.setTorch(parameters, on);
                camera.setParameters(parameters);
                return true;
            }
        }
        return false;
    }

    /**
     * 检测是否支持闪光灯（手电筒）
     *
     * @return
     */
    public boolean hasTorch() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivFlash) {
            clickFlash(v);
        }
    }
}
