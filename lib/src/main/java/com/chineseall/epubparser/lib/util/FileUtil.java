package com.chineseall.epubparser.lib.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okio.BufferedSink;
import okio.Okio;

public class FileUtil {
    /**
     * 是否存在外部存储卡
     *
     * @return
     */
    public static boolean isSDCardAvailable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return true;
        } else {
            return false;
        }
    }

    public static String getSDCardRootPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getMemoryAppDataPath(Context context) {
        // data/data/包名/
        return context.getFilesDir().getParentFile().getAbsolutePath();
    }

    public static String getMemoryDataPath() {
        return Environment.getDataDirectory().getAbsolutePath();
    }

    /**
     * 获取应用缓存目录
     *
     * @return
     */
    public static String getSDCardAppFilePath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }

    /**
     * 获取图片
     */
    public static String getImagePath(Context context, Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    String id = docId.split(":")[1];
                    String selection = MediaStore.Images.Media._ID + "=" + id;
                    imagePath = getImagePath(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    imagePath = getImagePath(context, contentUri, null);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                imagePath = getImagePath(context, uri, null);
            }
        } else {
            imagePath = getImagePath(context, uri, null);
        }
        return imagePath;

    }

    /**
     * 通过uri和selection来获取真实的图片路径,从相册获取图片时要用
     */
    public static String getImagePath(Context context, Uri uri, String selection) {
        String path = null;
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (columnIndex > -1) {
                    path = cursor.getString(columnIndex);
                }
            }
            cursor.close();
        }
        return path;
    }

    private static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
            file.delete();//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 解压文件到指定目录
     *
     * @param filePath
     * @param destPath
     */
    public static boolean unzip(final String filePath, final String destPath) {
        File destFolder = new File(destPath);
        if (destFolder.exists()) {
            deleteFile(destFolder);
        }
        destFolder.mkdirs();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long start = System.currentTimeMillis();
            Path zipFile = FileSystems.getDefault().getPath(filePath);
            Path decryptTo = FileSystems.getDefault().getPath(destPath);
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    final Path toPath = decryptTo.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectory(toPath);
                    } else {
                        Files.copy(zipInputStream, toPath);
                    }
                }
                zipInputStream.closeEntry();
                long end = System.currentTimeMillis();
                LogUtil.d("耗时：" + (end - start));
                return true;
            } catch (IOException e) {
                LogUtil.d(e.getMessage());
            }
        } else {
            long start = System.currentTimeMillis();
            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filePath))) {
                ZipEntry entry;
                byte[] buffer = new byte[1024];
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        File folder = new File(destFolder, entry.getName());
                        folder.mkdirs();
                    } else {
                        File newFile = new File(destFolder, entry.getName());
                        BufferedSink sink = Okio.buffer(Okio.sink(newFile));
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            sink.write(buffer, 0, len);
                        }
                        sink.close();
                    }
                }
                long end = System.currentTimeMillis();
                LogUtil.d("耗时：" + (end - start));
                return true;
            } catch (IOException e) {
                LogUtil.d(e.getMessage());
            }
        }
        return false;
    }
}
