/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leo.ijkplayer.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import com.leo.ijkplayer.media.controller.IMediaController;
import com.leo.ijkplayer.media.controller.IPlayerControl;
import com.leo.ijkplayer.media.render.IRenderView;
import com.leo.ijkplayer.media.render.SurfaceRenderView;
import com.leo.ijkplayer.media.render.TextureRenderView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;

public class IjkVideoView extends FrameLayout implements IVideoView, IPlayerControl {

    ///////////////////////////////////////////////////////////////////////////
    // 可配置
    ///////////////////////////////////////////////////////////////////////////
    /** 播放 Uri */
    private Uri mUri;
    /** 播放器旋转角度 */
    private int mVideoRotationDegree;
    /** 当前缓冲百分比 */
    private int mCurrentBufferPercentage;
    /** 缓冲中记录拖动位置 */
    private int mSeekWhenPrepared;
    /** player 配置 */
    private Settings mSettings;
    /** 媒体播放控制器 */
    private IMediaController mMediaController;

    ///////////////////////////////////////////////////////////////////////////
    // 常量
    ///////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IjkVideoView";
    /** 错误状态 */
    public static final int STATE_ERROR = -1;
    /** 闲置中 */
    public static final int STATE_IDLE = 0;
    /** 准备中 */
    public static final int STATE_PREPARING = 1;
    /** 准备好 */
    public static final int STATE_PREPARED = 2;
    /** 播放中 */
    public static final int STATE_PLAYING = 3;
    /** 暂停中 */
    public static final int STATE_PAUSED = 4;
    /** 播放完成 */
    public static final int STATE_PLAYBACK_COMPLETED = 5;

