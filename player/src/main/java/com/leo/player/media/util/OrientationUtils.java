package com.leo.player.media.util;

import android.app.Activity;
import android.provider.Settings;
import android.view.OrientationEventListener;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;

/**
 * Created on 2017/8/11 下午4:24.
 * leo linxiaotao1993@vip.qq.com
 */

public final class OrientationUtils {


    ///////////////////////////////////////////////////////////////////////////
    // 常量区
    ///////////////////////////////////////////////////////////////////////////

    private final Activity mContent;
    private OrientationEventListener mOrientationEventListener;
    /** 当前屏幕模式 */
    private int mScreenOrientation = SCREEN_ORIENTATION_PORTRAIT;
    /** 是否跟随系统 */
    private boolean mRotateWithSystem = true;
    /** 是否启用 */
    private boolean mEnable = true;
    /** 是否为手动切换 */
    private boolean mManual;
    private Callback mCallback;

    public OrientationUtils(Activity activity) {
        mContent = activity;
        init();
    }

    public OrientationUtils setCallback(Callback callback) {
        mCallback = callback;
        return this;
    }

    public OrientationUtils setEnable(boolean enable) {
        mEnable = enable;
        if (mEnable) {
            mOrientationEventListener.enable();
        } else {
            mOrientationEventListener.disable();
        }
        return this;
    }

    /** 切换屏幕模式 */
    public void toggleScreenOrientation() {
        mManual = true;
        if (mScreenOrientation == SCREEN_ORIENTATION_PORTRAIT) {
            //切换为横屏
            setScreenOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            //切换为竖屏
            setScreenOrientation(SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public void release() {
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
            mOrientationEventListener = null;
        }
    }

    private void init() {

        mScreenOrientation = mContent.getResources().getConfiguration().orientation;

        mOrientationEventListener = new OrientationEventListener(mContent) {
            @Override
            public void onOrientationChanged(int orientation) {

                boolean autoRotateOn = (Settings.System.getInt(mContent.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
                if (!autoRotateOn && mRotateWithSystem) {
                    return;
                }

                if (((orientation >= 0) && (orientation <= 30)) || (orientation >= 330)) {
                    //设置竖屏
                    if (mManual && mScreenOrientation == SCREEN_ORIENTATION_PORTRAIT) {
                        mManual = false;
                        return;
                    } else if (mManual) {
                        return;
                    }
                    if (mScreenOrientation != SCREEN_ORIENTATION_PORTRAIT) {
                        setScreenOrientation(SCREEN_ORIENTATION_PORTRAIT);
                    }
                } else if (((orientation >= 230) && (orientation <= 310))) {
                    //设置横屏
                    if (mManual && mScreenOrientation == SCREEN_ORIENTATION_LANDSCAPE) {
                        mManual = false;
                        return;
                    } else if (mManual) {
                        return;
                    }
                    if (mScreenOrientation != SCREEN_ORIENTATION_LANDSCAPE) {
                        setScreenOrientation(SCREEN_ORIENTATION_LANDSCAPE);
                    }
                } else if (((orientation > 30) && (orientation < 95))) {
                    if (mManual && mScreenOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                        mManual = false;
                        return;
                    } else if (mManual) {
                        return;
                    }
                    if (mScreenOrientation != SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                        setScreenOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    }
                }

            }
        };
        if (mEnable) {
            mOrientationEventListener.enable();
        } else {
            mOrientationEventListener.disable();
        }
    }

    private void setScreenOrientation(int screenOrientation) {
        mScreenOrientation = screenOrientation;
        switch (screenOrientation) {
            case SCREEN_ORIENTATION_LANDSCAPE:
                mContent.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case SCREEN_ORIENTATION_PORTRAIT:
                mContent.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
                break;
            case SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                mContent.setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
        }
        if (mCallback != null) {
            mCallback.screenOrientationChangle(mScreenOrientation);
        }
    }

    public interface Callback {

        void screenOrientationChangle(int screenOrientation);
    }

}
