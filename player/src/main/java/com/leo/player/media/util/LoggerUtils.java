package com.leo.player.media.util;

import android.util.Log;

/**
 * 简单的 log 打印
 * Created on 2017/8/22 上午9:45.
 * leo linxiaotao1993@vip.qq.com
 */

public final class LoggerUtils {

    private static final String TAG = "Player";

    private static boolean sLog = true;

    public static boolean isLog() {
        return sLog;
    }

    public static void setLog(boolean log) {
        sLog = log;
    }

    public static void debugLog(String tag, String message) {
        if (!sLog) {
            return;
        }
        Log.d(tag, message);
    }

    public static void errorLog(String tag, String message) {
        if (!sLog) {
            return;
        }

        Log.e(tag, message);
    }

    public static void debugLog(String message) {
        debugLog(TAG, message);
    }

    public static void errorLog(String message) {
        errorLog(TAG, message);
    }

}
