package com.repco.perfect.glassapp;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.glass.widget.CardBuilder;
import com.repco.perfect.glassapp.base.TuggableView;

/**
 * Created by hudson on 4/29/15.
 */
public class PublishWarningActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.ALERT);

        card.setIcon(R.drawable.ic_cloud_sad_150);
        card.setText("No WiFi connectivity");
        card.setFootnote("Swipe down to dismiss");

        setContentView(new TuggableView(this,card.getView()));
    }
}
