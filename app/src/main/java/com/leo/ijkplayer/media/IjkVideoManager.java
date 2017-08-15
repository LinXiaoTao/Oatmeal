package com.leo.ijkplayer.media;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.leo.ijkplayer.media.datasource.FileMediaDataSource;

import java.io.File;
import java.lang.ref.WeakReference;

import tv.danmaku.ijk.media.exo.IjkExoMediaPlayer;
import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkLibLoader;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import tv.danmaku.ijk.media.player.TextureMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

/**
 * 视频管理类
 * 单例
 * Created on 2017/8/5 下午4:29.
 * leo linxiaotao1993@vip.qq.com
 */

public final class IjkVideoManager implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener
        , IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener
        , IMediaPlayer.OnInfoListener, IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnTimedTextListener {


    ///////////////////////////////////////////////////////////////////////////
    // 可配置
    ///////////////////////////////////////////////////////////////////////////
    /** 当前播放的 Uri */
    private Uri mUri;
    /** video view */
    /** 因为 manager 是单例，所以在列表中使用时，要注意 videoview 的更新 */
    private WeakReference<IVideoView> mVideoView;
    /** ijk so loader */
    private static IjkLibLoader sIjkLibLoader;
    /** 当前是否为静音 */
    private boolean mMute;
    /** 是否需要进行缓冲超时判断 */
    private boolean mNeedTimeOut = true;
    /** 缓冲超时时间 */
    private int mTimeOut = DEFAULT_TIMEOUT;

    ///////////////////////////////////////////////////////////////////////////
    // 常量区
    ///////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IjkVideoManager";

    private static final int HANDLER_PREPARE = 0;
    private static final int HANDLER_RELEASE = 1;

    /** 默认缓冲超时时间 */
    private static final int DEFAULT_TIMEOUT = 8 * 1000;

    /** 超时错误码 */
    private static final int TIMEOUT_ERROR = -192;

    ///////////////////////////////////////////////////////////////////////////
    // 内部使用变量
    ///////////////////////////////////////////////////////////////////////////
    private static IjkVideoManager sInstance;
    private MediaHandler mMediaHandler;
    private Handler mMainThreadHandler;
    private IMediaPlayer mMediaPlayer;
    private final TimeOutRunnable mTimeOutRunnable = new TimeOutRunnable();


    public static IjkVideoManager getInstance() {
        if (sInstance == null) {
            synchronized (IjkVideoManager.class) {
                sInstance = new IjkVideoManager();
            }
        }
        return sInstance;
    }

    private IjkVideoManager() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mMediaHandler = new MediaHandler(handlerThread.getLooper());
        mMainThreadHandler = new Handler(Looper.getMainLooper());
    }

    /** 获取当前 media player */
    public IMediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    /**
     * 设置播放 Uri
     *
     * @param uri      播放源
     * @param settings 播放器配置
     */
    public void setVideoUri(@NonNull Uri uri, Settings settings) {
        mUri = uri;
        Message message = mMediaHandler.obtainMessage(HANDLER_PREPARE);
        message.obj = settings;
        mMediaHandler.sendMessage(message);
        if (mNeedTimeOut) {
            startTimeOutRunnable();
        }
    }

    /** 释放 media player */
    public void release() {
        Message message = mMediaHandler.obtainMessage(HANDLER_RELEASE);
        mMediaHandler.sendMessage(message);
    }

    public static void setIjkLibLoader(IjkLibLoader ijkLibLoader) {
        sIjkLibLoader = ijkLibLoader;
    }

    public IjkVideoManager setVideoView(@Nullable IVideoView videoView) {
        IVideoView oldVideoView = videoView();
        if (oldVideoView != null){
            //这里我们暂先让旧的 videoview，先走 release 回调
            oldVideoView.onReleasePlayer();
        }

        if (videoView == null) {
            mVideoView = null;
        } else {
            mVideoView = new WeakReference<>(videoView);
        }
        return this;
    }

    /**
     * 当前是否设置为静音
     *
     * @return 是否为静音
     */
    public boolean isMute() {
        return mMute;
    }

    /**
     * 设置播放器静音
     *
     * @param mute 是否为静音
     * @return this
     */
    public IjkVideoManager setMute(boolean mute) {
        mMute = mute;
        if (mMediaPlayer != null) {
            if (mMute) {
                mMediaPlayer.setVolume(0, 0);
            } else {
                mMediaPlayer.setVolume(1, 1);
            }
        }
        return this;
    }

    @Override
    public void onPrepared(final IMediaPlayer mp) {
        debugLog("onPrepared");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutRunnable();
                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onPrepared(mp);
                }
            }
        });
    }

    @Override
    public void onCompletion(final IMediaPlayer mp) {
        debugLog("onCompletion");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutRunnable();
                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onCompletion(mp);
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(final IMediaPlayer mp, final int percent) {
        debugLog("onBufferingUpdate");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onBufferingUpdate(mp, percent);
                }
            }
        });
    }

    @Override
    public void onSeekComplete(final IMediaPlayer mp) {
        debugLog("onSeekComplete");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutRunnable();
                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onSeekComplete(mp);
                }
            }
        });
    }

    @Override
    public boolean onError(final IMediaPlayer mp, final int what, final int extra) {
        errorLog("onError");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutRunnable();
                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onError(mp, what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(final IMediaPlayer mp, final int what, final int extra) {
        debugLog("onInfo");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mNeedTimeOut) {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        //开始缓冲
                        startTimeOutRunnable();
                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        //缓冲结束
                        cancelTimeOutRunnable();
                    }
                }

                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onInfo(mp, what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(final IMediaPlayer mp, final int width, final int height, final int sar_num, final int sar_den) {
        debugLog("onVideoSizeChanged");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onVideoSizeChanged(mp, width, height, sar_num, sar_den);
                }
            }
        });
    }

    @Override
    public void onTimedText(final IMediaPlayer mp, final IjkTimedText text) {
        debugLog("onTimedText");
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                IVideoView iVideoView = videoView();
                if (iVideoView != null) {
                    iVideoView.onTimedText(mp, text);
                }
            }
        });
    }


    private class MediaHandler extends Handler {

        MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    initPlayer(Settings.class.cast(msg.obj));
                    break;
                case HANDLER_RELEASE:
                    releaseVideoManager();
                    break;
            }
        }
    }

    private class TimeOutRunnable implements Runnable {

        @Override
        public void run() {
            IVideoView iVideoView = videoView();
            if (iVideoView != null) {
                iVideoView.onError(mMediaPlayer, TIMEOUT_ERROR, TIMEOUT_ERROR);
            }
        }
    }

    /** 启动超时检查任务 */
    private void startTimeOutRunnable() {
        debugLog("启动超时检查任务");
        mMainThreadHandler.postDelayed(mTimeOutRunnable, mTimeOut);
    }

    /** 取消超时检查任务 */
    private void cancelTimeOutRunnable() {
        debugLog("取消超时检查任务");
        mMainThreadHandler.removeCallbacks(mTimeOutRunnable);
    }

    /** 初始化播放器 */
    private void initPlayer(Settings settings) {

        try {
            releasePlayer(false);

            mMediaPlayer = createPlayer(settings);
            setMute(mMute);

            //data source
            String scheme = mUri.getScheme();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    settings.getUsingMediaDataSource() &&
                    (TextUtils.isEmpty(scheme) || scheme.equalsIgnoreCase("file"))) {
                IMediaDataSource dataSource = new FileMediaDataSource(new File(mUri.toString()));
                mMediaPlayer.setDataSource(dataSource);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && settings.getHeaders() != null) {
                mMediaPlayer.setDataSource(settings.getAppContext(), mUri, settings.getHeaders());
            } else {
                mMediaPlayer.setDataSource(mUri.toString());
            }

            //set listener
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.prepareAsync();

            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    IVideoView videoView = videoView();
                    if (videoView != null) {
                        videoView.onCreatePlayer(mMediaPlayer);
                    }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();

            releaseVideoManager();
            errorLog("initPlayer 出错了");
            onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }


    }

    /** 释放视频管理器 */
    private void releaseVideoManager() {
        releasePlayer(true);
        setMute(false);
        cancelTimeOutRunnable();
    }

    /** 释放播放器 */
    private void releasePlayer(boolean notify) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        IVideoView iVideoView = videoView();
        if (iVideoView != null && notify) {
            iVideoView.onReleasePlayer();
        }
    }


    /**
     * 根据 playertype 创建 MediaPlayer
     *
     * @return player
     */
    private IMediaPlayer createPlayer(Settings settings) {
        IMediaPlayer mediaPlayer;

        switch (settings.getPlayer()) {
            case Settings.PV_PLAYER__IjkExoMediaPlayer: {
                mediaPlayer = new IjkExoMediaPlayer(settings.getAppContext());
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            break;
            case Settings.PV_PLAYER__AndroidMediaPlayer: {
                mediaPlayer = new AndroidMediaPlayer();
            }
            break;
            case Settings.PV_PLAYER__IjkMediaPlayer:
            default: {
                mediaPlayer = initIjkPlayer(settings);
            }
            break;
        }

        if (settings.getEnableDetachedSurfaceTextureView()) {
            mediaPlayer = new TextureMediaPlayer(mediaPlayer);
        }

        return mediaPlayer;
    }

    /**
     * 配置 Ijk player
     *
     * @param settings 参数
     */
    private IjkMediaPlayer initIjkPlayer(Settings settings) {
        IjkMediaPlayer ijkMediaPlayer;
        if (sIjkLibLoader != null) {
            ijkMediaPlayer = new IjkMediaPlayer(sIjkLibLoader);
        } else {
            ijkMediaPlayer = new IjkMediaPlayer();
        }
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        ijkMediaPlayer.setSpeed(settings.getSpeed());
        ijkMediaPlayer.setLooping(settings.isLooping());
        if (settings.getUsingMediaCodec()) {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
            if (settings.getUsingMediaCodecAutoRotate()) {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
            } else {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
            }
            if (settings.getMediaCodecHandleResolutionChange()) {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
            } else {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
            }
        } else {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        }

        if (settings.getUsingOpenSLES()) {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        } else {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        }

        String pixelFormat = settings.getPixelFormat();
        if (TextUtils.isEmpty(pixelFormat)) {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        } else {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
        }
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        return ijkMediaPlayer;
    }

    private IVideoView videoView() {
        if (mVideoView != null) {
            return mVideoView.get();
        }
        return null;
    }

    private void debugLog(String message) {
        Log.d(TAG, message);
    }

    private void errorLog(String message) {
        Log.e(TAG, message);
    }


}
