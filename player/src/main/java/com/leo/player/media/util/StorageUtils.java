package com.leo.player.media.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.danikula.videocache.file.Md5FileNameGenerator;

import java.io.File;


public class StorageUtils {

    private static final String INDIVIDUAL_DIR_NAME = "video-cache";

    private static final Md5FileNameGenerator FILE_NAME_GENERATOR = new Md5FileNameGenerator();

    /**
     * 获取缓存目录
     * 优先使用外部存储
     *
     * @param context 上下文
     * @return 缓存目录
     */
    @Nullable
    public static File getCacheDirectory(Context context) {

        //检查是否有外部存储权限
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);

        if (isExternalStorageWritable() && hasPermission) {
            //删除可能存在内部存储的缓存
            delFile(new File(context.getCacheDir(),INDIVIDUAL_DIR_NAME));
            File movies = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            return new File(movies, INDIVIDUAL_DIR_NAME);
        } else {
            return new File(context.getCacheDir(), INDIVIDUAL_DIR_NAME);
        }
    }


    /**
     * 获取外部存储是否可用
     *
     * @return 是否可用
     */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * 删除指定的 url 缓存中文件
     *
     * @param context
     * @param cacheDir
     * @param url
     */
    public static void delSpecifyUrlCache(Context context, File cacheDir, String url) {
        String cacheFileName = FILE_NAME_GENERATOR.generate(url) + ".download";
        File specifyCacheDir = new File(cacheDir, cacheFileName);
        delFile(specifyCacheDir);
    }

    public static boolean delFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File childFile : file.listFiles()) {
                    delFile(childFile);
                }
                return file.delete();
            } else {
                return file.delete();
            }
        } else {
            return false;
        }
    }

}
