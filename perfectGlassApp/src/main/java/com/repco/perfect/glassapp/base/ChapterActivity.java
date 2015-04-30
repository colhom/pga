package com.repco.perfect.glassapp.base;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by chom on 2/11/15.
 */
public class ChapterActivity extends Activity {

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == Intent.ACTION_SCREEN_OFF){
                Log.i(getClass().getSimpleName(), "Screen off, will finish");
                finish();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(getClass().getSimpleName(), "Register screen receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, filter);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(getClass().getSimpleName(),"unregister screen receiver");
        unregisterReceiver(mScreenReceiver);
    }

}
