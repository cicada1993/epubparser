/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.chineseall.epubparser.lib.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.util.Pools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * This class contains utility method for Bitmap
 */
public final class BitmapUtil {
    private static final int DECODE_BUFFER_SIZE = 16 * 1024;
    private static final int POOL_SIZE = 12;
    private static final Pools.SynchronizedPool<ByteBuffer> DECODE_BUFFERS =
            new Pools.SynchronizedPool<>(POOL_SIZE);

    /**
     * Bytes per pixel definitions
     */
    public static final int ALPHA_8_BYTES_PER_PIXEL = 1;
    public static final int ARGB_4444_BYTES_PER_PIXEL = 2;
    public static final int ARGB_8888_BYTES_PER_PIXEL = 4;
    public static final int RGB_565_BYTES_PER_PIXEL = 2;
    public static final int RGBA_F16_BYTES_PER_PIXEL = 8;

    public static final float MAX_BITMAP_SIZE = 2048f;

    /**
     * @return size in bytes of the underlying bitmap
     */
    @SuppressLint("NewApi")
    public static int getSizeInBytes(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }

        // There's a known issue in KitKat where getAllocationByteCount() can throw an NPE. This was
        // apparently fixed in MR1: http://bit.ly/1IvdRpd. So we do a version check here, and
        // catch any potential NPEs just to be safe.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            try {
                return bitmap.getAllocationByteCount();
            } catch (NullPointerException npe) {
                // Swallow exception and try fallbacks.
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }

        // Estimate for earlier platforms. Same code as getByteCount() for Honeycomb.
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * Decodes only the bounds of an image and returns its width and height or null if the size can't
     * be determined
     *
     * @param bytes the input byte array of the image
     * @return dimensions of the image
     */
    public static @Nullable
    Pair<Integer, Integer> decodeDimensions(byte[] bytes) {
        // wrapping with ByteArrayInputStream is cheap and we don't have duplicate implementation
        return decodeDimensions(new ByteArrayInputStream(bytes));
    }

    /**
     * Decodes the bounds of an image from its Uri and returns a pair of the dimensions
     *
     * @param uri the Uri of the image
     * @return dimensions of the image
     */
    public static @Nullable
    Pair<Integer, Integer> decodeDimensions(Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri.getPath(), options);
        return (options.outWidth == -1 || options.outHeight == -1)
                ? null
                : new Pair<>(options.outWidth, options.outHeight);
    }

    /**
     * Decodes the bounds of an image and returns its width and height or null if the size can't be
     * determined
     *
     * @param is the InputStream containing the image data
     * @return dimensions of the image
     */
    public static @Nullable
    Pair<Integer, Integer> decodeDimensions(InputStream is) {
        ByteBuffer byteBuffer = DECODE_BUFFERS.acquire();
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocate(DECODE_BUFFER_SIZE);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            options.inTempStorage = byteBuffer.array();
            BitmapFactory.decodeStream(is, null, options);

            return (options.outWidth == -1 || options.outHeight == -1)
                    ? null
                    : new Pair<>(options.outWidth, options.outHeight);
        } finally {
            DECODE_BUFFERS.release(byteBuffer);
        }
    }

    /**
     * Decodes the bounds of an image and returns its width and height or null if the size can't be
     * determined. It also recovers the color space of the image, or null if it can't be determined.
     *
     * @param is the InputStream containing the image data
     * @return the metadata of the image
     */
    public static ImageMetaData decodeDimensionsAndColorSpace(InputStream is) {
        ByteBuffer byteBuffer = DECODE_BUFFERS.acquire();
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocate(DECODE_BUFFER_SIZE);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            options.inTempStorage = byteBuffer.array();
            BitmapFactory.decodeStream(is, null, options);

            ColorSpace colorSpace = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                colorSpace = options.outColorSpace;
            }
            return new ImageMetaData(options.outWidth, options.outHeight, colorSpace);
        } finally {
            DECODE_BUFFERS.release(byteBuffer);
        }
    }

    /**
     * Returns the amount of bytes used by a pixel in a specific
     * {@link android.graphics.Bitmap.Config}
     *
     * @param bitmapConfig the {@link android.graphics.Bitmap.Config} for which the size in byte
     *                     will be returned
     * @return
     */
    public static int getPixelSizeForBitmapConfig(Bitmap.Config bitmapConfig) {

        switch (bitmapConfig) {
            case ARGB_8888:
                return ARGB_8888_BYTES_PER_PIXEL;
            case ALPHA_8:
                return ALPHA_8_BYTES_PER_PIXEL;
            case ARGB_4444:
                return ARGB_4444_BYTES_PER_PIXEL;
            case RGB_565:
                return RGB_565_BYTES_PER_PIXEL;
            case RGBA_F16:
                return RGBA_F16_BYTES_PER_PIXEL;
        }
        throw new UnsupportedOperationException("The provided Bitmap.Config is not supported");
    }

    /**
     * Returns the size in byte of an image with specific size
     * and {@link android.graphics.Bitmap.Config}
     *
     * @param width        the width of the image
     * @param height       the height of the image
     * @param bitmapConfig the {@link android.graphics.Bitmap.Config} for which the size in byte
     *                     will be returned
     * @return
     */
    public static int getSizeInByteForBitmap(int width, int height, Bitmap.Config bitmapConfig) {
        return width * height * getPixelSizeForBitmapConfig(bitmapConfig);
    }

    /**
     *
     * @param context
     * @param base64Data
     * @return
     */
    public static Drawable drawableFromBase64Data(Context context, String base64Data) {
        Drawable drawable = null;
        ByteArrayInputStream bis = null;
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            bis = new ByteArrayInputStream(bytes);
            drawable = Drawable.createFromResourceStream(context.getResources(),
                    null, bis, null);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        } finally {
            Closeables.closeQuietly(bis);
        }
        return drawable;
    }

    /**
     * @param context
     * @param uri
     * @return
     */
    public static Drawable drawableFromBase64Uri(Context context, Uri uri) {
        Drawable drawable = null;
        ByteArrayInputStream bis = null;
        try {
            String imgSrc = uri.toString();
            int commaPos = imgSrc.indexOf(',');
            String dataStr = imgSrc.substring(commaPos + 1);
            byte[] bytes = Base64.decode(dataStr, Base64.DEFAULT);
            bis = new ByteArrayInputStream(bytes);
            drawable = Drawable.createFromResourceStream(context.getResources(),
                    null, bis, null);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        } finally {
            Closeables.closeQuietly(bis);
        }
        return drawable;
    }

    /**
     * @param base64Data
     * @return
     */
    public static ImageMetaData decodeFromBase64Data(String base64Data) {
        ImageMetaData imageMetaData = null;
        ByteArrayInputStream bis = null;
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            bis = new ByteArrayInputStream(bytes);
            imageMetaData = decodeDimensionsAndColorSpace(bis);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        } finally {
            Closeables.closeQuietly(bis);
        }
        return imageMetaData;
    }

    /**
     * @param uri
     * @return
     */
    public static ImageMetaData decodeFromBase64Uri(Uri uri) {
        ImageMetaData imageMetaData = null;
        ByteArrayInputStream bis = null;
        try {
            String imgSrc = uri.toString();
            int commaPos = imgSrc.indexOf(',');
            String dataStr = imgSrc.substring(commaPos + 1);
            byte[] bytes = Base64.decode(dataStr, Base64.DEFAULT);
            bis = new ByteArrayInputStream(bytes);
            imageMetaData = decodeDimensionsAndColorSpace(bis);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        } finally {
            Closeables.closeQuietly(bis);
        }
        return imageMetaData;
    }

    /**
     * @param w
     * @param h
     * @param maxW
     * @param maxH
     * @return
     */
    public static int caculateInSample(int w, int h, int maxW, int maxH) {
        int inSampleSize = 1;
        if (w > maxW && h > maxH) {
            inSampleSize = 2;
            while (w / inSampleSize > maxW && h / inSampleSize > maxH) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
