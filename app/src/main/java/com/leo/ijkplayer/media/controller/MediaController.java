package com.leo.ijkplayer.media.controller;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.ijkplayer.R;
import com.leo.ijkplayer.media.IjkVideoManager;
import com.leo.ijkplayer.media.util.OrientationUtils;
import com.leo.ijkplayer.media.videoview.IVideoView;
import com.leo.ijkplayer.media.videoview.IjkVideoView;
import com.leo.ijkplayer.media.weiget.ENDownloadView;
import com.leo.ijkplayer.media.weiget.ENPlayView;

import java.util.Formatter;
import java.util.Locale;

/**
 * 默认 媒体播放控制器
 * Created on 2017/8/10 上午10:46.
 * leo linxiaotao1993@vip.qq.com
 */

public class MediaController extends FrameLayout implements IMediaController, OrientationUtils.Callback {

    private static final String TAG = "MediaController";


    ///////////////////////////////////////////////////////////////////////////
    // 可配置
    ///////////////////////////////////////////////////////////////////////////
    /** 是否显示底部进度条 */
    private boolean mShowBottomProgress = true;
    /** 是否显示控制布局 */
    private boolean mShowControllLayout = true;
    /** 是否显示封面图 */
    private boolean mShowThumb = true;
    @DrawableRes
    private int mThumbRes;
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

    ///////////////////////////////////////////////////////////////////////////
    // 内部变量
    ///////////////////////////////////////////////////////////////////////////
    private OrientationUtils mOrientationUtils;
    private IVideoView mVideoView;
    private Activity mContext;
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
    /** 全屏显示的控件 */
    private View mFullScreenView;
    /** 是否延迟启动 */
    private boolean mDelayStart;
    private GestureDetectorCompat mGestureDetectorCompat;
    private int mCurrentState;
    private AlertDialog mInfoDialog;


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

    public MediaController setShowControllLayout(boolean showControllLayout) {
        mShowControllLayout = showControllLayout;
        return this;
    }

    public MediaController setShowThumb(boolean showThumb) {
        mShowThumb = showThumb;
        mMainHandler.sendEmptyMessage(UPDATE_THUMB);
        return this;
    }

