package com.leo.player.media.controller;

import android.view.View;

import com.leo.player.media.StateChangeListener;
import com.leo.player.media.videoview.IVideoView;

/**
 * Created on 2017/8/10 上午10:38.
 * leo linxiaotao1993@vip.qq.com
 */

public interface IMediaController extends StateChangeListener {

    View makeControllerView();

    void setVideoView(IVideoView videoView);

    void setEnabled(boolean enable);
}
