package com.leo.player.media.controller;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.AutoTransition;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.player.R;
import com.leo.player.media.IjkVideoManager;
import com.leo.player.media.util.CommonUtils;
import com.leo.player.media.util.LoggerUtils;
import com.leo.player.media.util.NetworkUtils;
import com.leo.player.media.util.OrientationUtils;
import com.leo.player.media.util.SimpleTransitionListener;
import com.leo.player.media.videoview.IVideoView;
import com.leo.player.media.videoview.IjkVideoView;
import com.leo.player.media.weiget.ENDownloadView;
import com.leo.player.media.weiget.ENPlayView;

import java.util.Formatter;
import java.util.Locale;

import static com.leo.player.R.id.fullscreen;
import static com.leo.player.R.id.total;


/**
 * 默认 媒体播放控制器
 * Created on 2017/8/10 上午10:46.
 * leo linxiaotao1993@vip.qq.com
 */

public class MediaController extends FrameLayout implements IMediaController, OrientationUtils.Callback {

    private static final String TAG = "MediaController";
    private static final String TRANSITION_NAME_IJKVIEW = "transition_name_ijkview";


    ///////////////////////////////////////////////////////////////////////////
    // 可配置
    ///////////////////////////////////////////////////////////////////////////

    /** 是否显示底部进度条 */
    private boolean mShowBottomProgress = true;
    /** 是否显示控制布局 */
    private boolean mShowBottomLayout = true;
    /** 是否显示封面图 */
    private boolean mShowThumb = true;
    /** 兼容列表播放，记录当前播放序号 */
    private int mPlayPosition = -1;
    /** 是否启用屏幕感应 */
    private boolean mEnableOrientation = true;
    /** 播放器点击事件 */
    private int mFullScreenMode = FULLSCREEN_ORIENTATION;
    private OnFullScreenChangeListener mFullScreenChangeListener;
    /** 是否静音 */
    private boolean mMute;
    /** 全屏是否静音 */
    private boolean mFullScreenMute;
    /** 是否准备好就播放 */
    private boolean mPreparedPlay;
    /** 是否检查当前是否为 Wifi */
    private boolean mCheckWifi = true;
    /** 是否开启左边往上滑动改变亮度 */
    private boolean mEnableSlideBrightness = true;
    /** 是否开启右边往上滑动改变音量 */
    private boolean mEnableSliderVolume = true;
    /** 是否开启滑动改变播放进度 */
    private boolean mEnableSlidePosition = true;
    /** 全屏视图模式下是否开启右边往上滑动改变音量 */
    private boolean mFullScreenViewEnableSlideBrightness = true;
    /** 全屏视图模式下是否开启右边往上滑动改变音量 */
    private boolean mFullScreenViewEnableSliderVolume = true;
    /** 全屏视图模式下是否开启滑动改变播放进度 */
    private boolean mFullScreenViewEnableSlidePosition = true;
    /** 是否需要添加全屏视图动画 */
    private boolean mFullScreenViewAnim = true;

    ///////////////////////////////////////////////////////////////////////////
    // 常量区
    ///////////////////////////////////////////////////////////////////////////

    /** 自动隐藏 */
    private static final int FADE_OUT = 0;
    /** 显示进度值 */
    private static final int SHOW_PROGRESS = 1;
    /** 处理状态 UI */
    private static final int HANDLE_STATE_UI = 2;
    /** 自动隐藏 UI */
    private static final int HIDE_LAYOUT = 3;
    /** 自动显示 UI */
    private static final int SHOW_LAYOUT = 4;
    /** 更新封面图 */
    private static final int UPDATE_THUMB = 5;
    /** 进度 Dialog */
    private static final int UPDATE_POSITION_DIALOG = 6;
    /** 亮度 Dialog */
    private static final int UPDATE_BRIGHTNESS_DIALOG = 7;
    /** 音量 Dialog */
    private static final int UPDATE_VOLUME_DIALOG = 8;


    /** 通过旋转屏幕来全屏 */
    public static final int FULLSCREEN_ORIENTATION = 0;
    /** 通过新增视图来全屏 */
    public static final int FULLSCREEN_VIEW = 1;

    /** 默认显示超时时间 */
    private static final int DEFALUT_TIMEOUT = 3 * 1000;

    ///////////////////////////////////////////////////////////////////////////
    // 控件
    ///////////////////////////////////////////////////////////////////////////

    private ENDownloadView mLoadingView;
    private ENPlayView mBtnPlay;
    private LinearLayout mLayoutBottom;
    private TextView mTextCurrent;
    private SeekBar mSeekProgress;
    private TextView mTextTotal;
    private ImageView mBtnFullscreen;
    private ProgressBar mBottomProgressbar;
    private ImageView mImgThumb;
    //position dialog
    private View mPositionDialogContentView;
    private Dialog mPositionDialog;
    private ProgressBar mDialogProgress;
    private TextView mDialogCurrent;
    private TextView mDialogTotal;
    //brightness dialog
    private View mBrightnessContentView;
    private Dialog mBrightnessDialog;
    private TextView mDialogBrightness;
    //volume dialog
    private View mVolumeContentView;
    private Dialog mVolumeDialog;
    private ProgressBar mDialogVolume;
    private ImageView mDialogVolumeLogo;


    ///////////////////////////////////////////////////////////////////////////
    // 内部变量
    ///////////////////////////////////////////////////////////////////////////

    private OrientationUtils mOrientationUtils;
    private IVideoView mVideoView;
    private Activity mContext;
    /** 当前播放 Uri */
    private Uri mCurrentUri;
    /** 记录暂停时的播放进度 */
    private int mOldPlayPosition;
    /** 当前是否为全屏显示 */
    private boolean mFullscreen;
    /** 是否显示播放按钮 */
    private boolean mShowPlay = true;
    /** 是否在拖动进度值 */
    private boolean mDragging;
    /** 是否启用 */
    private boolean mEnabled;
    /** 是否显示 */
    private boolean mShowing;
    /** 用于恢复全屏时使用 */
    private int mSystemUiVisibility = -1;
    /** 使用 fullscreen view 之前是否为全屏 */
    private boolean mOriginalFullScreen;
    /** 使用 fullscreen view 之前是否显示 actionbar */
    private boolean mOriginalActionBar;
    /** 全屏显示的控件 */
    private View mFullScreenView;
    private GestureDetectorCompat mGestureDetectorCompat;
    private int mCurrentState;
    private AlertDialog mInfoDialog;
    /** 屏幕宽度 */
    private int mScreenWidth;
    /** 屏幕高度 */
    private int mScreenHeight;