    public MediaController setThumbRes(int thumbRes) {
        mThumbRes = thumbRes;
        mMainHandler.sendEmptyMessage(UPDATE_THUMB);
        return this;
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
            IjkVideoManager.getInstance().setMute(mMute);
            IjkVideoManager.getInstance().setStateChangeListener(this);
            debug("当前为启动状态");
            if (mDelayStart) {
                mDelayStart = false;
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
                if (mBtnPlay != null) {
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
        if (!isActive()) {
            return;
        }

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

        mOrientationUtils = new OrientationUtils(mContext);
        mOrientationUtils.setCallback(this);
        mOrientationUtils.setEnable(mEnableOrientation);

        setBackgroundColor(Color.TRANSPARENT);
        setClickable(true);

        setOnKeyListener(mOrientationKeyListener);

        mGestureDetectorCompat = new GestureDetectorCompat(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return mEnabled;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleMediaControlsVisiblity();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (IjkVideoManager.getInstance().isPlaying()) {
                    pauseMedia();
                } else {
                    playMedia();
                }
                return true;
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
            }
        }
    };

    /** 复位布局 */
    private void resetLayout() {
        if (mBottomProgressbar != null) {
            mBottomProgressbar.setProgress(0);
            mBottomProgressbar.setSecondaryProgress(0);
        }
        if (mSeekProgress != null) {
            mSeekProgress.setProgress(0);
            mSeekProgress.setSecondaryProgress(0);
        }
        if (mBtnPlay != null) {
            mBtnPlay.setCurrentState(ENPlayView.STATE_PAUSE);
        }

        if (mLoadingView != null) {
            mLoadingView.reset();
        }

        if (mTextCurrent != null) {
            mTextCurrent.setText(stringForTime(0));
        }
    }

    /** 显示布局 */
    private void showLayout() {
        if (mLayoutBottom != null && mShowControllLayout) {
            mLayoutBottom.setVisibility(VISIBLE);
        }

        if (mBtnPlay != null && mShowControllLayout && (mLoadingView == null || mLoadingView.getVisibility() != VISIBLE)) {
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
        mTextCurrent = (TextView) mediaView.findViewById(R.id.current);
        mSeekProgress = (SeekBar) mediaView.findViewById(R.id.progress);
        mTextTotal = (TextView) mediaView.findViewById(R.id.total);
        mBtnFullscreen = (ImageView) mediaView.findViewById(R.id.fullscreen);
        mBottomProgressbar = (ProgressBar) mediaView.findViewById(R.id.bottom_progressbar);
        mBottomProgressbar.setVisibility(mShowBottomProgress ? VISIBLE : GONE);
        mImgThumb = (ImageView) mediaView.findViewById(R.id.thumb);
        mImgThumb.setVisibility(mShowThumb ? VISIBLE : GONE);
        if (mShowThumb && mThumbRes > 0) {
            mImgThumb.setImageResource(mThumbRes);
        }

        mBtnFullscreen.setOnClickListener(mFullScreenListener);

        mBtnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEnabled) {
                    if (IjkVideoManager.getInstance().isPlaying()) {
                        pauseMedia();
                    } else {
                        playMedia();
                    }
                } else if (mVideoView != null) {
                    mDelayStart = true;
                    mVideoView.openVideo();
                }
            }
        });

        mSeekProgress.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        handleControllLayout();
    }

    private void handleControllLayout() {
        if (mShowControllLayout) {
            boolean show = mEnabled && mShowing && (mLoadingView == null || mLoadingView.getVisibility() != VISIBLE);
            if (show) {
                mLayoutBottom.setVisibility(VISIBLE);
                mBtnPlay.setVisibility(VISIBLE);
            }
        } else {
            mLayoutBottom.setVisibility(GONE);
            mBtnPlay.setVisibility(GONE);
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

        ViewGroup viewGroup = (ViewGroup) mContext.findViewById(Window.ID_ANDROID_CONTENT);
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

        mSystemUiVisibility = mContext.getWindow().getDecorView().getSystemUiVisibility()
                | SYSTEM_UI_FLAG_LAYOUT_STABLE
                | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

        int systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | SYSTEM_UI_FLAG_LAYOUT_STABLE
                | SYSTEM_UI_FLAG_FULLSCREEN
                | SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        mContext.getWindow().getDecorView().setSystemUiVisibility(systemUiVisibility);


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
        ijkVideoView.setVideoPath("http://baobab.wdjcdn.com/14564977406580.mp4");
        IjkVideoManager.getInstance().setVideoView(ijkVideoView);
        IjkVideoManager.getInstance().setStateChangeListener(mediaController);
        IjkVideoManager.getInstance().setPlayPosition(-1);
        IjkVideoManager.getInstance().refreshRenderView();


        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewGroup.addView(mFullScreenView, layoutParams);


    }

    private void removeFullScreenView() {


        if (mFullScreenView != null) {
            ViewGroup viewGroup = (ViewGroup) mFullScreenView.getParent();
            viewGroup.removeView(mFullScreenView);
            mFullScreenView = null;
        }

        if (mSystemUiVisibility != -1) {
            mContext.getWindow().getDecorView().setSystemUiVisibility(mSystemUiVisibility);
        }
        if (mVideoView != null) {
            //恢复
            IjkVideoManager.getInstance().setMute(mMute);
            IjkVideoManager.getInstance().setVideoView(mVideoView);
            IjkVideoManager.getInstance().setStateChangeListener(this);
            IjkVideoManager.getInstance().refreshRenderView();
        }

    }

    /** 更新封面图 */
    private void updateThumb() {
        if (mImgThumb != null) {
            boolean needShow = !isActive() && mShowThumb;
            mImgThumb.setVisibility(needShow ? VISIBLE : GONE);
            if (needShow && mThumbRes > 0) {
                mImgThumb.setImageResource(mThumbRes);
            }

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
                .setTitle("播放器状态")
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
        Log.d(TAG, info);
    }

    private void error(@NonNull String info) {
        Log.e(TAG, info);
    }

}
