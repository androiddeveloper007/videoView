package com.eostek.videoviewcontroller;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

public class PlayActivity extends AppCompatActivity {
    private VideoViewWithController mVideView;
    String uri = Environment.getExternalStorageDirectory().toString() + "/SNSD - Gee (Japanese Ver.) (Bugs Full HD).mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        mVideView = (VideoViewWithController) findViewById(R.id.videView);
        if(getIntent().hasExtra("uri")) {
            mVideView.setUpVideoUrl(getIntent().getStringExtra("uri"), "");
        } else {
            mVideView.setUpVideoUrl(uri, "");
        }
    }

    @Override
    protected void onResume() {
        mVideView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mVideView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mVideView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mVideView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mVideView.onConfigScreenChanged(newConfig);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mVideView.onWindowFocusChanged(mVideView.getMeasuredHeight());
    }

    @Override
    public void onBackPressed() {
        if(mVideView.isFullSceen) {
            mVideView.switchFullSceenOrPortaint();
        }else{
            super.onBackPressed();
        }
    }
}
