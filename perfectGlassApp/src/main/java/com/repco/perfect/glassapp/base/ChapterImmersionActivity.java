package com.repco.perfect.glassapp.base;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by chom on 4/16/15.
 */
public abstract class ChapterImmersionActivity extends ChapterActivity{
    public abstract int getLayoutId();
    protected AudioManager am;
    protected Slider mSlider;
    protected Slider.GracePeriod mGraceSlider = null;
    protected Slider.Determinate mDeterminate;
    private Timer mTimer;
    private ImageView mStatusIcon;
    private TextView mStatusText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        View clipCaptureView = getLayoutInflater().inflate(getLayoutId(),null);
        mStatusIcon = (ImageView) clipCaptureView.findViewById(R.id.status_icon);
        mStatusText = (TextView) clipCaptureView.findViewById(R.id.status_text);
        mTimer = new Timer();
        View contentView = new TuggableView(this,clipCaptureView);
        mSlider = Slider.from(contentView);

        setContentView(contentView);
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

    protected void finishGraceTimer(){
        if(mGraceSlider != null){
            mGraceSlider.cancel();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        finishGraceTimer();
        finishClipTimer();
    }

    protected void showStatusViews(String text, int iconID){
        mStatusText.setText(text);
        Drawable src = getResources().getDrawable(iconID);
        mStatusIcon.setImageDrawable(src);
        mStatusIcon.setVisibility(View.VISIBLE);
        mStatusText.setVisibility(View.VISIBLE);

    }

    protected void hideStatusView(){
        mStatusIcon.setVisibility(View.INVISIBLE);
        mStatusText.setVisibility(View.INVISIBLE);
    }
}
