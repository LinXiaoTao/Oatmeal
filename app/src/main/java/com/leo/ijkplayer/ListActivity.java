package com.leo.ijkplayer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leo.ijkplayer.media.IjkVideoView;
import com.leo.ijkplayer.media.controller.MediaController;

/**
 * Created on 2017/8/15 下午5:08.
 * leo linxiaotao1993@vip.qq.com
 */

public class ListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;

    private static final String VIDEO_URL = "http://baobab.wdjcdn.com/14564977406580.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);


        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter());

    }

    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View contentView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
            ViewHolder viewHolder = new ViewHolder(contentView);
            viewHolder.controller = new MediaController(parent.getContext());
            viewHolder.video.setMediaController(viewHolder.controller);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.video.setVideoPath(VIDEO_URL);

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
