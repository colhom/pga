package com.repco.perfect.glassapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.repco.perfect.glassapp.base.TuggableView;

/**
 * Created by hudson on 4/29/15.
 */
public class PublishWarningActivity extends Activity {


    private AudioManager mAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.ALERT);

        card.setIcon(R.drawable.ic_cloud_sad_150);
        card.setText("No WiFi connectivity");
        card.setFootnote("Tap to view connectivity status");

        setContentView(new TuggableView(this,card.getView()));
        mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            mAudio.playSoundEffect(Sounds.TAP);
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
