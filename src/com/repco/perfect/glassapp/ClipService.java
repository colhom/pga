package com.repco.perfect.glassapp;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

public class ClipService extends Service {

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;

    @Override
    public void onCreate() {
        super.onCreate();
        mTimelineManager = TimelineManager.from(this);
    }
    
    public class ClipServiceBinder extends Binder{
    	public void stop(){
    		ClipService.this.stopSelf();
    	}
    	
    	public void recordClip(){
    		
    		Intent captureIntent = new Intent(ClipService.this,ClipCaptureActivity.class);
            captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getApplication().startActivity(captureIntent);
    	}
    }
    
    private final ClipServiceBinder mBinder = new ClipServiceBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = mTimelineManager.createLiveCard("perfect");

            mLiveCard.setDirectRenderingEnabled(false);
            
            mLiveCard.setViews(new RemoteViews(getPackageName(), R.layout.dash));
            
     
            Intent menuIntent = new Intent(this, LaunchMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish(LiveCard.PublishMode.SILENT);
        }
        mBinder.recordClip();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {

            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }

}
