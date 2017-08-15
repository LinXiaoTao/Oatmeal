package com.leo.ijkplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.leo.ijkplayer.media.IjkVideoView;
import com.leo.ijkplayer.media.controller.MediaController;

public class MainActivity extends AppCompatActivity {

    private IjkVideoView mIjkVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIjkVideoView = (IjkVideoView) findViewById(R.id.video_view);
        MediaController mediaController = new MediaController(this);
        mIjkVideoView.setMediaController(mediaController);

        mIjkVideoView.setVideoPath("http://baobab.wdjcdn.com/14564977406580.mp4");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIjkVideoView.stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIjkVideoView.pause();
    }
}
