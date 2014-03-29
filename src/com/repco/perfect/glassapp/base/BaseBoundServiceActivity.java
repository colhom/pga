package com.repco.perfect.glassapp.base;

import com.repco.perfect.glassapp.ClipService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public abstract class BaseBoundServiceActivity extends Activity {
	protected ClipService.ClipServiceBinder mClipService;
	protected ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			finish();
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if(service instanceof ClipService.ClipServiceBinder){
				mClipService = (ClipService.ClipServiceBinder) service;
				onClipServiceConnected();
			}
		}
	};
	protected abstract void onClipServiceConnected();
	@Override
	protected final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bindService(new Intent(this,ClipService.class), mServiceConnection, 0);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
	}
}
