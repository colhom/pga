package com.repco.perfect.glassapp.base;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.R;

/**
 * Created by chom on 5/20/15.
 */
public abstract class ChapterSliderActivity extends ChapterActivity {

    protected AudioManager am;
    protected Slider mSlider;
    protected Slider.GracePeriod mGraceSlider = null;
    private CardBuilder mCb;
    private View mCv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        mCb = new CardBuilder(this, CardBuilder.Layout.MENU);
        mCv = mCb.getView();
        mSlider = Slider.from(mCv);

        setStatusIcon(getStatusIcon(true));
        setStatusText(getStatusText(true));

        setResult(Activity.RESULT_CANCELED);
        setContentView(mCv);

        mGraceSlider = mSlider.startGracePeriod(new Slider.GracePeriod.Listener() {
            @Override
            public void onGracePeriodEnd() {
                setStatusIcon(getStatusIcon(false));
                setStatusText(getStatusText(false));
                setResult(Activity.RESULT_OK);
                finish();

            }

            @Override
            public void onGracePeriodCancel() {
                am.playSoundEffect(Sounds.DISMISSED);
                finish();
            }
        });

    }
    protected abstract String getStatusText(boolean inProgress);

    protected abstract Drawable getStatusIcon(boolean inProgress);


    protected void setStatusText(String text){
        mCb.setText(text).getView(mCv, null);
    }

    protected void setStatusIcon(Drawable icon){
        mCb.setIcon(icon).getView(mCv,null);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){

            am.playSoundEffect(Sounds.DISALLOWED);

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onDestroy() {
        finishGraceTimer();
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishGraceTimer();
    }
    protected void finishGraceTimer(){
        if(mGraceSlider != null){
            mGraceSlider.cancel();
        }
    }
}
