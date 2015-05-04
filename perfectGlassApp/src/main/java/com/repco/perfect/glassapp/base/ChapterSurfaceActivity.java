package com.repco.perfect.glassapp.base;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.R;

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
    protected ImageView mIconView;
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Below is what we tried to use to fix the grey interstitial loading screen issue.

//        ViewGroup texture = (ViewGroup) getLayoutInflater().inflate(R.layout.surface_layout, null);
//        mTextureView = (TextureView) texture.findViewById(R.id.texture_view);
//        mIconView = (ImageView) texture.findViewById(R.id.texture_icon);
//
//        final View cv = new TuggableView(this,R.layout.surface_layout);

//        mTextureView = new TextureView(this);
//
//        mTextureView = (TextureView) findViewById(R.id.texture_view);
//        mIconView = (ImageView) findViewById(R.id.texture_icon);
//        mTextureView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        texture.addView(mTextureView);
//        ViewTreeObserver vto = cv.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                mSlider = Slider.from(cv);
//                mLoading = mSlider.startIndeterminate();
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
//        mIconView.setVisibility(View.GONE);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            am.playSoundEffect(Sounds.DISALLOWED);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
