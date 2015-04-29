package com.repco.perfect.glassapp.base;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.R;

/**
 * Created by chom on 4/17/15.
 */
public abstract class ChapterStatusActivity extends ChapterActivity{

    protected AudioManager am;
    protected Slider mSlider;
    protected Slider.GracePeriod mGraceSlider = null;

    protected Slider.Indeterminate mLoading;

    private ImageView mStatusImageView;
    private TextView mStatusTextView;
    protected ImageView mBackgroundImageView;

    private boolean mMenuEnabled;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        View cv = getContentView();
        mStatusImageView = (ImageView) cv.findViewById(R.id.status_icon);
        mStatusTextView = (TextView) cv.findViewById(R.id.status_text);
        mBackgroundImageView = (ImageView) cv.findViewById(R.id.background_image_view);

        cv = new TuggableView(this,cv);
        mSlider = Slider.from(cv);
        setContentView(cv);

        //StatusViews default hidden, just do showStatusViews just after super.onCreate
        //in your child class
        hideStatusView();
        mLoading = mSlider.startIndeterminate();
    }

    public abstract View getContentView();
    public int getVoiceMenuId(){ return 0;};
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            if(mMenuEnabled) {
                am.playSoundEffect(Sounds.SELECTED);
                openOptionsMenu();
            }else{
                am.playSoundEffect(Sounds.DISALLOWED);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if(featureId == WindowUtils.FEATURE_VOICE_COMMANDS){
            return mMenuEnabled;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(!mMenuEnabled){
            return false;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if(featureId == WindowUtils.FEATURE_VOICE_COMMANDS){
            if(getVoiceMenuId() != 0) {
                getMenuInflater().inflate(getVoiceMenuId(), menu);
            }
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishGraceTimer();
    }
    @Override
    public void onBackPressed() {
        if(mGraceSlider == null) {
            super.onBackPressed();
        }else{
            am.playSoundEffect(Sounds.DISMISSED);
            mGraceSlider.cancel();
        }
    }
    protected void finishGraceTimer(){
        if(mGraceSlider != null){
            mGraceSlider.cancel();
        }
    }
    //If actionText is empty string, voice menu is enabled. kinda funky
    protected void showStatusViews(String actionText, int iconID){
        mMenuEnabled = actionText.isEmpty();
        Drawable src = getResources().getDrawable(iconID);
        mStatusImageView.setImageDrawable(src);
        mStatusImageView.setVisibility(View.VISIBLE);
        if (actionText == ""){
            mStatusTextView.setVisibility(View.GONE);
        }else{
            mStatusTextView.setText(actionText);
            mStatusTextView.setVisibility(View.VISIBLE);
        }


    }

    protected void hideStatusView(){
        mStatusImageView.setVisibility(View.INVISIBLE);
        mStatusTextView.setVisibility(View.INVISIBLE);
        mMenuEnabled = false;
    }
}
