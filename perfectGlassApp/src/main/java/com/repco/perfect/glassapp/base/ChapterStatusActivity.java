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
        View cv = getLayoutInflater().inflate(getLayoutId(),null);
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

    public abstract int getLayoutId();
    public int getVoiceMenuId(){ return 0;};

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

    protected void finishGraceTimer(){
        if(mGraceSlider != null){
            mGraceSlider.cancel();
        }
    }

    protected void showStatusViews(String text, int iconID){
        mStatusTextView.setText(text);
        Drawable src = getResources().getDrawable(iconID);
        mStatusImageView.setImageDrawable(src);
        mStatusImageView.setVisibility(View.VISIBLE);
        mStatusTextView.setVisibility(View.VISIBLE);
        mMenuEnabled = true;
    }

    protected void hideStatusView(){
        mStatusImageView.setVisibility(View.INVISIBLE);
        mStatusTextView.setVisibility(View.INVISIBLE);
        mMenuEnabled = false;
    }
}