    private float mSlideDamping = 2f;

    /** 上一次滑动的进度 */
    private int mPreSlidePosition = -1;
    /** 是否正在滑动播放进度 */
    private boolean mSlidePosition;

    /** 上一次滑动的亮度 */
    private float mPreSlideBrightness = -1;
    /** 是否正在滑动亮度 */
    private boolean mSlideBrightness;

    /** 上一次滑动的音量 */
    private int mPreSlideVolume = -1;
    private AudioManager mAudioManager;
    /** 是否正在滑动音量 */
    private boolean mSlideVolume;

    /** 状态栏高度 */
    private int mStatusHeight;

    /** 当前 window location */
    private int[] mLocations = new int[2];

    private static final String NOT_READY_INFO = "视频还没准备好呢";

    public MediaController(Context context) {
        this(context, null);

    }

    public MediaController(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public int getPlayPosition() {
        return mPlayPosition;
    }

    public MediaController setPlayPosition(int playPosition) {
        mPlayPosition = playPosition;
        return this;
    }


    public MediaController setShowBottomProgress(boolean showBottomProgress) {
        mShowBottomProgress = showBottomProgress;
        return this;
    }

    public MediaController setShowBottomLayout(boolean showBottomLayout) {
        mShowBottomLayout = showBottomLayout;
        return this;
    }

    public MediaController setShowThumb(boolean showThumb) {
        mShowThumb = showThumb;
        mMainHandler.sendEmptyMessage(UPDATE_THUMB);
        return this;
    }

    @Nullable
    public ImageView getImgThumb() {
        return mImgThumb;
    }

    public MediaController setEnableOrientation(boolean enableOrientation) {
        mEnableOrientation = enableOrientation;
        if (mOrientationUtils != null) {
            mOrientationUtils.setEnable(mEnableOrientation);
        }
        return this;
    }

    public MediaController setFullScreenMode(int fullScreenMode) {
        mFullScreenMode = fullScreenMode;
        return this;
    }


    public MediaController setFullScreenChangeListener(OnFullScreenChangeListener fullScreenChangeListener) {
        mFullScreenChangeListener = fullScreenChangeListener;
        return this;
    }

    public boolean isFullscreen() {
        return mFullscreen;
    }

    public MediaController setMute(boolean mute) {
        mMute = mute;
        IjkVideoManager.getInstance().setMute(mute);
        return this;
    }

    public MediaController setFullScreenMute(boolean fullScreenMute) {
        mFullScreenMute = fullScreenMute;
        return this;
    }

    public MediaController setPreparedPlay(boolean preparedPlay) {
        mPreparedPlay = preparedPlay;
        return this;
    }

    public MediaController setCheckWifi(boolean checkWifi) {
        mCheckWifi = checkWifi;
        return this;
    }

    public MediaController setEnableSlideBrightness(boolean enableSlideBrightness) {
        mEnableSlideBrightness = enableSlideBrightness;
        return this;
    }

    public MediaController setEnableSliderVolume(boolean enableSliderVolume) {
        mEnableSliderVolume = enableSliderVolume;
        return this;
    }

    public MediaController setEnableSlidePosition(boolean enableSlidePosition) {
        mEnableSlidePosition = enableSlidePosition;
        return this;
    }

    public MediaController setFullScreenViewEnableSlideBrightness(boolean fullScreenViewEnableSlideBrightness) {
        mFullScreenViewEnableSlideBrightness = fullScreenViewEnableSlideBrightness;
        return this;
    }

    public MediaController setFullScreenViewEnableSliderVolume(boolean fullScreenViewEnableSliderVolume) {
        mFullScreenViewEnableSliderVolume = fullScreenViewEnableSliderVolume;
        return this;
    }

    public MediaController setFullScreenViewEnableSlidePosition(boolean fullScreenViewEnableSlidePosition) {
        mFullScreenViewEnableSlidePosition = fullScreenViewEnableSlidePosition;
        return this;
    }

    public MediaController setFullScreenViewAnim(boolean fullScreenViewAnim) {
        mFullScreenViewAnim = fullScreenViewAnim;
        return this;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;

        if (mEnabled && isKeyCodeSupported) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (IjkVideoManager.getInstance().isPlaying()) {
                    IjkVideoManager.getInstance().pause();
                    show();
                } else {
                    IjkVideoManager.getInstance().start();
                    hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!IjkVideoManager.getInstance().isPlaying()) {
                    IjkVideoManager.getInstance().start();
                    hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (IjkVideoManager.getInstance().isPlaying()) {
                    IjkVideoManager.getInstance().pause();
                    show();
                }
                return true;
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mGestureDetectorCompat.onTouchEvent(ev);
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {

            if (getParent() instanceof ViewGroup) {
                ViewGroup.class.cast(getParent()).requestDisallowInterceptTouchEvent(false);
            }
            handleSlidePosition();

            //取消音量 Dialog
            handleVoluemDialog();

            //取消亮度 Dialog
            handleBrightnessDialog();


        }
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (mEnabled) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }


    private void hide() {
        mShowing = false;
        mMainHandler.sendEmptyMessage(HIDE_LAYOUT);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (mEnabled) {
            //当 player prepared
            mCurrentUri = mVideoView.getCurrentUri();
            IjkVideoManager.getInstance().setMute(mMute);
            IjkVideoManager.getInstance().setStateChangeListener(this);
            debug("当前为启动状态");
            if (mPreparedPlay) {
                mPreparedPlay = false;
                playMedia();
            }
        }
    }


    public void show() {
        show(DEFALUT_TIMEOUT);
    }


    public void show(int timeout) {

        mMainHandler.sendEmptyMessage(SHOW_LAYOUT);


        if (!mShowing) {
            mShowing = true;
        }

        mMainHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout > 0) {
            mMainHandler.removeMessages(FADE_OUT);
            Message message = mMainHandler.obtainMessage(FADE_OUT);
            mMainHandler.sendMessageDelayed(message, timeout);
        }
    }

    @Override
    public View makeControllerView() {
        if (getChildCount() > 0) {
            removeAllViews();
        }
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View contentView = layoutInflater.inflate(R.layout.media_controller, this, true);
        initControllerView(contentView);
        return this;
    }

    @Override
    public void setVideoView(IVideoView videoView) {
        mVideoView = videoView;
    }

    @Override
    public void notifyPlayState(int state) {
        mCurrentState = state;
        Message message = mMainHandler.obtainMessage(HANDLE_STATE_UI, state);
        mMainHandler.sendMessage(message);
    }


    private void handleStateUI(int state) {

        mShowPlay = false;

        switch (state) {
            case IjkVideoManager.STATE_IDLE://闲置中
                release();
                break;
            case IjkVideoManager.STATE_BUFFERING_START://开始缓冲
            case IjkVideoManager.STATE_PREPARING://准备中
                if (mImgThumb != null) {
                    mImgThumb.setVisibility(GONE);
                }
                if (mBtnPlay != null) {
                    mBtnPlay.setVisibility(GONE);
                }
                if (mLoadingView != null) {
                    mLoadingView.setVisibility(VISIBLE);
                    mLoadingView.start();
                }
                break;
            case IjkVideoManager.STATE_PREPARED://准备好
                if (mLoadingView != null) {
                    mLoadingView.reset();
                    mLoadingView.setVisibility(GONE);
                }
                show();
                break;
            case IjkVideoManager.STATE_PLAYING://播放中
                if (mLoadingView != null) {
                    mLoadingView.reset();
                    mLoadingView.setVisibility(GONE);
                }
                if (mBtnPlay != null) {
                    mBtnPlay.setVisibility(VISIBLE);
                    mBtnPlay.play();
                }
                show();
                break;
            case IjkVideoManager.STATE_PAUSED://暂停中
                if (mLoadingView != null) {
                    mLoadingView.reset();
                    mLoadingView.setVisibility(GONE);
                }
                if (mBtnPlay != null) {
                    mBtnPlay.setVisibility(VISIBLE);
                    mBtnPlay.pause();
                }
                show();
                break;
            case IjkVideoManager.STATE_PLAYBACK_COMPLETED://播放完成
                if (mLoadingView != null) {
                    mLoadingView.reset();
                    mLoadingView.setVisibility(GONE);
                }
                if (mBtnPlay != null) {
                    mBtnPlay.setVisibility(VISIBLE);
                    mBtnPlay.pause();
                }
                show();
                break;
            case IjkVideoManager.STATE_ERROR://错误
                if (mLoadingView != null) {
                    mLoadingView.reset();
                    mLoadingView.setVisibility(GONE);
                }
                mShowPlay = true;
                if (mBtnPlay != null) {
                    mBtnPlay.pause();
                    mBtnPlay.setVisibility(VISIBLE);
                }
                show();
                showInfo("播放出错啦");
                break;
        }
    }

    @Override
    public void screenOrientationChangle(int screenOrientation) {
        if (!isActive() || mFullScreenMode != FULLSCREEN_ORIENTATION) {
            return;
        }

        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;

        debug("选择屏幕后，当前屏幕尺寸：" + mScreenWidth + "，" + mScreenHeight);

        switch (screenOrientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT://竖屏
                mFullscreen = false;
                mBtnFullscreen.setImageResource(R.drawable.video_enlarge);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE://横屏
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE://反向横屏
                mFullscreen = true;
                mBtnFullscreen.setImageResource(R.drawable.video_shrink);
                setFocusableInTouchMode(true);
                requestFocus();
                break;
        }

        if (mFullScreenChangeListener != null) {
            mFullScreenChangeListener.onFullScreenChange(mFullscreen);
        }

        show();
    }

    private void release() {
        mEnabled = false;
        mShowing = false;
        mShowPlay = true;
        if (mBtnPlay != null) {
            mBtnPlay.setVisibility(VISIBLE);
            mBtnPlay.pause();
        }
        if (mLoadingView != null) {
            mLoadingView.reset();
            mLoadingView.setVisibility(GONE);
        }
        if (mInfoDialog != null) {
            mInfoDialog.dismiss();
            mInfoDialog = null;
        }
        hide();
        updateThumb();
        if (mToast != null) {
            mToast.cancel();
        }
        if (mPositionDialog != null) {
            mPositionDialog.dismiss();
            mPositionDialog = null;
        }
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
            mBrightnessDialog = null;
        }
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
            mVolumeDialog = null;
        }
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
        }
        if (mContext.getWindow() != null) {
            //恢复亮度
            WindowManager.LayoutParams layoutParams = mContext.getWindow().getAttributes();
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            mContext.getWindow().setAttributes(layoutParams);
        }
    }

    public void toggleScreenOrientation() {
        if (mFullScreenMode == FULLSCREEN_ORIENTATION) {
            //当前为旋转屏幕的全屏模式
            mOrientationUtils.toggleScreenOrientation();
        }
    }

    public void toggleFullScreen() {
        toggleScreenOrientation();
        toggleFullScreenView();
    }

    public void toggleFullScreenView() {
        if (mFullScreenMode == FULLSCREEN_VIEW) {
            if (mFullscreen) {
                removeFullScreenView();
            } else {
                addFullScreenView();
            }
        }
    }

    private void init(Context context) {


        mContext = Activity.class.cast(context);

        //获取状态栏高度
        mStatusHeight = CommonUtils.getStatusHeight(mContext);
        debug("当前状态高度：" + mStatusHeight);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;

        debug("当前屏幕尺寸：" + mScreenWidth + "，" + mScreenHeight);


        mOrientationUtils = new OrientationUtils(mContext);
        mOrientationUtils.setCallback(this);
        mOrientationUtils.setEnable(mEnableOrientation);

        setBackgroundColor(Color.TRANSPARENT);
        setClickable(true);

        setOnKeyListener(mOrientationKeyListener);

        mGestureDetectorCompat = new GestureDetectorCompat(mContext, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                if (mEnabled) {

                    if (mEnableSlidePosition) {
                        mPreSlidePosition = IjkVideoManager.getInstance().getCurrentPosition();
                        debug("按下时，当前播放进度：" + stringForTime(mPreSlidePosition));
                    }
                    if (mEnableSliderVolume) {
                        mPreSlideVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        debug("按下时，当前的音量：" + mPreSlideVolume);
                    }
                    if (mEnableSlideBrightness) {
                        mPreSlideBrightness = mContext.getWindow().getAttributes().screenBrightness;
                        if (mPreSlideBrightness < 0) {
                            mPreSlideBrightness = 0.5f;
                        }
                        debug("按下时，当前的亮度：" + mPreSlideBrightness);
                    }


                }
                return mEnabled;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!mEnabled) {
                    return false;
                }
                toggleMediaControlsVisiblity();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!mEnabled) {
                    return false;
                }
                if (IjkVideoManager.getInstance().isPlaying()) {
                    pauseMedia();
                } else {
                    playMedia();
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!mEnabled) {
                    return false;
                }

                //判断是否点击在状态栏范围
                if (mStatusHeight > 0 && e1.getY() <= mStatusHeight) {
                    return false;
                }


                if (Math.abs(distanceX) > Math.abs(distanceY) && mEnableSlidePosition && !(mSlideBrightness || mSlideVolume)) {
                    //滑动改变播放进度
                    distanceX = -distanceX;
                    int total = IjkVideoManager.getInstance().getDuration();
                    mPreSlidePosition += distanceX * total / getWidth();
                    if (mPreSlidePosition < 0) {
                        mPreSlidePosition = 0;
                    }
                    if (mPreSlidePosition > total) {
                        mPreSlidePosition = total;
                    }
                    mSlidePosition = true;
                    Message message = mMainHandler.obtainMessage(UPDATE_POSITION_DIALOG, mPreSlidePosition);
                    mMainHandler.sendMessage(message);

                    if (getParent() instanceof ViewGroup) {
                        ViewGroup.class.cast(getParent()).requestDisallowInterceptTouchEvent(true);
                    }

                    return true;
                }

                if (!mSlidePosition && Math.abs(distanceY) > Math.abs(distanceX)) {

                    if (e1.getX() > getWidth() / 2f && mEnableSliderVolume && !mSlideBrightness) {

                        if (getHeight() > (mScreenHeight * 2 / 3)) {
                            mSlideDamping = 3f;
                        } else {
                            mSlideDamping = 2f;
                        }

                        mSlideVolume = true;
                        //滑动改变音量
                        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int diff = (int) ((e1.getY() - e2.getY()) * mSlideDamping * maxVolume / getHeight()) + mPreSlideVolume;
                        if (diff < 0) {
                            diff = 0;
                        }
                        if (diff > maxVolume) {
                            diff = maxVolume;
                        }
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, diff, 0);
                        Message message = mMainHandler.obtainMessage(UPDATE_VOLUME_DIALOG, diff);
                        mMainHandler.sendMessage(message);

                        return true;

                    } else if (mEnableSlideBrightness && !mSlideVolume) {
                        //滑动改变亮度
                        if (getHeight() > (mScreenHeight * 2 / 3)) {
                            mSlideDamping = 3f;
                        } else {
                            mSlideDamping = 2f;
                        }
                        mSlideBrightness = true;
                        float diff = (e1.getY() - e2.getY()) * mSlideDamping / getHeight() + mPreSlideBrightness;
                        if (diff < 0) {
                            diff = 0.01f;
                        }
                        if (diff > 1) {
                            diff = 1;
                        }

                        WindowManager.LayoutParams layoutParams = mContext.getWindow().getAttributes();
                        layoutParams.screenBrightness = diff;
                        mContext.getWindow().setAttributes(layoutParams);

                        Message message = mMainHandler.obtainMessage(UPDATE_BRIGHTNESS_DIALOG, diff);
                        mMainHandler.sendMessage(message);


                        return true;
                    }
                }

                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
    }

    /** 对通过旋转屏幕来全屏的返回键处理 */
    private final OnKeyListener mOrientationKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (mFullscreen && mFullScreenMode == FULLSCREEN_ORIENTATION) {
                //当前为旋转屏幕的全屏模式
                toggleScreenOrientation();
                return true;
            }
            return false;
        }
    };

    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    int pos = setProgress();
                    if (!mDragging && IjkVideoManager.getInstance().isPlaying() && (mShowing || mShowBottomProgress)) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case HANDLE_STATE_UI:
                    handleStateUI((Integer) msg.obj);
                    break;
                case HIDE_LAYOUT:
                    hideLayout();
                    break;
                case SHOW_LAYOUT:
                    showLayout();
                    break;
                case UPDATE_THUMB:
                    updateThumb();
                    break;
                case UPDATE_POSITION_DIALOG:
                    showPositionDialog((Integer) msg.obj);
                    break;
                case UPDATE_BRIGHTNESS_DIALOG:
                    showBrightnessDialog((Float) msg.obj);
                    break;
                case UPDATE_VOLUME_DIALOG:
                    showVolumeDialog((Integer) msg.obj);
                    break;
            }
        }
    };

    /** 显示布局 */
    private void showLayout() {
        if (mLayoutBottom != null && mShowBottomLayout) {
            mLayoutBottom.setVisibility(VISIBLE);
        }

        if (mBtnPlay != null && (mLoadingView == null || mLoadingView.getVisibility() != VISIBLE)) {
            mBtnPlay.setVisibility(VISIBLE);
        }
        if (mBottomProgressbar != null) {
            mBottomProgressbar.setVisibility(GONE);
        }

    }

    /** 隐藏布局 */
    private void hideLayout() {
        if (mLayoutBottom != null) {
            mLayoutBottom.setVisibility(GONE);
        }

        if (mBtnPlay != null && !mShowPlay) {
            mBtnPlay.setVisibility(GONE);
        }

        if (mBottomProgressbar != null) {
            mBottomProgressbar.setVisibility(mShowBottomProgress && mEnabled ? VISIBLE : GONE);
        }

    }

    private void toggleMediaControlsVisiblity() {
        if (mShowing) {
            hide();
        } else {
            show();
        }
    }

    /** 开始播放视频 */
    private void playMedia() {

        if (!IjkVideoManager.getInstance().isPlaying()) {
            IjkVideoManager.getInstance().start();
            debug("开始播放视频");
        }
    }

    /** 暂停视频 */
    private void pauseMedia() {
        if (mEnabled && IjkVideoManager.getInstance().isPlaying()) {
            IjkVideoManager.getInstance().pause();
            debug("暂停播放视频");
        }
    }

    /** 设置播放进度 */
    private int setProgress() {
        if (mDragging) {
            return 0;
        }
        int position = IjkVideoManager.getInstance().getCurrentPosition();
        //fix 暂停后第一次获取当前进度值不准确
        if (mOldPlayPosition != 0 && position - mOldPlayPosition > 2) {
            mOldPlayPosition = 0;
            return mOldPlayPosition;
        }

        int duration = IjkVideoManager.getInstance().getDuration();

        if (duration > 0) {
            // use long to avoid overflow
            long pos = mSeekProgress.getMax() * position / duration;
            //播放进度
            mSeekProgress.setProgress((int) pos);
            if (mShowBottomProgress) {
                mBottomProgressbar.setProgress((int) pos);
            }
        }
        //缓冲进度
        int percent = IjkVideoManager.getInstance().getBufferPercentage();
        mSeekProgress.setSecondaryProgress((int) (percent / 100f * mSeekProgress.getMax()));
        if (mShowBottomProgress) {
            mBottomProgressbar.setSecondaryProgress((int) (percent / 100f * mSeekProgress.getMax()));
        }

        mTextTotal.setText(stringForTime(duration));

        mTextCurrent.setText(stringForTime(position));

        return position;
    }

    private void updatePausePlay() {
        if (IjkVideoManager.getInstance() == null) {
            return;
        }
        if (IjkVideoManager.getInstance().isPlaying()) {
            mBtnPlay.setCurrentState(ENPlayView.STATE_PLAY);
        } else {
            mBtnPlay.setCurrentState(ENPlayView.STATE_PAUSE);
        }

    }

    //seek
    private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            if (!fromUser || mSeekProgress == null) {
                return;
            }

            if (!mEnabled) {
                showInfo(NOT_READY_INFO);
                return;
            }

            long duration = IjkVideoManager.getInstance().getDuration();
            long newposition = (duration * progress) / mSeekProgress.getMax();
            if (mTextCurrent != null) {
                mTextCurrent.setText(stringForTime((int) newposition));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

            show(3600000);

            mDragging = true;

            mMainHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            mDragging = false;

            long duration = IjkVideoManager.getInstance().getDuration();
            long newposition = (duration * mSeekProgress.getProgress()) / mSeekProgress.getMax();
            IjkVideoManager.getInstance().seekTo((int) newposition);

            setProgress();
            updatePausePlay();
            show();

            mMainHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    private void initControllerView(View mediaView) {
        mLoadingView = (ENDownloadView) mediaView.findViewById(R.id.loading);
        mBtnPlay = (ENPlayView) mediaView.findViewById(R.id.play);
        mLayoutBottom = (LinearLayout) mediaView.findViewById(R.id.layout_bottom);
        mLayoutBottom.setVisibility(mShowBottomLayout ? VISIBLE : GONE);
        mTextCurrent = (TextView) mediaView.findViewById(R.id.current);
        mSeekProgress = (SeekBar) mediaView.findViewById(R.id.progress);
        mTextTotal = (TextView) mediaView.findViewById(R.id.total);
        mBtnFullscreen = (ImageView) mediaView.findViewById(fullscreen);
        mBottomProgressbar = (ProgressBar) mediaView.findViewById(R.id.bottom_progressbar);
        mBottomProgressbar.setVisibility(mShowBottomProgress ? VISIBLE : GONE);
        mImgThumb = (ImageView) mediaView.findViewById(R.id.thumb);
        mImgThumb.setVisibility(mShowThumb ? VISIBLE : GONE);

        mBtnFullscreen.setOnClickListener(mFullScreenListener);

        mBtnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //检查网络
                if (mCheckWifi && !NetworkUtils.isWifiConnected(mContext) && !IjkVideoManager.getInstance().isPlaying()) {
                    mShowPlay = true;
                    if (mBtnPlay != null) {
                        mBtnPlay.setVisibility(VISIBLE);
                    }
                    showInfoDialog("当前为非 Wift 环境，是否继续播放视频？", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mShowPlay = false;
                            if (mBtnPlay != null) {
                                mBtnPlay.setVisibility(GONE);
                            }
                            startPlay();
                        }
                    });
                    return;
                }
                startPlay();
            }
        });

        mSeekProgress.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        handleControllLayout();
    }

    private MediaController startPlay() {


        if (mEnabled) {
            if (IjkVideoManager.getInstance().isPlaying()) {
                pauseMedia();
            } else {
                playMedia();
            }
        } else if (mVideoView != null) {
            mPreparedPlay = true;
            mVideoView.openVideo();
        }
        return this;
    }

    private void handleControllLayout() {
        if (mShowBottomLayout) {
            boolean show = mEnabled && mShowing;
            if (show) {
                mLayoutBottom.setVisibility(VISIBLE);
            }
        } else {
            mLayoutBottom.setVisibility(GONE);
        }
    }

    private OnClickListener mFullScreenListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mEnabled) {
                showInfo(NOT_READY_INFO);
                return;
            }
            if (mFullScreenMode == FULLSCREEN_ORIENTATION) {
                mOrientationUtils.toggleScreenOrientation();
            } else {
                if (!mFullscreen) {
                    addFullScreenView();
                } else {
                    removeFullScreenView();
                }
            }
        }
    };


    private void addFullScreenView() {

        if (!mEnabled || mCurrentUri == null) {
            return;
        }

        final ViewGroup viewGroup = (ViewGroup) mContext.findViewById(Window.ID_ANDROID_CONTENT);
        if (viewGroup == null) {
            return;
        }

        //设置 fitsSystemWindows = true
        viewGroup.getChildAt(0).setFitsSystemWindows(true);

        //清除旧的
        if (mFullScreenView != null) {
            int index = viewGroup.indexOfChild(mFullScreenView);
            if (index > -1) {
                viewGroup.removeView(mFullScreenView);
            }
            mFullScreenView = null;
        }

        IjkVideoManager.getInstance().clearVideoView();
        IjkVideoManager.getInstance().clearStateChangeListener();


        mFullScreenView = new IjkVideoView(mContext);
        final IjkVideoView ijkVideoView = (IjkVideoView) mFullScreenView;
        if (!IjkVideoManager.getInstance().isPlaying()) {
            mVideoView.initPauseCover();
            if (mVideoView.getPauseBitmap() != null) {
                ijkVideoView.setPauseBitmap(Bitmap.createBitmap(mVideoView.getPauseBitmap()));
            }
        }

        ijkVideoView.setSettings(mVideoView.getSettings());
        final MediaController mediaController = new MediaController(mContext);
        mediaController.setEnableSlideBrightness(mFullScreenViewEnableSlideBrightness);
        mediaController.setEnableSlidePosition(mFullScreenViewEnableSlidePosition);
        mediaController.setEnableSliderVolume(mFullScreenViewEnableSliderVolume);
        mediaController.setShowBottomLayout(mShowBottomLayout);
        //全屏播放器可以处理返回按键
        mediaController.setFocusableInTouchMode(true);
        mediaController.requestFocus();
        mediaController.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    //按下返回按键
                    if (mediaController.isFullscreen()) {
                        mediaController.toggleScreenOrientation();
                    } else {
                        if (!IjkVideoManager.getInstance().isPlaying()) {
                            ijkVideoView.initPauseCover();
                            if (ijkVideoView.getPauseBitmap() != null) {
                                mVideoView.setPauseBitmap(Bitmap.createBitmap(ijkVideoView.getPauseBitmap()));
                            }
                        }
                        removeFullScreenView();
                    }
                    return true;
                }
                return false;
            }
        });
        mediaController.setMute(mFullScreenMute);
        mediaController.setShowThumb(false);
        ijkVideoView.setMediaController(mediaController);
        ijkVideoView.setVideoURI(mCurrentUri);
        IjkVideoManager.getInstance().setVideoView(ijkVideoView);
        IjkVideoManager.getInstance().setStateChangeListener(mediaController);
        IjkVideoManager.getInstance().setPlayPosition(-1);
        IjkVideoManager.getInstance().refreshRenderView();


        fullscreen();

        if (mFullScreenViewAnim) {

            getLocationOnScreen(mLocations);

            int leftMargin = mLocations[0];
            int topMargin = mLocations[1];

            if (!mOriginalFullScreen) {
                topMargin -= mStatusHeight;
            }

            FrameLayout frameLayout = new FrameLayout(mContext);
            ViewGroup.LayoutParams frameLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT);
            frameLayout.setBackgroundColor(Color.BLACK);
            viewGroup.addView(frameLayout, frameLayoutParams);

            MarginLayoutParams layoutParams = new MarginLayoutParams(getWidth(), getHeight());
            layoutParams.leftMargin = leftMargin;
            layoutParams.topMargin = topMargin;
            frameLayout.addView(mFullScreenView, layoutParams);

            mFullScreenView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mFullScreenView.getLayoutParams() instanceof MarginLayoutParams) {
                        TransitionManager.beginDelayedTransition(viewGroup);

                        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) mFullScreenView.getLayoutParams();
                        marginLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        marginLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        marginLayoutParams.leftMargin = 0;
                        marginLayoutParams.topMargin = 0;
                        mFullScreenView.setLayoutParams(marginLayoutParams);
                    }
                }
            }, 300);

        } else {
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            viewGroup.addView(mFullScreenView, layoutParams);
        }


        mFullscreen = true;

    }


    private void fullscreen() {
        //全屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSystemUiVisibility = mContext.getWindow().getDecorView().getSystemUiVisibility()
                    | SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            int systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | SYSTEM_UI_FLAG_FULLSCREEN
                    | SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            mContext.getWindow().getDecorView().setSystemUiVisibility(systemUiVisibility);
        }
        int flag = mContext.getWindow().getAttributes().flags;
        mOriginalFullScreen = (flag & WindowManager.LayoutParams.FLAG_FULLSCREEN) > 0;
        if (!mOriginalFullScreen) {
            mContext.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                    , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        mOriginalActionBar = CommonUtils.hideActionBar(mContext);

    }


    private void removeFullScreenView() {

        final ViewGroup viewGroup = (ViewGroup) mContext.findViewById(Window.ID_ANDROID_CONTENT);

        if (!mEnabled || viewGroup == null) {
            return;
        }

        if (mFullScreenViewAnim) {
            int leftMargin = mLocations[0];
            int topMargin = mLocations[1];

            if (!mOriginalFullScreen) {
                topMargin -= mStatusHeight;
            }

            SimpleTransitionListener simpleTransitionListener = new SimpleTransitionListener() {
                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    super.onTransitionEnd(transition);
                    restoreFullScreenView();
                    transition.removeListener(this);
                }
            };
            AutoTransition autoTransition = new AutoTransition();
            autoTransition.addListener(simpleTransitionListener);

            TransitionManager.beginDelayedTransition(viewGroup, autoTransition);

            MarginLayoutParams layoutParams = (MarginLayoutParams) mFullScreenView.getLayoutParams();
            layoutParams.topMargin = topMargin;
            layoutParams.leftMargin = leftMargin;
            layoutParams.width = getWidth();
            layoutParams.height = getHeight();
            mFullScreenView.setLayoutParams(layoutParams);

        } else {
            restoreFullScreenView();
        }

    }

    private void restoreFullScreenView() {

        //恢复界面
        exitFullscreen();

        if (mFullScreenView != null) {
            ViewGroup viewGroup = (ViewGroup) mFullScreenView.getParent();
            viewGroup.removeView(mFullScreenView);
            mFullScreenView = null;

            if (mFullScreenViewAnim) {
                ((ViewGroup) viewGroup.getParent()).removeView(viewGroup);
            }

        }


        if (mVideoView != null) {
            //恢复
            IjkVideoManager.getInstance().setMute(mMute);
            IjkVideoManager.getInstance().setVideoView(mVideoView);
            IjkVideoManager.getInstance().setStateChangeListener(this);
            IjkVideoManager.getInstance().refreshRenderView();
        }
        mFullscreen = false;
    }

    private void exitFullscreen() {
        if (mSystemUiVisibility != -1) {
            mContext.getWindow().getDecorView().setSystemUiVisibility(mSystemUiVisibility);
        }
        if (!mOriginalFullScreen) {
            mContext.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (mOriginalActionBar) {
            mOriginalActionBar = false;
            CommonUtils.showActionBar(mContext);
        }


    }


    /** 更新封面图 */
    private void updateThumb() {
        if (mImgThumb != null) {
            boolean needShow = !isActive() && mShowThumb;
            mImgThumb.setVisibility(needShow ? VISIBLE : GONE);

        }
    }


    /** 是否为活动的 */
    private boolean isActive() {
        return mPlayPosition == IjkVideoManager.getInstance().getPlayPosition();
    }

    private void showInfoDialog(String message, DialogInterface.OnClickListener onClickListener) {
        if (mInfoDialog != null) {
            mInfoDialog.dismiss();
            mInfoDialog = null;
        }

        mInfoDialog = new AlertDialog.Builder(mContext)
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton("确定", onClickListener)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public interface OnFullScreenChangeListener {

        void onFullScreenChange(boolean fullscreen);
    }

    ///////////////////////////////////////////////////////////////////////////
    // volume dialog
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 设置当前音量
     *
     * @param volume -1 关闭 dialog
     */
    private void showVolumeDialog(int volume) {
        if (mVolumeDialog == null && volume != -1) {
            initVolumeDialog();
        }

        if (volume != -1) {
            getCurrentVolume();
            if (!mVolumeDialog.isShowing()) {

                //关闭其他 dialog
                mMainHandler.removeMessages(UPDATE_POSITION_DIALOG);
                if (mPositionDialog != null) {
                    mPositionDialog.dismiss();
                }
                mMainHandler.removeMessages(UPDATE_BRIGHTNESS_DIALOG);
                if (mBrightnessDialog != null) {
                    mBrightnessDialog.dismiss();
                }

                updateVolumeDialogLocation();
                mVolumeDialog.show();
            }
        } else {
            if (mVolumeDialog != null) {
                mVolumeDialog.dismiss();
            }
        }
    }

    private void initVolumeDialog() {
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
            mVolumeDialog = null;
        }

        mVolumeContentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_volume, null);
        mDialogVolume = (ProgressBar) mVolumeContentView.findViewById(R.id.volume);
        mDialogVolumeLogo = (ImageView) mVolumeContentView.findViewById(R.id.volume_logo);
        getCurrentVolume();

        mVolumeDialog = new AppCompatDialog(mContext, R.style.AppDialog);
        mVolumeDialog.setCancelable(false);
        mVolumeDialog.setContentView(mVolumeContentView);
    }

    /** 更新 dialog 位置 */
    private void updateVolumeDialogLocation() {
        if (mVolumeDialog.getWindow() == null || mVolumeContentView == null) {
            return;
        }
        int measure = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mVolumeContentView.measure(measure, measure);
        int contentViewHeight = mVolumeContentView.getMeasuredHeight();
        WindowManager.LayoutParams layoutParams = mVolumeDialog.getWindow().getAttributes();
        int[] locations = new int[2];
        getLocationOnScreen(locations);
        int centY = (int) (locations[1] + getHeight() / 2f);
        int screenCentY = (int) (mScreenHeight / 2f);
        layoutParams.y = (int) (centY - screenCentY - contentViewHeight / 2f);
        mVolumeDialog.getWindow().setAttributes(layoutParams);
    }

    private void getCurrentVolume() {
        if (mDialogVolume != null && mDialogVolumeLogo != null) {
            int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int progress = mDialogVolume.getMax() * volume / maxVolume;
            mDialogVolume.setProgress(progress);
            debug(String.format(Locale.CHINA, "当前音量：%d，最大音量：%d，占比：%d", volume, maxVolume, progress));
            if (volume == 0) {
                mDialogVolumeLogo.setImageResource(R.drawable.volume_x);
            } else if (progress > 70) {
                mDialogVolumeLogo.setImageResource(R.drawable.volume_2);
            } else {
                mDialogVolumeLogo.setImageResource(R.drawable.volume_1);
            }
        }
    }

    private void handleVoluemDialog() {
        if (mSlideVolume) {
            mSlideVolume = false;
            mMainHandler.removeMessages(UPDATE_VOLUME_DIALOG);
            Message message = mMainHandler.obtainMessage(UPDATE_VOLUME_DIALOG, -1);
            mMainHandler.sendMessage(message);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // brightness dialog
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 设置当前亮度
     *
     * @param brightness -1 关闭 dialog
     */
    private void showBrightnessDialog(float brightness) {
        if (mBrightnessDialog == null && brightness != -1) {
            initBrightnessDialog();
        }

        if (brightness != -1) {
            mDialogBrightness.setText(String.format(Locale.CHINA, "%d %%", (int) (brightness * 100)));
            if (!mBrightnessDialog.isShowing()) {


                //关闭其他 dialog
                mMainHandler.removeMessages(UPDATE_POSITION_DIALOG);
                if (mPositionDialog != null) {
                    mPositionDialog.dismiss();
                }
                mMainHandler.removeMessages(UPDATE_VOLUME_DIALOG);
                if (mVolumeDialog != null) {
                    mVolumeDialog.dismiss();
                }


                updateBrightnessDialogLocation();
                mBrightnessDialog.show();
            }
        } else {
            if (mBrightnessDialog != null) {
                mBrightnessDialog.dismiss();
            }
        }
    }

    private void initBrightnessDialog() {
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
            mBrightnessDialog = null;
        }

        mBrightnessContentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_brightness, null);
        mDialogBrightness = (TextView) mBrightnessContentView.findViewById(R.id.brightness);
        mDialogBrightness.setText(String.format(Locale.CHINA, "%d %%", 0));

        mBrightnessDialog = new AppCompatDialog(mContext, R.style.AppDialog);
        mBrightnessDialog.setCancelable(false);
        mBrightnessDialog.setContentView(mBrightnessContentView);
    }

    /** 更新 dialog 位置 */
    private void updateBrightnessDialogLocation() {
        if (mBrightnessDialog.getWindow() == null || mBrightnessContentView == null) {
            return;
        }
        int measure = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mBrightnessContentView.measure(measure, measure);
        int contentViewHeight = mBrightnessContentView.getMeasuredHeight();
        WindowManager.LayoutParams layoutParams = mBrightnessDialog.getWindow().getAttributes();
        int[] locations = new int[2];
        getLocationOnScreen(locations);
        int centY = (int) (locations[1] + getHeight() / 2f);
        int screenCentY = (int) (mScreenHeight / 2f);
        layoutParams.y = (int) (centY - screenCentY - contentViewHeight / 2f);
        mBrightnessDialog.getWindow().setAttributes(layoutParams);
    }

    private void handleBrightnessDialog() {
        if (mSlideBrightness) {
            mSlideBrightness = false;
            mMainHandler.removeMessages(UPDATE_BRIGHTNESS_DIALOG);
            Message message = mMainHandler.obtainMessage(UPDATE_BRIGHTNESS_DIALOG, -1f);
            mMainHandler.sendMessage(message);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // position dialog
    ///////////////////////////////////////////////////////////////////////////

    /** 处理滑动的播放进度 */
    private void handleSlidePosition() {
        //取消进度 dialog
        mMainHandler.removeMessages(UPDATE_POSITION_DIALOG);
        Message message = mMainHandler.obtainMessage(UPDATE_POSITION_DIALOG, -1);
        mMainHandler.sendMessage(message);
        //更新播放进度
        if (mSlidePosition && mPreSlidePosition != -1 && mPreSlidePosition != IjkVideoManager.getInstance().getCurrentPosition()) {
            IjkVideoManager.getInstance().seekTo(mPreSlidePosition);
        }
        mSlidePosition = false;
        mPreSlidePosition = -1;
    }

    /**
     * 设置进度 dialog
     *
     * @param seekPosition -1 为关闭 dialog
     */
    private void showPositionDialog(int seekPosition) {
        if (mPositionDialog == null && seekPosition != -1) {
            initPositionDialog();
        }

        if (seekPosition != -1) {
            int total = IjkVideoManager.getInstance().getDuration();

            long pos = mSeekProgress.getMax() * seekPosition / total;

            mDialogCurrent.setText(stringForTime(seekPosition));
            mDialogProgress.setProgress((int) pos);
            if (!mPositionDialog.isShowing()) {

                //关闭其他 dialog
                mMainHandler.removeMessages(UPDATE_BRIGHTNESS_DIALOG);
                if (mBrightnessDialog != null) {
                    mBrightnessDialog.dismiss();
                }
                mMainHandler.removeMessages(UPDATE_VOLUME_DIALOG);
                if (mVolumeDialog != null) {
                    mVolumeDialog.dismiss();
                }

                updatePositionDialogLocation();
                mPositionDialog.show();
            }
        } else {
            if (mPositionDialog != null) {
                mPositionDialog.dismiss();
            }
        }

    }

    /** 更新 dialog 位置 */
    private void updatePositionDialogLocation() {
        if (mPositionDialog.getWindow() == null || mPositionDialogContentView == null) {
            return;
        }
        int measure = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mPositionDialogContentView.measure(measure, measure);
        int contentViewHeight = mPositionDialogContentView.getMeasuredHeight();
        WindowManager.LayoutParams layoutParams = mPositionDialog.getWindow().getAttributes();
        int[] locations = new int[2];
        getLocationOnScreen(locations);
        int centY = (int) (locations[1] + getHeight() / 2f);
        int screenCentY = (int) (mScreenHeight / 2f);
        layoutParams.y = (int) (centY - screenCentY - contentViewHeight / 2f);
        mPositionDialog.getWindow().setAttributes(layoutParams);
    }

    private void initPositionDialog() {
        if (mPositionDialog != null) {
            mPositionDialog.dismiss();
            mPositionDialog = null;
        }

        mPositionDialogContentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_video_position, null);
        mDialogProgress = (ProgressBar) mPositionDialogContentView.findViewById(R.id.progress);
        mDialogTotal = (TextView) mPositionDialogContentView.findViewById(total);
        mDialogCurrent = (TextView) mPositionDialogContentView.findViewById(R.id.current);

        mDialogTotal.setText(String.format(Locale.CHINA, " / %s", stringForTime(IjkVideoManager.getInstance().getDuration())));
        mDialogCurrent.setText(stringForTime(0));
        mDialogProgress.setProgress(0);

        mPositionDialog = new AppCompatDialog(mContext, R.style.AppDialog);
        mPositionDialog.setCancelable(false);
        mPositionDialog.setContentView(mPositionDialogContentView);

    }

    ///////////////////////////////////////////////////////////////////////////
    // uitl
    ///////////////////////////////////////////////////////////////////////////

    private Toast mToast;

    private void showInfo(@NonNull String info) {
        if (mToast == null) {
            mToast = Toast.makeText(getContext(), info, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(info);
        }

        mToast.show();
    }

    private final static StringBuilder STRING_BUILDER = new StringBuilder();
    private final static Formatter FORMATTER = new Formatter(STRING_BUILDER, Locale.getDefault());

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        STRING_BUILDER.setLength(0);
        if (hours > 0) {
            return FORMATTER.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return FORMATTER.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void debug(@NonNull String info) {
        LoggerUtils.debugLog(TAG, info);
    }

    private void error(@NonNull String info) {
        LoggerUtils.errorLog(TAG, info);
    }

}
