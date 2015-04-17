package com.repco.perfect.glassapp.base;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by chom on 4/16/15.
 */
public abstract class ChapterImmersionActivity extends ChapterActivity{

    protected AudioManager am;
    protected Slider mSlider;
    protected Slider.GracePeriod mGraceSlider = null;
    protected Slider.Determinate mDeterminate;
    protected Slider.Indeterminate mLoading;
    private Timer mTimer;
    private ImageView mStatusIcon;
    private TextView mStatusText;
    private View mContentView;
    private boolean mVoiceEnabled;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        View clipCaptureView = getLayoutInflater().inflate(getLayoutId(),null);
        mStatusIcon = (ImageView) clipCaptureView.findViewById(R.id.status_icon);
        mStatusText = (TextView) clipCaptureView.findViewById(R.id.status_text);
        mTimer = new Timer();
        mContentView = new TuggableView(this,clipCaptureView);
        mSlider = Slider.from(mContentView);
        setContentView(mContentView);


        hideStatusView();
        mLoading = mSlider.startIndeterminate();
    }
    public abstract int getLayoutId();
    public abstract int getVoiceMenuId();

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if(featureId == WindowUtils.FEATURE_VOICE_COMMANDS){
            return mVoiceEnabled;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if(featureId == WindowUtils.FEATURE_VOICE_COMMANDS){
            getMenuInflater().inflate(getVoiceMenuId(),menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishGraceTimer();
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

    protected void finishGraceTimer(){
        if(mGraceSlider != null){
            mGraceSlider.cancel();
        }
    }

    protected void showStatusViews(String text, int iconID){
        mStatusText.setText(text);
        Drawable src = getResources().getDrawable(iconID);
        mStatusIcon.setImageDrawable(src);
        mStatusIcon.setVisibility(View.VISIBLE);
        mStatusText.setVisibility(View.VISIBLE);
        mVoiceEnabled = true;
        mContentView.requestLayout();
    }

    protected void hideStatusView(){
        mStatusIcon.setVisibility(View.INVISIBLE);
        mStatusText.setVisibility(View.INVISIBLE);
        mVoiceEnabled = false;
        mContentView.requestLayout();
    }
}
