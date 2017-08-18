package com.leo.ijkplayer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leo.ijkplayer.media.IjkVideoManager;
import com.leo.ijkplayer.media.controller.MediaController;
import com.leo.ijkplayer.media.videoview.IjkVideoView;

/**
 * Created on 2017/8/15 下午5:08.
 * leo linxiaotao1993@vip.qq.com
 */

public class ListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    /** 上一次播放下标 */
    private int mPrePlayPosition = -1;

    private static final String VIDEO_URL = "http://baobab.wdjcdn.com/14564977406580.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);



        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(mLinearLayoutManager = new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter());
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                autoPlay();
            }
        });
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    autoPlay();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (IjkVideoManager.getInstance().getPlayPosition() > -1) {
                    int position = IjkVideoManager.getInstance().getPlayPosition();
                    if (!isVisible(position)) {
                        IjkVideoManager.getInstance().release();
                    }
                }
            }
        });

    }



    /** 自动播放 */
    private void autoPlay(){
        int firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition();
        int position = (firstVisibleItem + lastVisibleItem) / 2;
        if (mPrePlayPosition != position && !isVisible(mPrePlayPosition)) {
            mPrePlayPosition = position;
            ViewHolder viewHolder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
//            viewHolder.video.openVideo();
        }
    }

    /**
     * 当前下标是否可见
     *
     * @param position
     * @return
     */
    private boolean isVisible(int position) {
        int firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition();
        return !(position < firstVisibleItem || position > lastVisibleItem);
    }

    @Override
    protected void onPause() {
        super.onPause();
        IjkVideoManager.getInstance().pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IjkVideoManager.getInstance().release();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View contentView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
            ViewHolder viewHolder = new ViewHolder(contentView);
            viewHolder.controller = new MediaController(parent.getContext());
            viewHolder.controller.setThumbRes(R.drawable.xxx2);
            viewHolder.controller.setFullScreenMode(MediaController.FULLSCREEN_VIEW);
            viewHolder.video.setMediaController(viewHolder.controller);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.video.setVideoPath(VIDEO_URL);
            holder.video.setPlayPosition(position);
            holder.controller.setPlayPosition(position);
        }

        @Override
        public int getItemCount() {
            return 10;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        IjkVideoView video;
        MediaController controller;

        ViewHolder(View itemView) {
            super(itemView);

            video = (IjkVideoView) itemView.findViewById(R.id.video_view);
        }
    }


}
