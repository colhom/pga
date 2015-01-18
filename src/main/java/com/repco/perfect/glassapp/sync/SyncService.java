package com.repco.perfect.glassapp.sync;

import com.repco.perfect.glassapp.R;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncService extends Service {
	
	private static SyncAdapter syncAdapter = null;
	public static final Object syncAdapterLock = new Object();
	@Override
	public void onCreate() {
		super.onCreate();
		synchronized(SyncService.syncAdapterLock){
			if(syncAdapter == null){
				syncAdapter = new SyncAdapter(getApplicationContext(), true);
			}
		}
	}
    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
