package com.repco.perfect.glassapp;

import java.io.File;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
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
    private MediaPlayer openDinkSound; 
    private MediaPlayer closeDinkSound;
    public class ClipServiceBinder extends Binder{
    
    	public void stop(){
    		ClipService.this.stopSelf();
    	}
    	
    	public void recordClip(){	
    		Intent captureIntent = new Intent(ClipService.this,ClipCaptureActivity.class);
            captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getApplication().startActivity(captureIntent);
            openDinkSound.start();
    	}
    	
    	public void abortClip(String outputPath){
    		File f = new File(outputPath);
    		f.delete();
    		closeDinkSound.start();
    	}
    	
    	public void saveClip(String outputPath){
    		closeDinkSound.start();
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
        if (closeDinkSound == null){
        	closeDinkSound = MediaPlayer.create(this, R.raw.fart);
        }
        if (openDinkSound == null){
        	openDinkSound = MediaPlayer.create(this, R.raw.fart_open);
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
        if(openDinkSound != null){
        	openDinkSound.release();
        }
        if(closeDinkSound != null){
        	closeDinkSound.release();
        }
        super.onDestroy();
    }

}
