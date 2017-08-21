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

package com.leo.player.media;

import android.content.Context;

import java.util.Map;


/** 播放器初始参数 */
@SuppressWarnings("unused")
public class Settings {

    private Context mAppContext;

    public static final int PV_PLAYER__Auto = 0;
    public static final int PV_PLAYER__AndroidMediaPlayer = 1;
    public static final int PV_PLAYER__IjkMediaPlayer = 2;
    public static final int PV_PLAYER__IjkExoMediaPlayer = 3;

    ///////////////////////////////////////////////////////////////////////////
    // 可配置
    ///////////////////////////////////////////////////////////////////////////
    /** 播放器类型 */
    private int mPlayer;
    /** 是否循环播放 */
    private boolean looping;
    /** 播放速度 */
    private float speed;
    /** header */
    private Map<String, String> mHeaders;

    private boolean mUsingMediaCodec;
    private boolean mUsingMediaCodecAutoRotate;
    private boolean mMediaCodecHandleResolutionChange;
    private boolean mUsingOpenSLES;
    private String mPixelFormat;
    private boolean mEnableNoView;
    private boolean mEnableSurfaceView;
    private boolean mEnableTextureView;
    private boolean mEnableDetachedSurfaceTextureView;
    private boolean mUsingMediaDataSource;

    public Settings(Context context) {
        mAppContext = context.getApplicationContext();
    }

    public Context getAppContext() {
        return mAppContext;
    }

    public boolean isLooping() {
        return looping;
    }

    public Settings setLooping(boolean looping) {
        this.looping = looping;
        return this;
    }

    public float getSpeed() {
        return speed;
    }

    public Settings setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public Settings setHeaders(Map<String, String> headers) {
        mHeaders = headers;
        return this;
    }

    public boolean isUsingMediaCodec() {
        return mUsingMediaCodec;
    }

    public boolean isUsingMediaCodecAutoRotate() {
        return mUsingMediaCodecAutoRotate;
    }

    public boolean isMediaCodecHandleResolutionChange() {
        return mMediaCodecHandleResolutionChange;
    }

    public boolean isUsingOpenSLES() {
        return mUsingOpenSLES;
    }

    public boolean isEnableNoView() {
        return mEnableNoView;
    }

    public boolean isEnableSurfaceView() {
        return mEnableSurfaceView;
    }

    public boolean isEnableTextureView() {
        return mEnableTextureView;
    }

    public boolean isEnableDetachedSurfaceTextureView() {
        return mEnableDetachedSurfaceTextureView;
    }

    public boolean isUsingMediaDataSource() {
        return mUsingMediaDataSource;
    }

    public int getPlayer() {
        return mPlayer;
    }

    public boolean getUsingMediaCodec() {
        return mUsingMediaCodec;
    }

    public boolean getUsingMediaCodecAutoRotate() {
        return mUsingMediaCodecAutoRotate;
    }

    public boolean getMediaCodecHandleResolutionChange() {
        return mMediaCodecHandleResolutionChange;
    }

    public boolean getUsingOpenSLES() {
        return mUsingOpenSLES;
    }

    public String getPixelFormat() {
        return mPixelFormat;
    }

    public boolean getEnableNoView() {
        return mEnableNoView;
    }

    public boolean getEnableSurfaceView() {
        return mEnableSurfaceView;
    }

    public boolean getEnableTextureView() {
        return mEnableTextureView;
    }

    public boolean getEnableDetachedSurfaceTextureView() {
        return mEnableDetachedSurfaceTextureView;
    }

    public boolean getUsingMediaDataSource() {
        return mUsingMediaDataSource;
    }

}
