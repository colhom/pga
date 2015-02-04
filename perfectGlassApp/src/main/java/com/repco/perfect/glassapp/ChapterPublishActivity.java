package com.repco.perfect.glassapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.storage.StorageService;

/**
 * Created by chom on 2/2/15.
 */
public class ChapterPublishActivity extends BaseBoundServiceActivity{
    @Override
    protected void onClipServiceConnected() {
//        mClipService.publishChapter();
//        finish();
        Intent storageIntent = new Intent(this,StorageService.class);
        if(!bindService(storageIntent,mStorageService,0)){
            throw new RuntimeException("Could not connect to storage service");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mStorageService);
    }

    private ServiceConnection mStorageService  = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Message m = Message.obtain();
            m.what = StorageHandler.END_CHAPTER;
            try {
                (new Messenger(iBinder)).send(m);
            }catch(RemoteException e){
                throw new RuntimeException(e);
            }
            finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}

