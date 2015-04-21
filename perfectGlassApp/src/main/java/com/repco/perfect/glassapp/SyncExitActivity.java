package com.repco.perfect.glassapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.glass.widget.CardBuilder;
import com.repco.perfect.glassapp.base.TuggableView;

/**
 * Created by chom on 4/20/15.
 */
public class SyncExitActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.ALERT);

        card.setIcon(R.drawable.ic_warning_50);
        card.setText("Sync in progress!");
        card.setFootnote("Tap for options");

        setContentView(new TuggableView(this,card.getView()));
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.continue_sync_mi:
                finish();
                break;
            case R.id.close_sync_mi:
                Intent stopIntent = new Intent();
                stopIntent.setAction(ClipService.Action.CS_FORCE_STOP_SERVICE.toString());
                LocalBroadcastManager.getInstance(this).sendBroadcast(stopIntent);
                finish();
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sync_exit,menu);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            openOptionsMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
