package com.repco.perfect.glassapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.storage.StorageService;
import com.repco.perfect.glassapp.sync.SyncService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteOpenHelper;
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
		}
	};
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
		

		if(!bindService(new Intent(this, StorageService.class), mStorageConnection, BIND_AUTO_CREATE)){
			throw new RuntimeException("Could not bind to storage service");
		}

		// rx
		HandlerThread ht = new HandlerThread("StorageReplyHandler");
		ht.start();
		mStorageReplyHandler = new Handler(ht.getLooper(), mReplyCallback);
		mStorageReplyMessenger = new Messenger(mStorageReplyHandler);

	}

	private final Handler.Callback mReplyCallback = new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			boolean delivered = false;

			Log.d(LTAG, "handleMessage " + msg.what);
			switch (msg.what) {
			case StorageHandler.RECEIVE_ACTIVE_CHAPTER:
				Chapter active = (Chapter) msg.obj;

				Intent previewIntent = new Intent(ClipService.this,
						ClipPreviewActivity.class);
				previewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_CLEAR_TASK);
				Bundle args = new Bundle();
				args.putSerializable("chapter", active);
				previewIntent.putExtras(args);
				startActivity(previewIntent);
				delivered = true;
				Log.i(LTAG, "GET_ACTIVE_CHAPTER delivered");
				break;
			default:
				break;
			}

			return delivered;
		}
	};

	public class ClipServiceBinder extends Binder {

		public void stop() {
			ClipService.this.stopSelf();
		}

		public AudioManager mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		public void recordClip() {

			Intent captureIntent = new Intent(ClipService.this,
					ClipCaptureActivity.class);
			captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			getApplication().startActivity(captureIntent);
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

				sendMessage(StorageHandler.PUSH_CLIP, clip);

				updateDash();

			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		public void sendMessage(int what, Object obj) {
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

	}

	private void updateDash() {
		if (mLiveCard.isPublished()) {
			mLiveCard.unpublish();
		}

		mLiveCard.setViews(mDashView);
		mLiveCard.publish(PublishMode.SILENT);
	}

	private ClipServiceBinder mBinder;

	@Override
	public IBinder onBind(Intent intent) {
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

			updateDash();
		}
		mBinder = new ClipServiceBinder();
		mBinder.recordClip();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mLiveCard != null && mLiveCard.isPublished()) {

			mLiveCard.unpublish();
			mLiveCard = null;
		}
		unbindService(mStorageConnection);
		super.onDestroy();
	}

}