    ///////////////////////////////////////////////////////////////////////////
    // 内部使用
    ///////////////////////////////////////////////////////////////////////////
    private IMediaPlayer mMediaPlayer = null;
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    private Context mAppContext;
    private long mPrepareStartTime = 0;
    private long mPrepareEndTime = 0;
    private long mSeekStartTime = 0;
    private long mSeekEndTime = 0;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    public IjkVideoView(Context context) {
        super(context);
        initVideoView(context);
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        mAppContext = context.getApplicationContext();
        mVideoWidth = 0;
        mVideoHeight = 0;
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        notifyStateChange();
        mTargetState = STATE_IDLE;
        //bind manager
        IjkVideoManager.getInstance().setIVideoView(this);
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /** 释放 */
    public void release(boolean cleartargetstate) {
        IjkVideoManager.getInstance().release();
        if (cleartargetstate) {
            mTargetState = STATE_IDLE;
        }
    }

    /** 开始 */
    @Override
    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            notifyStateChange();
        }else {
            openVideo();
            requestLayout();
            invalidate();
        }
        mTargetState = STATE_PLAYING;
    }

    /** 暂停 */
    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
            notifyStateChange();
        }
        mTargetState = STATE_PAUSED;
    }

    /** 停止播放 */
    @Override
    public void stop() {
        IjkVideoManager.getInstance().release();
        releasePauseCover(true);
        mTargetState = STATE_IDLE;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    /** 当前播放视频进度 */
    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    /** 拖动视频 */
    @Override
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mSeekStartTime = System.currentTimeMillis();
            IjkVideoManager.getInstance().getMediaPlayer().seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    /** 当前是否在播放 */
    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return mCurrentBufferPercentage;
    }


    ///////////////////////////////////////////////////////////////////////////
    // media control
    ///////////////////////////////////////////////////////////////////////////

    public void setMediaController(IMediaController mediaController) {
        if (mediaController == null) {
            throw new IllegalArgumentException("media controller not null");
        }
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = mediaController;
        attachMediaController();
        notifyStateChange();
    }

    private void attachMediaController() {
        if (mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            mMediaController.setEnabled(isInPlaybackState());
            View contentView = mMediaController.makeControllerView();
            LayoutParams layoutParams = generateDefaultLayoutParams();
            addView(contentView, layoutParams);
        }
    }

    private void notifyStateChange() {
        handleStateChange();
        if (mMediaController != null) {
            mMediaController.notifyPlayState(mCurrentState);
        }
    }

    /** 处理 ijkview 的状态变化 */
    private void handleStateChange() {
        if (mCurrentState == STATE_PLAYING) {
            releasePauseCover(true);
        }
        if (mCurrentState == STATE_PAUSED) {
            updatePauseCover();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // video uri
    ///////////////////////////////////////////////////////////////////////////

    private void setVideoURI(Uri uri, Settings settings) {
        mUri = uri;
        mSettings = settings;
        mSeekWhenPrepared = 0;
        mCurrentBufferPercentage = 0;
        mVideoRotationDegree = 0;
        release(false);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void openVideo() {
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (mSettings == null) {
            mSettings = new Settings(mAppContext);
        }
        IjkVideoManager.getInstance().setVideoUri(mUri, mSettings);
        mCurrentBufferPercentage = 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    // video mananger
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreatePlayer(IMediaPlayer mp) {
        mMediaPlayer = mp;
        mPrepareStartTime = System.currentTimeMillis();
        // we don't set the target state here either, but preserve the
        // target state that was there before.
        mCurrentState = STATE_PREPARING;
        notifyStateChange();
        //attach media controller
        //init renders
        initRenders();
    }

    @Override
    public void onReleasePlayer() {
        //ijk view 需要释放的资源
        releasePauseCover(true);
        mCurrentState = STATE_IDLE;
        notifyStateChange();
        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {

        if (mMediaController != null) {
            mMediaController.setEnabled(true);
        }

        mPrepareEndTime = System.currentTimeMillis();
        mCurrentState = STATE_PREPARED;
        notifyStateChange();

        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();

        // mSeekWhenPrepared may be changed after seekTo() call
        int seekToPosition = mSeekWhenPrepared;
        if (seekToPosition != 0) {
            seekTo(seekToPosition);
        }
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            if (mRenderView != null) {
                mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                    // We didn't actually change the size (it was already at the size
                    // we need), so we won't get a "surface changed" callback, so
                    // start the video here instead of in the callback.

                    if (mTargetState == STATE_PLAYING) {
                        start();
                    } else if (!isPlaying() &&
                            (seekToPosition != 0 || getCurrentPosition() > 0)) {
                        if (mMediaController != null) {
                            mMediaController.show(0);
                        }
                    }
                }
            }
        } else {
            // We don't know the video size yet, but should start anyway.
            // The video size might be reported to us later.
            if (mTargetState == STATE_PLAYING) {
                start();
            }
        }
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        notifyStateChange();
        mTargetState = STATE_PLAYBACK_COMPLETED;
        if (mMediaController != null) {
            mMediaController.hide();
        }
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        mCurrentBufferPercentage = percent;
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        mSeekEndTime = System.currentTimeMillis();
        releasePauseCover(true);
    }

    @Override
    public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
        error(framework_err + "," + impl_err);
        mCurrentState = STATE_ERROR;
        notifyStateChange();
        mTargetState = STATE_ERROR;
        if (mMediaController != null) {
            mMediaController.hide();
        }
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                debug("MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                debug("MEDIA_INFO_VIDEO_RENDERING_START:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                debug("MEDIA_INFO_BUFFERING_START:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                debug("MEDIA_INFO_BUFFERING_END:");
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                debug("MEDIA_INFO_NETWORK_BANDWIDTH: " + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                debug("MEDIA_INFO_BAD_INTERLEAVING:");
                break;
            case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                debug("MEDIA_INFO_NOT_SEEKABLE:");
                break;
            case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                debug("MEDIA_INFO_METADATA_UPDATE:");
                break;
            case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                debug("MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                break;
            case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                debug("MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                mVideoRotationDegree = extra;
                debug("MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + extra);
                if (mRenderView != null)
                    mRenderView.setVideoRotation(extra);
                break;
            case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                debug("MEDIA_INFO_AUDIO_RENDERING_START:");
                break;
        }
        return true;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        mVideoSarNum = mp.getVideoSarNum();
        mVideoSarDen = mp.getVideoSarDen();
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            if (mRenderView != null) {
                mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
            }
            requestLayout();
        }
    }

    @Override
    public void onTimedText(IMediaPlayer mp, IjkTimedText text) {

    }

    ///////////////////////////////////////////////////////////////////////////
    // Aspect Ratio
    ///////////////////////////////////////////////////////////////////////////

    private int mCurrentAspectRatio = IRenderView.AR_ASPECT_FIT_PARENT;

    public void setAspectRatio(int aspectRatio) {
        mCurrentAspectRatio = aspectRatio;
        if (mRenderView != null) {
            mRenderView.setAspectRatio(mCurrentAspectRatio);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // render
    ///////////////////////////////////////////////////////////////////////////

    public static final int RENDER_NONE = 0;
    public static final int RENDER_SURFACE_VIEW = 1;
    public static final int RENDER_TEXTURE_VIEW = 2;

    private IRenderView mRenderView;
    private IRenderView.ISurfaceHolder mSurfaceHolder;
    private List<Integer> mAllRenders = new ArrayList<>();
    private int mCurrentRenderIndex = 0;
    private int mCurrentRender = RENDER_NONE;

    private void initRenders() {
        mAllRenders.clear();

        if (mSettings.getEnableSurfaceView()) {
            mAllRenders.add(RENDER_SURFACE_VIEW);
        }
        if (mSettings.getEnableTextureView() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mAllRenders.add(RENDER_TEXTURE_VIEW);
        }
        if (mSettings.getEnableNoView()) {
            mAllRenders.add(RENDER_NONE);
        }
        if (mAllRenders.isEmpty()) {
            mAllRenders.add(RENDER_TEXTURE_VIEW);
        }
        mCurrentRender = mAllRenders.get(mCurrentRenderIndex);
        setRender(mCurrentRender);
    }

    private void setRender(int render) {
        switch (render) {
            case RENDER_NONE:
                setRenderView(null);
                break;
            case RENDER_TEXTURE_VIEW: {
                TextureRenderView renderView = new TextureRenderView(getContext());
                IMediaPlayer mediaPlayer = IjkVideoManager.getInstance().getMediaPlayer();
                if (mediaPlayer != null) {
                    renderView.getSurfaceHolder().bindToMediaPlayer(mediaPlayer);
                    renderView.setVideoSize(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                    renderView.setVideoSampleAspectRatio(mediaPlayer.getVideoSarNum(), mediaPlayer.getVideoSarDen());
                    renderView.setAspectRatio(mCurrentAspectRatio);
                }
                setRenderView(renderView);
                break;
            }
            case RENDER_SURFACE_VIEW: {
                SurfaceRenderView renderView = new SurfaceRenderView(getContext());
                setRenderView(renderView);
                break;
            }
            default:
                Log.e(TAG, String.format(Locale.getDefault(), "invalid render %d\n", render));
                break;
        }
    }

    private void setRenderView(IRenderView renderView) {
        if (mRenderView != null) {

            if (IjkVideoManager.getInstance().getMediaPlayer() != null) {
                IjkVideoManager.getInstance().getMediaPlayer().setDisplay(null);
            }

            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            removeView(renderUIView);
        }

        if (renderView == null) {
            return;
        }

        mRenderView = renderView;
        renderView.setAspectRatio(mCurrentAspectRatio);
        if (mVideoWidth > 0 && mVideoHeight > 0) {
            renderView.setVideoSize(mVideoWidth, mVideoHeight);
        }
        if (mVideoSarNum > 0 && mVideoSarDen > 0) {
            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
        }

        View renderUIView = mRenderView.getView();
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView,0);

        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    private IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                debug("onSurfaceChanged: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = holder;
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                debug("onSurfaceCreated: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = holder;
            showPauseCover();
            releasePauseCover(false);
            bindSurfaceHolder(mMediaPlayer, holder);
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                debug("onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = null;

            releaseWithoutStop();
        }

    };

    public void releaseWithoutStop() {
        IMediaPlayer mediaPlayer = IjkVideoManager.getInstance().getMediaPlayer();
        if (mediaPlayer != null) {
            mediaPlayer.setDisplay(null);
        }
    }

    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null) {
            return;
        }
        if (holder == null) {
            mp.setDisplay(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }

    private Bitmap mPauseBitmap;

    /** 显示暂停切换显示的bitmap */
    private void showPauseCover() {
        // TODO: 2017/8/11 暂时只兼容 TextureRenderView
        if (mCurrentState == STATE_PAUSED && mPauseBitmap != null && !mPauseBitmap.isRecycled() && mSurfaceHolder != null
                && mRenderView != null) {
            Surface surface = mSurfaceHolder.openSurface();
            if (surface != null && surface.isValid()) {
                int width = mRenderView.getView().getWidth();
                int height = mRenderView.getView().getHeight();
                RectF rectF = new RectF(0, 0, width, height);
                Canvas canvas = surface.lockCanvas(new Rect(0, 0, width, height));
                if (canvas != null) {
                    canvas.drawBitmap(mPauseBitmap, null, rectF, null);
                    surface.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    /** 释放暂停时的封面 */
    private void releasePauseCover(boolean forced) {
        if ((mCurrentState != STATE_PAUSED || forced) && mPauseBitmap != null && !mPauseBitmap.isRecycled()) {
            mPauseBitmap.recycle();
            mPauseBitmap = null;
        }
    }

    /** 更新暂停时的封面 */
    private void updatePauseCover() {
        if (mPauseBitmap == null || mPauseBitmap.isRecycled()) {
            initCover();
        }
    }

    /** 初始化暂停时的封面 */
    private void initCover() {
        if (mRenderView == null) {
            return;
        }
        if (mPauseBitmap != null && !mPauseBitmap.isRecycled()) {
            mPauseBitmap.recycle();
        }
        mPauseBitmap = null;
        mPauseBitmap = mRenderView.getBitmap();
    }

    ///////////////////////////////////////////////////////////////////////////
    // util
    ///////////////////////////////////////////////////////////////////////////
    private String buildTimeMilli(long duration) {
        long total_seconds = duration / 1000;
        long hours = total_seconds / 3600;
        long minutes = (total_seconds % 3600) / 60;
        long seconds = total_seconds % 60;
        if (duration <= 0) {
            return "--:--";
        }
        if (hours >= 100) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    private void debug(@NonNull String info) {
        Log.d(TAG, info);
    }

    private void error(@NonNull String info) {
        Log.e(TAG, info);
    }
}
