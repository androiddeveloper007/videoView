package com.eostek.videoviewcontroller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class VideoViewWithController extends FrameLayout {
    private UniVideoView itemVideoView;
    private Context mContext;
    private FrameLayout mSurfaceContainer;
    private TextView mCurrent;
    private SeekBar mProgress;
    private TextView mTotal;
    private ImageView mFullscreen;
    private LinearLayout mLayoutBottom;
    private ProgressBar mLoading;
    private ImageView mStart;
    private ImageView mThumb;
    private String videoUrl;
    public boolean isFullSceen = false;
    private String videoPic;
    private float mDownX;
    private float mDownY;
    private float currentVolume = -1;
    private float currentBriteness = -1;
    private int currentPosition = 0;
    public int mSceenWidth_2;
    public int mScreenHeight;
    private boolean isPause = true;
    private BritenessUtils britenessUtils;
    private VolumeUtils volumeUtils;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    public static final int TIME_PROGRESS = 1;
    public static final int SHOW_HIDE_CONTROLLER = 2;
    public static final int SHOW_HIDE_DURATION = 5000;
    public static final int THHOLD = 30;//最小滑动距离
    private float videoHorizontalHeight;
    @SuppressLint("HandlerLeak")
    private Handler UIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case TIME_PROGRESS:
                    showProgress();
                    UIHandler.sendEmptyMessage(TIME_PROGRESS);
                    break;
                case SHOW_HIDE_CONTROLLER:
                    showOrHideControler();
                    break;
            }
        }
    };

    public VideoViewWithController(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initView();
    }

    private void initView() {
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.jc_layout_base, this);
        itemVideoView = (UniVideoView) LayoutInflater.from(mContext).inflate(R.layout.layout_video_view, null);
        this.mSurfaceContainer = rootView.findViewById(R.id.surface_container);
        this.mCurrent = rootView.findViewById(R.id.current);
        this.mProgress = rootView.findViewById(R.id.progress);
        this.mTotal = rootView.findViewById(R.id.total);
        this.mFullscreen = rootView.findViewById(R.id.fullscreen);
        this.mLayoutBottom = rootView.findViewById(R.id.layout_bottom);
        this.mLoading = rootView.findViewById(R.id.loading);
        this.mStart = rootView.findViewById(R.id.start);
        this.mThumb = rootView.findViewById(R.id.thumb);

        if (mSurfaceContainer.getChildCount() > 0) {
            mSurfaceContainer.removeAllViews();
        }
        mSurfaceContainer.addView(itemVideoView);
        mSceenWidth_2 = ScreenUtils.getScreenWidth(mContext) / 2;
        mScreenHeight = ScreenUtils.getScreenHeight(mContext);
        britenessUtils = new BritenessUtils((Activity) mContext);
        volumeUtils = new VolumeUtils(mContext);
        bindListener();
    }

    private void bindListener() {
        mStart.setOnClickListener(onClickListener);
        mFullscreen.setOnClickListener(onClickListener);
        mSurfaceContainer.setOnTouchListener(onTouchListener);
        itemVideoView.setOnInfoListener(onInfoListener);
        itemVideoView.setOnErrorListener(onErrorListener);
        mProgress.setOnSeekBarChangeListener(onSeekBarChangeListener);
        itemVideoView.setOnCompletionListener(onCompletionListener);
        itemVideoView.setOnPreparedListener(onPreparedListener);
    }

    OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start:
                    if (itemVideoView.isPlaying()) {
                        isPause = true;
                        itemVideoView.pause();
                        changeStartImage(false);
                        UIHandler.removeMessages(TIME_PROGRESS);
                        UIHandler.removeMessages(SHOW_HIDE_CONTROLLER);
                    } else {
                        if (currentPosition > 0) {
                            itemVideoView.start();
                        } else {
                            start();
                        }
                        isPause = false;
                        changeStartImage(true);
                        UIHandler.sendEmptyMessageDelayed(SHOW_HIDE_CONTROLLER, SHOW_HIDE_DURATION);
                        UIHandler.sendEmptyMessage(TIME_PROGRESS);
                    }
                    break;
                case R.id.fullscreen:

                    switchFullSceenOrPortaint();

                    break;
            }
        }
    };

    OnTouchListener onTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float currentX = event.getX();
            float currentY = event.getY();
            int id = v.getId();
            if (id == R.id.surface_container) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mDownX = currentX;
                        mDownY = currentY;
                        mChangeVolume = false;
                        mChangePosition = false;
                        mChangeBrightness = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = currentX - mDownX;
                        float deltaY = currentY - mDownY;
                        float absDeltaX = Math.abs(deltaX);
                        float absDeltaY = Math.abs(deltaY);
                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                            if (absDeltaX > THHOLD || absDeltaY > THHOLD) {
                                if (absDeltaX >= THHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    mChangePosition = true;
                                } else {
                                    //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                                    if (mDownX < mSceenWidth_2) {//左侧改变亮度
                                        mChangeBrightness = true;
                                    } else {//右侧改变声音
                                        mChangeVolume = true;
                                    }
                                }
                            }
                        }
                        if (mChangePosition) {
                            changeVideoCurrentPosition(deltaX/2);//速度太快，改成1/2
                            mStart.setVisibility(GONE);
                        }
                        if (mChangeVolume) {
                            changeVideoVolume(deltaY);
                        }
                        if (mChangeBrightness) {
                            changeVideoBriteness(deltaY);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        endGesture();
                        if (mChangePosition) {
                            itemVideoView.seekTo((int) seekPosition);
                            long duration = itemVideoView.getDuration();
                            int progress = (int) (seekPosition * 100 / (duration == 0 ? 1 : duration));
                            mProgress.setProgress(progress);
                        }
                        if (mThumb.getVisibility() == GONE) {
                            UIHandler.sendEmptyMessage(SHOW_HIDE_CONTROLLER);
                        }
                        break;
                }
            }
            return true;
        }
    };

    MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            isPause = true;
            return false;
        }
    };

    MediaPlayer.OnInfoListener onInfoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    mLoading.setVisibility(VISIBLE);
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    mLoading.setVisibility(GONE);
                    break;
            }
            return true;
        }
    };

    MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            isPause = true;
            //移除当前的状态
            UIHandler.removeMessages(TIME_PROGRESS);
            UIHandler.removeMessages(SHOW_HIDE_CONTROLLER);
            mLayoutBottom.setVisibility(VISIBLE);
            mStart.setVisibility(VISIBLE);
            mThumb.setVisibility(VISIBLE);
            mStart.setImageResource(R.drawable.jc_play_normal);
            currentPosition = 0;
            mProgress.setProgress(0);
            itemVideoView.stopPlayback();
            setUpVideoUrl(videoUrl, videoPic);
        }
    };

    private int mVideoWidth;
    private int mVideoHeight;
    //缓冲回掉
    MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {
            mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    // 获得当前播放时间和当前视频的长度
                    // 设置进度条的次要进度，表示视频的缓冲进度
                    int duration = mp.getDuration();
                    float currentPercent = percent * duration / 100;
                    mProgress.setSecondaryProgress((int) currentPercent);
                }
            });
            mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    changeVideoSize(mVideoWidth,mVideoHeight);
                }
            });
        }
    };

    public void changeVideoSize(int videoWidth,int videoHeight) {
        if (itemVideoView != null) {
            // 根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
            float max;
            if (getResources().getConfiguration().orientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                // 竖屏模式下按视频宽度计算放大倍数值
                max = Math.max((float) videoWidth / (float) ScreenUtils.getScreenWidth(mContext),
                        (float) videoHeight / (float) ScreenUtils.getScreenHeight(mContext));
            } else {
                // 横屏模式下按视频高度计算放大倍数值
                max = Math.max(((float) videoWidth / (float) ScreenUtils.getScreenHeight(mContext)),
                        (float) videoHeight / (float) ScreenUtils.getScreenWidth(mContext));
            }

            // 视频宽高分别/最大倍数值 计算出放大后的视频尺寸
            videoWidth = (int) Math.ceil((float) videoWidth / max);
            videoHeight = (int) Math.ceil((float) videoHeight / max);

            // 无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
            itemVideoView.setLayoutParams(new FrameLayout.LayoutParams(videoWidth, videoHeight, Gravity.CENTER));
        }
    }

    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                if (itemVideoView != null) {
                    itemVideoView.seekTo(progress);
                }
            }

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    //更改屏幕的声音
    private void changeVideoVolume(float deltaY) {
        if (currentVolume == -1) {
            currentVolume = volumeUtils.getCurrentVolume();
        }
        float maxVolume = volumeUtils.getMaxVolume();
        float percent = (-deltaY / mScreenHeight) * maxVolume;
        float mCurrentVolume = Math.min(currentVolume + percent, volumeUtils.getMaxVolume());
        int volumePercent = (int) (currentVolume * 100 / maxVolume + -deltaY * 100 * 2 / mScreenHeight);
        if (mCurrentVolume <= 0) {
            mCurrentVolume = 0f;
        }
        showVolumeDialog(-deltaY, volumePercent);
        volumeUtils.setCurrentVolume((int) mCurrentVolume);

    }

    float seekPosition;

    //更改当前视频进度
    private void changeVideoCurrentPosition(float deltaX) {
        float duration = itemVideoView.getDuration();
        float curPostion = itemVideoView.getCurrentPosition();
        seekPosition = (int) (curPostion + deltaX * duration / mSceenWidth_2 * 2);
        if (seekPosition >= duration) {
            seekPosition = duration;
        }
        if (seekPosition <= 0) {
            seekPosition = 0;
        }
        showProgressDialog(deltaX, TimeUtils.stringForTime((int) seekPosition), (int) seekPosition, TimeUtils.stringForTime((int) duration), (int) duration);
//        itemVideoView.seekTo((int) seekPosition);

    }

    //更改屏幕的亮度
    private void changeVideoBriteness(float deltaY) {
        if (currentBriteness == -1) {
            currentBriteness = britenessUtils.getCurrentBrite();
        }
        float mCurrentBriteness = Math.min(currentBriteness + (-deltaY / mScreenHeight), britenessUtils.MAX_BRITEN);
        if (mCurrentBriteness <= 0.1) {
            mCurrentBriteness = 0.1f;
        }
        int britenessPercent = (int) (mCurrentBriteness * 100 / britenessUtils.MAX_BRITEN + (-deltaY) * 100 / mScreenHeight);
        showBritenessDialog(-deltaY, britenessPercent);
        britenessUtils.setCurrentBrite(mCurrentBriteness);
    }

    //显示或者隐藏mediaController
    private void showOrHideControler() {
        if (currentPosition != 0) {
            if (mLayoutBottom.getVisibility() == View.VISIBLE) {
                mLayoutBottom.setVisibility(GONE);
                mStart.setVisibility(GONE);
                UIHandler.removeMessages(SHOW_HIDE_CONTROLLER);
            } else if (mLayoutBottom.getVisibility() == View.GONE) {
                mLayoutBottom.setVisibility(VISIBLE);
                mStart.setVisibility(VISIBLE);
                UIHandler.removeMessages(SHOW_HIDE_CONTROLLER);
                UIHandler.sendEmptyMessageDelayed(SHOW_HIDE_CONTROLLER, SHOW_HIDE_DURATION);
            }
        }
    }

    //更新当前进度
    private void showProgress() {
        if (currentPosition != itemVideoView.getCurrentPosition()) {
            if (mLoading.getVisibility() == View.VISIBLE) {
                mLoading.setVisibility(GONE);
            }
        }
        currentPosition = itemVideoView.getCurrentPosition();
        int totalPosition = itemVideoView.getDuration();
        Log.e("-----------", currentPosition + "");
        mCurrent.setText(TimeUtils.stringForTime(currentPosition));
        mTotal.setText(TimeUtils.stringForTime(totalPosition));
        mProgress.setMax(totalPosition);
        mProgress.setProgress(currentPosition);
    }

    //更改是否播放按钮图片
    private void changeStartImage(boolean isPlaying) {
        if (isPlaying) {
            mStart.setImageResource(R.drawable.jc_pause_normal);
        } else {
            mStart.setImageResource(R.drawable.jc_play_normal);
        }
    }
    //播放
    public void start() {
        if (TextUtils.isEmpty(videoUrl)) {
            throw new NullPointerException("视频地址不能为空");
        }
        itemVideoView.start();
        mLoading.setVisibility(VISIBLE);
        mThumb.setVisibility(GONE);
    }

    //设置videourl
    public void setUpVideoUrl(String videoUrl, String videoPic) {
        this.videoUrl = videoUrl;
        this.videoPic = videoPic;
//        itemVideoView.setVideoPath(videoUrl);
        itemVideoView.setVideoURI(Uri.parse(videoUrl));
//        Glide.with(mContext).load(videoPic).into(mThumb);
    }

    public void switchFullSceenOrPortaint() {
        Activity activity = (Activity) mContext;
        if (isFullSceen) {
//竖屏
            isFullSceen = false;
            mFullscreen.setImageResource(R.drawable.jc_enlarge);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        } else {
//横屏
            isFullSceen = true;
            mFullscreen.setImageResource(R.drawable.jc_shrink);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        }
    }
    private void endGesture() {
        dismissProgressDialog();
        dismissVolumeDialog();
        dismissBritenessDialog();
        mStart.setVisibility(VISIBLE);
        currentBriteness = -1;
        currentVolume = -1;
        currentPosition = 0;
    }

    //展示当前的进度
    protected View processDialog;;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;
    public void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        processDialog = findViewById(R.id.processDialog);
        mDialogProgressBar = findViewById(R.id.duration_progressbar);
        mDialogSeekTime = findViewById(R.id.tv_current);
        mDialogTotalTime = findViewById(R.id.tv_duration);
        mDialogIcon = findViewById(R.id.duration_image_tip);
        processDialog.setVisibility(View.VISIBLE);

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(" / " + totalTime);
        mDialogProgressBar.setProgress(totalTimeDuration <= 0 ? 0 : (seekTimePosition * 100 / totalTimeDuration));
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.jc_forward_icon);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.jc_backward_icon);
        }

    }

    public void dismissProgressDialog() {
        if (processDialog != null) {
            processDialog.setVisibility(GONE);
        }
    }

    protected View mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;
    public void showVolumeDialog(float deltaY, int volumePercent) {
        if (mVolumeDialog == null) {
            mVolumeDialog = findViewById(R.id.volumeDia);
            mDialogVolumeProgressBar = findViewById(R.id.volume_progressbar);
        }
        if (mVolumeDialog != null) mVolumeDialog.setVisibility(VISIBLE);
        mDialogVolumeProgressBar.setProgress(volumePercent);
    }

    public void dismissVolumeDialog() {
        if(mVolumeDialog!=null)
        mVolumeDialog.setVisibility(GONE);
    }

    /**
     * 左侧亮度
     */
    protected View mBritenessDialog;
    protected ProgressBar mDialogBritenessProgressBar;
    public void showBritenessDialog(float deltaY, int volumePercent) {
        if (mBritenessDialog == null) {
            mBritenessDialog = findViewById(R.id.brightDia);
            mDialogBritenessProgressBar =  findViewById(R.id.bright_progressbar);
        }
        if (mBritenessDialog != null) mBritenessDialog.setVisibility(VISIBLE);
        mDialogBritenessProgressBar.setProgress(volumePercent);
    }

    public void dismissBritenessDialog() {
        if(mBritenessDialog!=null) mBritenessDialog.setVisibility(GONE);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        if (itemVideoView != null && itemVideoView.isPlaying()) {
            currentPosition = itemVideoView.getCurrentPosition();
            isPause = false;
        }
        return super.onSaveInstanceState();
    }

    public void onResume() {
        if (itemVideoView != null && currentPosition > 0 && !isPause) {
            itemVideoView.start();
            itemVideoView.seekTo(currentPosition);
            UIHandler.sendEmptyMessage(TIME_PROGRESS);
            UIHandler.sendEmptyMessage(SHOW_HIDE_CONTROLLER);
        }
    }

    public void onPause() {
        if (itemVideoView != null && itemVideoView.isPlaying()) {
            currentPosition = itemVideoView.getCurrentPosition();
        }
    }


    public void onStop() {
        if (itemVideoView != null && itemVideoView.isPlaying()) {
            UIHandler.removeMessages(TIME_PROGRESS);
            UIHandler.removeMessages(SHOW_HIDE_CONTROLLER);
            itemVideoView.pause();
        }
    }


    public void onDestroy() {
        if (itemVideoView != null) {
            itemVideoView.stopPlayback();
            itemVideoView = null;
        }
        UIHandler.removeCallbacksAndMessages(null);
        UIHandler = null;
    }

    public void onConfigScreenChanged(Configuration newConfig) {
        resetLayoutParams(newConfig.orientation);
    }

    public void onWindowFocusChanged(int height) {
        videoHorizontalHeight = height;
    }

    //横屏切换到竖屏时候修改布局大小
    private void resetLayoutParams(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) this.getLayoutParams();
            params.height = ScreenUtils.getScreenHeight(mContext);
            params.width = ScreenUtils.getScreenWidth(mContext);
            this.setLayoutParams(params);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) this.getLayoutParams();
            params.height = (int) videoHorizontalHeight;
            params.width = ScreenUtils.getScreenWidth(mContext);
            this.setLayoutParams(params);
        }
    }
}
