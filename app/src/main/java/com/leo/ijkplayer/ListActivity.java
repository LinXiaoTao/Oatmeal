package com.leo.ijkplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.leo.player.media.IjkVideoManager;
import com.leo.player.media.controller.MediaController;
import com.leo.player.media.util.NetworkUtils;
import com.leo.player.media.videoview.IjkVideoView;


/**
 * Created on 2017/8/15 下午5:08.
 * leo linxiaotao1993@vip.qq.com
 */

public class ListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private Button mBtnDetail;
    private LinearLayoutManager mLinearLayoutManager;
    /** 上一次播放下标 */
    private int mPrePlayPosition = -1;

    private static final String VIDEO_URL = "http://baobab.wdjcdn.com/14564977406580.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mBtnDetail = (Button) findViewById(R.id.btn_detail);
        mBtnDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ListActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

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
    private void autoPlay() {
        if (!NetworkUtils.isWifiConnected(this)) {
            return;
        }
        int firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition();
        int position = (firstVisibleItem + lastVisibleItem) / 2;
        if (mPrePlayPosition != position && !isVisible(mPrePlayPosition)) {
            mPrePlayPosition = position;
            ViewHolder viewHolder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            viewHolder.controller.setPreparedPlay(true);
            viewHolder.video.openVideo();
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


    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private int mVideoWidth;
        private int mVideoHeight;

        private static final String[] IMGS = {"http://pic4.nipic.com/20091217/3279936_085313832375_2.jpg"
                , "https://b-ssl.duitang.com/uploads/item/201203/21/20120321162242_wdG8r.thumb.700_0.jpeg"
                , "http://imgsrc.baidu.com/baike/pic/item/f636afc379310a550e25d28ab34543a98226104e.jpg"
                , "http://c1.hoopchina.com.cn/uploads/star/event/images/141208/067438ed3ecdbdff6067d056315aed16ed2d3358.jpg"
                , "http://b.zol-img.com.cn/desk/bizhi/image/1/960x600/1348724812863.jpg"
                , "http://news.cnhubei.com/xw/ty/201612/W020161226410633306261.jpeg"
                , "http://img.zybus.com/uploads/allimg/141110/1-141110163R2.jpg"
                , "http://img4q.duitang.com/uploads/item/201406/26/20140626135444_jruft.jpeg"
                , "http://img0.imgtn.bdimg.com/it/u=2763254483,2728302026&fm=214&gp=0.jpg"
                , "http://cdn.duitang.com/uploads/item/201411/27/20141127184622_SEwUF.jpeg"
                , "http://pic19.nipic.com/20120320/8279160_140432483114_2.jpg"
                , "http://img1.gtimg.com/sports/pics/hv1/232/231/1859/120940612.jpg"
                , "http://img.zybus.com/uploads/allimg/141110/1-141110163K5.jpg"
                , "http://cdnq.duitang.com/uploads/item/201404/14/20140414001638_hratS.jpeg"};


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View contentView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
            ViewHolder viewHolder = new ViewHolder(contentView);
            viewHolder.controller = new MediaController(parent.getContext());
            viewHolder.controller.setEnableSlideBrightness(false);
            viewHolder.controller.setEnableSliderVolume(false);
            viewHolder.controller.setEnableSlidePosition(false);
//            viewHolder.controller.setFullScreenViewEnableSlideBrightness(false);
//            viewHolder.controller.setFullScreenViewEnableSlidePosition(false);
//            viewHolder.controller.setFullScreenViewEnableSliderVolume(false);
            viewHolder.controller.setFullScreenMode(MediaController.FULLSCREEN_VIEW);
            viewHolder.controller.setMute(true);
            viewHolder.controller.setShowBottomLayout(false);
            viewHolder.video.setMediaController(viewHolder.controller);
            mVideoWidth = parent.getContext().getResources().getDisplayMetrics().widthPixels;
            mVideoHeight = viewHolder.video.getLayoutParams().height;
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.video.setVideoPath(VIDEO_URL);
            holder.video.setPlayPosition(position);
            holder.controller.setPlayPosition(position);
            holder.controller.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.controller.toggleFullScreenView();
                }
            });
            if (holder.controller.getImgThumb() != null) {
                Glide
                        .with(holder.itemView.getContext())
                        .load(IMGS[position])
                        .apply(RequestOptions.centerCropTransform())
                        .into(holder.controller.getImgThumb());
            } else {
                Log.d(getClass().getSimpleName(), "Thumb ImgView is null");
            }

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
