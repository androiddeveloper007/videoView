package com.eostek.videoviewcontroller;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

//横竖切换时大小变化
public class UniVideoView extends VideoView {
    public UniVideoView(Context context) {
        super(context);
    }

    public UniVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UniVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(0, widthMeasureSpec);
        int height = getDefaultSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
