package com.leo.player.media.util;

import android.app.Activity;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

/**
 * Created on 2017/8/28 下午2:16.
 * leo linxiaotao1993@vip.qq.com
 */

public class CommonUtils {

    private CommonUtils() {
    }

    /** 获取状态栏高度 */
    public static int getStatusHeight(@NonNull Activity context) {
        Rect rect = new Rect();
        context.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }

    /** 隐藏 ActionBar */
    public static boolean hideActionBar(@NonNull Activity context) {
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
            if (actionBar != null && actionBar.isShowing()) {
                actionBar.setShowHideAnimationEnabled(false);
                actionBar.hide();
                return true;
            }
        } else if (context.getActionBar() != null && context.getActionBar().isShowing()) {
            context.getActionBar().hide();
            return true;
        }
        return false;
    }

    /** 显示 ActionBar */
    public static void showActionBar(@NonNull Activity context) {
        if (context.getActionBar() != null) {
            context.getActionBar().show();
        } else if (context instanceof AppCompatActivity && ((AppCompatActivity) context).getSupportActionBar() != null) {
            ((AppCompatActivity) context).getSupportActionBar().setShowHideAnimationEnabled(false);
            ((AppCompatActivity) context).getSupportActionBar().show();
        }
    }

    /** 获取 ActionBar 的高度 */
    public static int getActionBarHeight(@NonNull Activity context) {
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
            if (actionBar != null) {
                return actionBar.getHeight();
            }
        } else if (context.getActionBar() != null) {
            return context.getActionBar().getHeight();
        }
        return 0;
    }


}
