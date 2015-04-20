package com.repco.perfect.glassapp.sync;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.repco.perfect.glassapp.BuildConfig;
import com.repco.perfect.glassapp.base.Storable;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.storage.StorageService;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class SyncTask{

	private static final String LTAG = SyncTask.class.getSimpleName();


    private final Context context;
    public SyncTask(Context context){
        this.context = context;
    }

    protected final Context getContext(){
        return this.context;
    }


	private Messenger mStorageMessenger;
	private final ServiceConnection mStorageConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(LTAG, "storage service connection disconnected");
			mStorageMessenger = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(LTAG, "storage service connection connected");
			mStorageMessenger = new Messenger(service);
            doneBarrier = new CountDownLatch(1);
			serviceBarrier.countDown();
		}
	};

	private CountDownLatch serviceBarrier;
    private CountDownLatch doneBarrier;
    private boolean syncStorable(Storable storable){
        if (BuildConfig.DEBUG && !storable.dirty) {
            throw new RuntimeException(
                    "We got a clean storable in the SyncAdapter! "
                            + storable);
        }

        Log.i(LTAG, "sync storable " + storable);


        if(storable.doSync()){
            Log.i(LTAG, "Sync successful, writing back storable");
            storable.dirty = false;
            Message storemsg = Message.obtain();
            storemsg.what = StorageHandler.PUSH_STORABLE;
            storemsg.obj = storable;

            try {
                mStorageMessenger.send(storemsg);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            return true;
        }else{
            Log.w(LTAG,"Sync failed, will abort sync");
            return false;
        }
    }
    private void finishSync(){
        Log.i(LTAG,"finishSync()");
        Message msg = Message.obtain();
        msg.what = StorageHandler.CLEANUP_PUBLISHED_CHAPTERS;
        try{
            mStorageMessenger.send(msg);
        }catch(RemoteException e){
            throw new RuntimeException(e);
        }
        doneBarrier.countDown();
        getContext().unbindService(mStorageConnection);
    }


    private void continueSync(){
        Log.i(LTAG,"continueSync()");
        Message msg = Message.obtain();
        msg.what = StorageHandler.GET_NEXT_STORABLE;
        msg.obj = null;
        msg.replyTo = mStorageReplyMessenger;
        try {
            // this should result in in a release
            mStorageMessenger.send(msg);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
    private Messenger mStorageReplyMessenger;

    public static final int
            SYNC_SUCCESS = 0,
            SYNC_NO_NETWORK = 1,
            SYNC_ONLY_METERED=2,
            SYNC_ERROR =3,
            SYNC_IN_PROGRESS=4,
            SYNC_INITIALIZING=5
    ;


    private int doSync(){
        Log.i(LTAG, "Sync started");

        ConnectivityManager connManager = (ConnectivityManager) getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo mWifi = connManager.getActiveNetworkInfo();

        if (mWifi == null) {
            Log.i(LTAG, "No active network connection, skipping sync");
            return SYNC_NO_NETWORK;
        }

        if (connManager.isActiveNetworkMetered()) {
            Log.i("LTAG",
                    "Active network connection is metered, skipping sync");
            return SYNC_ONLY_METERED;
        }



        // TODO: this is gross, architect your software properly plz
        final Semaphore syncBarrier = new Semaphore(1);
        final Boolean done = false;
        final Handler.Callback mReplyCallback = new Handler.Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                boolean delivered = false;

                switch (msg.what) {
                    case StorageHandler.RECEIVE_NEXT_STORABLE:
                        Log.i(LTAG,"RECEIVE NEXT STORABLE "+msg.obj);
                        if (msg.obj == null) {
                            Log.i(LTAG, "receive next storable [null]");
                            finishSync();

                        } else{
                            Storable s = (Storable) msg.obj;

                            if (!syncStorable(s)) {
                                finishSync();
                            }else{
                                continueSync();
                            }
                        }
                        delivered = true;
                        break;
                    default:
                        Log.e(LTAG, "unknown message type " + msg.what);
                        break;

                }
                return delivered;
            }
        };

        HandlerThread ht = new HandlerThread("StorageReplyHandler");
        ht.start();
        Handler storageReplyHandler = new Handler(ht.getLooper(),
                mReplyCallback);
        mStorageReplyMessenger = new Messenger(
                storageReplyHandler);

        serviceBarrier = new CountDownLatch(1);
        if (!getContext().bindService(
                new Intent(getContext(), StorageService.class),
                mStorageConnection, Context.BIND_AUTO_CREATE)) {
            throw new RuntimeException("Could not bind to storage service");
        }

        try {
            serviceBarrier.await();
        } catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        }

        continueSync();
        Log.i(LTAG,"synced started, awaiting doneBarrier");

        try{
            doneBarrier.await();
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }


        return SYNC_SUCCESS;
    }

    private boolean isSyncing = false;

    private synchronized boolean shouldSync(){
        if(!isSyncing){
            isSyncing = true;
            return true;
        }

        return false;
    }

    private int mSyncStatus = SYNC_INITIALIZING;


    private Thread mThread;

    public void requestSync() {
        if (!shouldSync()) {
            Log.i(LTAG, "should not sync");
            return;
        }
        mThread = new Thread(new Runnable() {

            public void run() {


                mSyncStatus = SYNC_IN_PROGRESS;
                try

                {
                    mSyncStatus = doSync();
                } catch (Exception e)

                {
                    Log.e(LTAG, "Error in sync block");
                    e.printStackTrace();
                    mSyncStatus = SYNC_ERROR;
                } finally

                {
                    isSyncing = false;
                }
            }
        });

        mThread.start();

    }
}
