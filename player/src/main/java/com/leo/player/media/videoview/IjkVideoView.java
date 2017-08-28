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

package com.leo.player.media.videoview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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

import com.leo.player.media.IjkVideoManager;
import com.leo.player.media.Settings;
import com.leo.player.media.controller.IMediaController;
import com.leo.player.media.render.IRenderView;
import com.leo.player.media.render.SurfaceRenderView;
import com.leo.player.media.render.TextureRenderView;
import com.leo.player.media.util.LoggerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;

public class IjkVideoView extends FrameLayout implements IVideoView {

    ///////////////////////////////////////////////////////////////////////////
    // 可配置
    ///////////////////////////////////////////////////////////////////////////
    /** 播放 Uri */
    private Uri mUri;
    /** 播放器旋转角度 */
    private int mVideoRotationDegree;
    /** player 配置 */
    private Settings mSettings;
    /** 兼容列表播放，记录当前播放序号 */
    private int mPlayPosition = -1;

    ///////////////////////////////////////////////////////////////////////////
    // 常量
    ///////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IjkVideoView";

    ///////////////////////////////////////////////////////////////////////////
    // 内部使用
    ///////////////////////////////////////////////////////////////////////////
    private Context mAppContext;
    private boolean mEnabled;
    private boolean mPrepared;
    private IMediaController mMediaController;
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
        setBackgroundColor(Color.BLACK);
        mAppContext = context.getApplicationContext();
        mVideoWidth = 0;
        mVideoHeight = 0;

    }

    public int getPlayPosition() {
        return mPlayPosition;
    }

    public IjkVideoView setPlayPosition(int playPosition) {
        mPlayPosition = playPosition;
        return this;
    }

    @Override
    public void setSettings(Settings settings) {
        mSettings = settings;
    }

    @Override
    public Settings getSettings() {
        return mSettings;
    }

    ///////////////////////////////////////////////////////////////////////////
    // media control
    ///////////////////////////////////////////////////////////////////////////

    public void setMediaController(IMediaController mediaController) {
        if (mediaController == null) {
            throw new IllegalArgumentException("media controller not null");
        }
        mMediaController = mediaController;
        View contentView = mediaController.makeControllerView();
        LayoutParams layoutParams = generateDefaultLayoutParams();
        addView(contentView, layoutParams);
        // TODO: 2017/8/17 这里将 setVideoView 延迟到 setUri 之后，是否合理？
        if (mUri != null) {
            mediaController.setVideoView(this);
        }
        mediaController.setEnabled(mPrepared);
    }

    ///////////////////////////////////////////////////////////////////////////
    // video uri
    ///////////////////////////////////////////////////////////////////////////

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

    private void setVideoURI(Uri uri, Settings settings) {
        mUri = uri;
        if (settings != null) {
            mSettings = settings;
        }
        mVideoRotationDegree = 0;
        if (mMediaController != null) {
            mMediaController.setVideoView(this);
        }
    }

    @Override
    public void openVideo() {

        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (mSettings == null) {
            mSettings = new Settings(mAppContext);
        }

        //对列表的兼容
        IjkVideoManager.getInstance().setVideoView(this);
        if (mMediaController != null) {
            IjkVideoManager.getInstance().setStateChangeListener(mMediaController);
        }

        IjkVideoManager.getInstance().setVideoUri(mUri, mSettings);
    }

    ///////////////////////////////////////////////////////////////////////////
    // video mananger
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreatePlayer(IMediaPlayer mp) {
        mEnabled = true;
        IjkVideoManager.getInstance().setPlayPosition(mPlayPosition);
        initRenders(mp);

    }

    @Override
    public void onPausePlayer(IMediaPlayer mp) {
        updatePauseCover();
    }

    @Override
    public void onStartPlayer(IMediaPlayer mp) {
        releasePauseCover();
    }

    @Override
    public void onReleasePlayer() {
        //ijk view 需要释放的资源
        mPrepared = false;
        mEnabled = false;
        releasePauseCover();
        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);
        if (IjkVideoManager.getInstance().getPlayPosition() == mPlayPosition) {
            IjkVideoManager.getInstance().setPlayPosition(-1);
        }
        if (IjkVideoManager.getInstance().getStateChangeListener() == mMediaController) {
            IjkVideoManager.getInstance().setStateChangeListener(null);
        }
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {

        mPrepared = true;

        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();

        IjkVideoManager.getInstance().handleSeekWhenPrepared();

        if (mVideoWidth != 0 && mVideoHeight != 0) {
            if (mRenderView != null) {
                mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                    if (mMediaController != null) {
                        mMediaController.setEnabled(true);
                    }
                }
            }
        } else {
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }
        }
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {

    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
    }

    @Override
    public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
        error(framework_err + "," + impl_err);
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
    // TODO: 2017/8/17 当前都是基于 TextureRenderView

    public static final int RENDER_NONE = 0;
    public static final int RENDER_SURFACE_VIEW = 1;
    public static final int RENDER_TEXTURE_VIEW = 2;

    private IRenderView mRenderView;
    private IRenderView.ISurfaceHolder mSurfaceHolder;
    private List<Integer> mAllRenders = new ArrayList<>();
    private int mCurrentRenderIndex = 0;
    private int mCurrentRender = RENDER_NONE;

    private void initRenders(IMediaPlayer mediaPlayer) {
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
        setRender(mCurrentRender, mediaPlayer);
    }

    private void setRender(int render, IMediaPlayer mediaPlayer) {
        switch (render) {
            case RENDER_NONE:
                setRenderView(null);
                break;
            case RENDER_TEXTURE_VIEW: {
                TextureRenderView renderView = new TextureRenderView(getContext());
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
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView, 0);

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
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (hasValidSize) {
                debug("onSurfaceChanged");
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                debug("onSurfaceCreated: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = holder;
            IjkVideoManager.getInstance().bindSurfaceHolder(IjkVideoView.this, mSurfaceHolder);
            showPauseCover();
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                debug("onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = null;
        }
    };


    private Bitmap mPauseBitmap;

    @Override
    public Uri getCurrentUri() {
        return mUri;
    }

    @Override
    public Bitmap getPauseBitmap() {
        return mPauseBitmap;
    }

    @Override
    public void setPauseBitmap(Bitmap pauseBitmap) {
        releasePauseCover();
        mPauseBitmap = pauseBitmap;
    }

    /** 初始化暂停时的封面 */
    @Override
    public void initPauseCover() {
        if (mRenderView == null) {
            return;
        }
        if (mPauseBitmap != null && !mPauseBitmap.isRecycled()) {
            mPauseBitmap.recycle();
        }
        mPauseBitmap = null;
        mPauseBitmap = mRenderView.getBitmap();
    }

    /** 更新暂停时的封面 */
    private void updatePauseCover() {
        if (mPauseBitmap == null || mPauseBitmap.isRecycled()) {
            initPauseCover();
        }
    }

    /** 显示暂停切换显示的bitmap */
    private void showPauseCover() {
        // TODO: 2017/8/11 暂时只兼容 TextureRenderView
        if (mPauseBitmap != null && !mPauseBitmap.isRecycled() && mSurfaceHolder != null
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
    private void releasePauseCover() {
        if (mPauseBitmap != null && !mPauseBitmap.isRecycled()) {
            mPauseBitmap.recycle();
            mPauseBitmap = null;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // util
    ///////////////////////////////////////////////////////////////////////////


    private void debug(@NonNull String info) {
        LoggerUtils.debugLog(TAG, info);
    }

    private void error(@NonNull String info) {
        LoggerUtils.errorLog(TAG, info);
    }
}
