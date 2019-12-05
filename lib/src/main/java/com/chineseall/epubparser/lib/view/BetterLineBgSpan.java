package com.chineseall.epubparser.lib.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.text.style.LineBackgroundSpan;

import androidx.annotation.NonNull;

import com.chineseall.epubparser.lib.render.Page;

import kotlin.Pair;

public class BetterLineBgSpan implements LineBackgroundSpan {
    private Page page;

    public BetterLineBgSpan(Page page) {
        this.page = page;
    }

    @Override
    public void drawBackground(@NonNull Canvas canvas, @NonNull Paint paint, int left, int right, int top, int baseline, int bottom, @NonNull CharSequence text, int start, int end, int lineNumber) {
        Pair<Boolean, BetterImageSpan> imageLineCheck = page.isImageLine(lineNumber);
        Rect rect = new Rect();
        if (imageLineCheck.getFirst()) {
            BetterImageSpan imageSpan = imageLineCheck.getSecond();
            Rect bounds = imageSpan.getBounds();
            rect.left = bounds.left;
            rect.top = top;
            rect.right = bounds.right;
            rect.bottom = top + (bounds.bottom - bounds.top);
        } else {
            rect.left = (int) page.getLineLeft(lineNumber);
            rect.top = top;
            rect.right = (int) page.getLineWidth(lineNumber);
            rect.bottom = (int) (baseline + paint.getFontMetrics().descent);
        }
        page.appendLineRec(lineNumber, rect);
//        final int paintColor = paint.getColor();
//        paint.setColor(Color.parseColor("#afe1f4"));
//        canvas.drawRect(rect, paint);
//        paint.setColor(paintColor);
    }
}
