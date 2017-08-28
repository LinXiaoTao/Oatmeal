## 简介
因为最近项目有视频播放的需求，所以就有了这个项目。视频的编解码功能是由 Bilibili 开源的 ijkplayer 处理的，这个项目只是基于 UI 层面的上封装。
> **注意：** 这个项目部分代码参考于 GSYVideoPlayer。

## 感谢
* [ijkplayer](https://github.com/Bilibili/ijkplayer)
* [AndroidVideoCache](https://github.com/danikula/AndroidVideoCache)
* [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer)
* [ENViews](https://github.com/codeestX/ENViews)

![](https://user-gold-cdn.xitu.io/2017/8/24/eb05fe5b0eb7cb5e061cf82970f67e29)

## 功能
* 播放器的基础功能（播放，暂停，快进等等）
* 支持列表播放，自动释放上一个播放器
* 视频封面图设置
* 提供两种视频全屏（设置屏幕的旋转方向和添加一个全屏的播放器）
* 手势滑动改变播放进度，屏幕亮度和音量
* 简单的 Wifi 网络检查
* 使用 AndroidVideoCache 实现的视频缓存
* 待续。。。

## 用法
在列表中使用，这里的代码是基于 `RecyclerView` 的：
``` java
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

```
这里需要注意的是，在列表中使用，需要开发者在 Item 不可见的时候，手动释放播放器，来节约内存。
``` java
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

```
而在单个播放器的页面使用，则相对要简单一点：
``` java
        mIjkVideoView = (IjkVideoView) findViewById(R.id.video_view);
        MediaController mediaController = new MediaController(this);
        mediaController.setShowThumb(true);
        mIjkVideoView.setMediaController(mediaController);

        mIjkVideoView.setVideoPath("http://baobab.wdjcdn.com/14564977406580.mp4");

```
需要注意的地方是，不管是列表中使用，还是只是单个播放器，都需要在页面关闭时，手动调用释放播放器：
``` java
    @Override
    protected void onDestroy() {
        super.onDestroy();
        IjkVideoManager.getInstance().release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IjkVideoManager.getInstance().pause();
    }

```
更多代码可以参见项目。

有这方面需要的朋友可以自由复制属于我的代码，唯一希望就是能给我提点 Bug，或者有更好的实现，多谢。
