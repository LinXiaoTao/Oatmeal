package com.leo.ijkplayer.media.controller;

/**
 * Created on 2017/8/10 上午10:41.
 * leo linxiaotao1993@vip.qq.com
 */

public interface IPlayerControl {

    /** 开始播放 */
    void start();

    /** 暂停播放 */
    void pause();

    /** 停止播放 */
    void stop();

    /** 是否在播放中 */
    boolean isPlaying();

    /** 获取视频时长 */
    int getDuration();

    /** 获取当前播放位置 */
    int getCurrentPosition();

    void seekTo(int pos);

    /** 返回当前缓冲百分比 */
    int getBufferPercentage();



}
