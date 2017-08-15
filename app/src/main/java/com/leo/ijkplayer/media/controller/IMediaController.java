package com.leo.ijkplayer.media.controller;

import android.view.View;

/**
 * Created on 2017/8/10 上午10:38.
 * leo linxiaotao1993@vip.qq.com
 */

public interface IMediaController {

    void hide();

    void setEnabled(boolean enabled);

    void setMediaPlayer(IPlayerControl playerControl);

    void show();

    void show(int timeout);

    View makeControllerView();

    void notifyPlayState(int state);

}
