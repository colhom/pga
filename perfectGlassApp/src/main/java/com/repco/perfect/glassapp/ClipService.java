package com.repco.perfect.glassapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.storage.StorageService;
import com.repco.perfect.glassapp.ui.LiveCardBindings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;

public class ClipService extends Service {
	private static final String LTAG = ClipService.class.getSimpleName();
	private LiveCard mLiveCard;


	private Messenger mStorageMessenger;
	private Messenger mStorageReplyMessenger;
	private Handler mStorageReplyHandler;


	public static  String AUTHORITY,ACCOUNT_TYPE;
	public static final String ACCOUNT_NAME = "dummy_perfect_account";
	public static Account ACCOUNT;

	private final ServiceConnection mStorageConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Log.i(LTAG, "Storage service connection disconnected");
			mStorageMessenger = null;
		}

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			Log.i(LTAG, "Storage service connection connected");
			mStorageMessenger = new Messenger(binder);
            storageLatch.countDown();
            sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,false);
		}
	};

    private CountDownLatch storageLatch;
	@Override
	public void onCreate() {
		super.onCreate();
		AUTHORITY = getResources().getString(R.string.provider_type);
		ACCOUNT_TYPE = getResources().getString(R.string.account_type);


		AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

		if(accounts.length == 0){
			Log.i(LTAG,"creating new account");
			ACCOUNT = new Account(ACCOUNT_NAME,ACCOUNT_TYPE);
			ContentResolver.setIsSyncable(ACCOUNT, AUTHORITY, 1);
			ContentResolver.setSyncAutomatically(ACCOUNT, AUTHORITY, true);
			if (!accountManager.addAccountExplicitly(ACCOUNT, null, null)) {

				System.err.println("Add account failed!");
			}
		}else if(accounts.length == 1){
			Log.i(LTAG,"Using existing account");
			ACCOUNT = accounts[0];

		}else{

			throw new RuntimeException("We have too many ("+accounts.length+") accounts!");
		}
		ContentResolver.setIsSyncable(ACCOUNT, AUTHORITY, 1);
		ContentResolver.setSyncAutomatically(ACCOUNT, AUTHORITY, true);


		ContentResolver.addPeriodicSync(ACCOUNT, AUTHORITY, new Bundle(), 60*5);

        storageLatch = new CountDownLatch(1);
        mStorageIntent = new Intent(this, StorageService.class);
        startService(mStorageIntent);
		if(!bindService(mStorageIntent, mStorageConnection, 0)){
			throw new RuntimeException("Could not bind to storage service");
		}

		// rx
		HandlerThread ht = new HandlerThread("StorageReplyHandler");
		ht.start();
		mStorageReplyHandler = new Handler(ht.getLooper(), mReplyCallback);
		mStorageReplyMessenger = new Messenger(mStorageReplyHandler);
		mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	}
    private Intent mStorageIntent;
	private final Handler.Callback mReplyCallback = new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			boolean delivered = false;

			Log.d(LTAG, "handleMessage " + msg.what);
            Chapter active;
			switch (msg.what) {
			case StorageHandler.RECEIVE_ACTIVE_CHAPTER:
				active = (Chapter) msg.obj;
                updateDash(active);
                if(msg.arg1 > 0) {
                    if (active == null || active.clips.isEmpty()) {
                        mAudio.playSoundEffect(Sounds.DISALLOWED);
                    } else {
                        Intent previewIntent = new Intent(ClipService.this,
                                ClipPreviewActivity.class);
                        previewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        Bundle args = new Bundle();
                        args.putSerializable("chapter", active);
                        previewIntent.putExtras(args);
                        startActivity(previewIntent);
                    }
                }
				Log.i(LTAG, "GET_ACTIVE_CHAPTER delivered");
				delivered = true;
				break;
            case StorageHandler.RECEIVE_END_CHAPTER:
                active = (Chapter) msg.obj;
                if (active == null){
                    mAudio.playSoundEffect(Sounds.DISALLOWED);
                }else{
                    mAudio.playSoundEffect(Sounds.SUCCESS);
                    sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,false);
                }
                break;
			default:
				break;
			}

			return delivered;
		}
	};

	private AudioManager mAudio;
	public class ClipServiceBinder extends Binder {

		public void stop() {
			ClipService.this.stopSelf();
		}


		public void saveClip(String outputPath, Bitmap rawPreview) {
			try {

				File previewFile = new File(outputPath + ".thumb.jpg");

				Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);

				Bitmap preview = Bitmap.createScaledBitmap(rawPreview, size.x,
						size.y, true);

				preview.compress(CompressFormat.JPEG, 50, new FileOutputStream(
						previewFile));

				Clip clip = new Clip(outputPath, previewFile.getAbsolutePath());
				clip.dirty = true;
				sendStorageMessage(StorageHandler.PUSH_CLIP, clip);
                sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,false);

			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

        public void publishChapter(){
            sendStorageMessage(StorageHandler.END_CHAPTER, null);
            sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,false);
        }

        public void previewChapter(){
            sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,true);
        }

	}
    public void sendStorageMessage(int what, Object obj) {
        try {
            if (!storageLatch.await(15,TimeUnit.SECONDS)){
                throw new RuntimeException("Timeout getting storageLatch");
            }
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.replyTo = mStorageReplyMessenger;
        try {
            mStorageMessenger.send(msg);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
	private void updateDash(Chapter active) {
		if (mLiveCard.isPublished()) {
			mLiveCard.unpublish();
		}
        LiveCardBindings.buildDashView(mDashView,active);

		mLiveCard.setViews(mDashView);
		mLiveCard.publish(PublishMode.SILENT);
	}

	private ClipServiceBinder mBinder;

	@Override
	public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new ClipServiceBinder();
        }

        return mBinder;
	}

	private RemoteViews mDashView = null;


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, "pga_dash");

            mLiveCard.setDirectRenderingEnabled(false);

            mDashView = new RemoteViews(getPackageName(), R.layout.dash);
            mLiveCard.setViews(mDashView);

            Intent menuIntent = new Intent(this, LaunchMenuActivity.class);

            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent,
                    0));

        }

        return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mLiveCard != null && mLiveCard.isPublished()) {

			mLiveCard.unpublish();
			mLiveCard = null;
		}
		unbindService(mStorageConnection);
        stopService(mStorageIntent);
		super.onDestroy();
	}

}
