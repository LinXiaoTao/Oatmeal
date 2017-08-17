package com.leo.ijkplayer.media.controller;

import android.view.View;

import com.leo.ijkplayer.media.StateChangeListener;
import com.leo.ijkplayer.media.videoview.IVideoView;

/**
 * Created on 2017/8/10 上午10:38.
 * leo linxiaotao1993@vip.qq.com
 */

public interface IMediaController extends StateChangeListener {

    View makeControllerView();

    void setVideoView(IVideoView videoView);

    void setEnabled(boolean enable);
}
