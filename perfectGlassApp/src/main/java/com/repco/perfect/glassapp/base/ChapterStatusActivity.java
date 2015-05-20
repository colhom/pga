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

    protected ImageView mBackgroundImageView,mStatusImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        View cv = getLayoutInflater().inflate(R.layout.chapter_status,null);
        mStatusImageView = (ImageView) cv.findViewById(R.id.status_icon);
        mBackgroundImageView = (ImageView) cv.findViewById(R.id.background_image_view);

        cv = new TuggableView(this,cv);

        setContentView(cv);

    }


    public abstract int getVoiceMenuId();
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            am.playSoundEffect(Sounds.SELECTED);
            openOptionsMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

}
