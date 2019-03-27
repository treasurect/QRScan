package com.treasure.qrscan.ui.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.RelativeLayout;

import com.treasure.qrscan.R;
import com.treasure.qrscan.utils.MediaHelper;
import com.treasure.qrscan.utils.ScreenUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoPlayerView extends RelativeLayout {
    private static final String TAG = "VideoPlayerView";
    @BindView(R.id.texture_view)
    public TextureView videoView;
    @BindView(R.id.media_controller)
    public VideoControllerView mediaController;

    public MediaPlayer mPlayer;
    private Surface mSurface;

    public boolean hasPlay;//是否播放了
    private Context context;

    public VideoPlayerView(Context context) {
        this(context, null);
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initView();
    }

    //初始化布局
    private void initView() {
        View view = View.inflate(getContext(), R.layout.layout_player_view, this);
        ButterKnife.bind(this,view);

        LayoutParams layoutParams = (LayoutParams) videoView.getLayoutParams();
        layoutParams.width = ScreenUtils.getScreenWidth(context) - ScreenUtils.dip2px(context, 32);
        layoutParams.height = ScreenUtils.getScreenHeight(context) - ScreenUtils.dip2px(context, 143);
        layoutParams.addRule(Gravity.CENTER);
        videoView.setLayoutParams(layoutParams);
        mediaController.setLayoutParams(layoutParams);

        //设置视频圆角
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            videoView.setOutlineProvider(new TextureVideoViewOutlineProvider(context, ScreenUtils.dip2px(context, 6)));
            videoView.setClipToOutline(true);
        }

        initViewDisplay();
        //把VideoPlayer对象传递给VideoMediaController
        mediaController.setVideoPlayer(this);

        //进行TextureView控件创建的监听
        videoView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

        //创建完成  TextureView才可以进行视频画面的显示
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
           // Log.i(TAG,"onSurfaceTextureAvailable");
            mSurface = new Surface(surface);//连接对象（MediaPlayer和TextureView）
            play(url);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
           // Log.i(TAG,"onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
           // Log.i(TAG,"onSurfaceTextureDestroyed");
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
           // Log.i(TAG,"onSurfaceTextureUpdated");
        }
    };

    //视频播放（视频的初始化）
    private void play(String url){
        try {
            mPlayer = MediaHelper.getInstance();
            mPlayer.reset();
            mPlayer.setDataSource(url);
            //让MediaPlayer和TextureView进行视频画面的结合
            mPlayer.setSurface(mSurface);
            //设置监听
            mPlayer.setOnBufferingUpdateListener(onBufferingUpdateListener);
            mPlayer.setOnCompletionListener(onCompletionListener);
            mPlayer.setOnErrorListener(onErrorListener);
            mPlayer.setOnPreparedListener(onPreparedListener);
            mPlayer.setScreenOnWhilePlaying(true);//在视频播放的时候保持屏幕的高亮
            //异步准备
            mPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //准备完成监听
    private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            //隐藏视频加载进度条
            mediaController.setPbLoadingVisiable(View.GONE);
            //进行视频的播放
            MediaHelper.play();
            hasPlay = true;
            //隐藏标题
            mediaController.delayHideTitle();
            //设置视频的总时长
            mediaController.setDuration(mPlayer.getDuration());
            //更新播放的时间和进度
            mediaController.updatePlayTimeAndProgress();
        }
    };

    //错误监听
    private MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return true;
        }
    };

    //完成监听
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            //视频播放完成
            mediaController.showPlayFinishView();
        }
    };

    //缓冲的监听
    private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
//            Log.i(TAG,"percent:"+percent);
            mediaController.updateSeekBarSecondProgress(percent);
        }
    };


    //初始化控件的显示状态
    public void initViewDisplay() {
        videoView.setVisibility(View.GONE);
        mediaController.initViewDisplay();
    }

    //设置视频播放界面的显示
    public void setVideoViewVisiable(int visible) {
        videoView.setVisibility(View.VISIBLE);
    }
    private String url;
    public void setPlayData(String url) {
        this.url = url;
    }
}
