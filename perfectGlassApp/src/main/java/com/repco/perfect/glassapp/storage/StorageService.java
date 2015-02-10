package com.repco.perfect.glassapp.storage;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Messenger;

public class StorageService extends Service {

	
	private StorageHandler mStorageHandler;
	@Override
	public void onCreate() {
		super.onCreate();
		mStorageHandler = new StorageHandler(this);
		
	}
	@Override
	public IBinder onBind(Intent arg0) {
		return mStorageHandler.mMessenger.getBinder();
	}

}
