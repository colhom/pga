package com.repco.perfect.glassapp.base;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.Slider;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by chom on 4/16/15.
 */
public abstract class ChapterSurfaceActivity extends ChapterActivity implements TextureView.SurfaceTextureListener{

    protected AudioManager am;
    protected Slider mSlider;
    protected Slider.GracePeriod mGraceSlider = null;
    protected Slider.Determinate mDeterminate;
    protected Slider.Indeterminate mLoading;
    private Timer mTimer;


    protected TextureView mTextureView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mTimer = new Timer();
        mTextureView = new TextureView(this);
        View cv = new TuggableView(this,mTextureView);
        mSlider = Slider.from(cv);
        setContentView(cv);

        mLoading = mSlider.startIndeterminate();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishClipTimer();
    }

    private TimerTask activeTask = null;
    protected void startClipTimer(final long duration, final long updateInterval){
        if(activeTask != null){
            System.err.println("[startClipTimer] overwriting uncleaned-up activetask before proceeding. finishClipTimer not called yet, will call now");
            finishClipTimer();
        }
        mDeterminate.setPosition(0.f);
        mDeterminate.show();
        activeTask = new TimerTask() {
            private long totalMS = 0;
            @Override
            public void run() {
                long newMS = totalMS + updateInterval;
                totalMS = Math.min(newMS,duration);
                mDeterminate.setPosition((float) totalMS / duration);
            }
        };
        mTimer.scheduleAtFixedRate(activeTask,0,updateInterval);
    }

    protected void finishClipTimer(){
        if(activeTask != null) {
            activeTask.cancel();
            mTimer.purge();
            activeTask = null;
            mDeterminate.setPosition(1.f);
            mDeterminate.hide();
        }
    }



}
