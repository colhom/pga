package com.repco.perfect.glassapp.base;

import com.repco.perfect.glassapp.ClipService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class BaseBoundServiceActivity extends Activity {
	private ClipService.ClipServiceBinder mClipService;
    private CountDownLatch mServiceLatch;

    protected ClipService.ClipServiceBinder getClipService(){
        try {
            if(!mServiceLatch.await(10, TimeUnit.SECONDS)){
                throw new RuntimeException("Timed out waiting for clip service");
            }
        }catch(InterruptedException e){
            e.printStackTrace();
            finish();
        }
        return mClipService;
    }
	protected ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if(service instanceof ClipService.ClipServiceBinder){
				mClipService = (ClipService.ClipServiceBinder) service;
                mServiceLatch.countDown();
				onClipServiceConnected();
			}
		}
	};
	protected abstract void onClipServiceConnected();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mServiceLatch = new CountDownLatch(1);
        Intent service = new Intent(this,ClipService.class);
        startService(service);
		if(!bindService(service, mServiceConnection,0)){
			throw new RuntimeException("Couldn't bind to ClipService");
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
	}
}
