package com.repco.perfect.glassapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.storage.StorageService;
import com.repco.perfect.glassapp.ui.LiveCardBindings;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClipService extends Service {
	private static final String LTAG = ClipService.class.getSimpleName();
	private LiveCard mLiveCard;


	private Messenger mStorageMessenger;
	private Messenger mStorageReplyMessenger;
	private Handler mStorageReplyHandler;
    private Handler mDashHandler = new Handler();

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
            Message m = Message.obtain();
            m.what = StorageHandler.GET_ACTIVE_CHAPTER;
            m.arg1 = CBID_INITIALIZE_LIVECARD;
            m.replyTo = mStorageReplyMessenger;
            try {
                mStorageMessenger.send(m);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            storageLatch.countDown();
            mDashHandler.post(mDashUpdateRunnable);
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

				Log.e(LTAG,"Add account failed!");
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

        //set up broadcast receiver for activites
        IntentFilter filter = new IntentFilter();
        for(Action action : Action.values()){
            filter.addAction(action.toString());
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,filter);
	}
    private static final long UPDATE_DASH_DELAY = 30 * DateUtils.SECOND_IN_MILLIS;

    private boolean isStopped = false;
    private Runnable mDashUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if(isStopped){
                return;
            }
            updateDash();
            mDashHandler.postDelayed(this,UPDATE_DASH_DELAY);
        }
    };

    public static final int CBID_CHAPTER_PREVIEW=1,CBID_LAUNCH_OPTIONS_MENU=2,CBID_INITIALIZE_LIVECARD=3;

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

                    switch(msg.arg1) {
                        case CBID_CHAPTER_PREVIEW:
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
                            break;
                        case CBID_LAUNCH_OPTIONS_MENU:

                            break;

                        case CBID_INITIALIZE_LIVECARD:
                            if (mLiveCard == null) {
                                mLiveCard = new LiveCard(ClipService.this, "pga_dash");
                                mLiveCard.attach(ClipService.this);
                                mLiveCard.setDirectRenderingEnabled(false);

                                mDashView = new RemoteViews(getPackageName(), R.layout.dash);
                                mLiveCard.setViews(mDashView);
                            }
                            break;
                        default:

                            break;
                    }
                    Intent menuIntent = new Intent(ClipService.this, LaunchMenuActivity.class);
                    menuIntent.putExtra("chapter", active);
                    mLiveCard.setAction(PendingIntent.getActivity(ClipService.this,0, menuIntent, PendingIntent.FLAG_UPDATE_CURRENT));

                    mCachedActive = active;
                    updateDash();

                    delivered = true;
                    break;
                case StorageHandler.RECEIVE_END_CHAPTER:
                    active = (Chapter) msg.obj;
                    if (active == null){
                        mAudio.playSoundEffect(Sounds.DISALLOWED);
                    }else{
                        mAudio.playSoundEffect(Sounds.SUCCESS);
                        sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,null);
                    }
                    break;
                default:
                    break;
			}

			return delivered;
		}
	};

	private AudioManager mAudio;

    public enum Action {
        CS_SAVE_CLIP("CS_SAVE_CLIP"),
        CS_PUBLISH_CHAPTER("CS_PUBLISH_CHAPTER"),
        CS_PREVIEW_CHAPTER("CS_PREVIEW_CHAPTER"),
        CS_STOP_SERVICE("CS_STOP_SERVICE");

        private final String val;
        private Action(final String val){
            this.val = val;
        }

        @Override
        public String toString() {
            return val;
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Our filter setup should ensure that the fitler this receiver
            //was registered with was generated from the same enum

            //Illegal arg exception will be thrown here otherwise from Action.valueOf()
            Action action = Action.valueOf(intent.getAction());

            switch (action){
                case CS_SAVE_CLIP:
                    String outputPath = intent.getStringExtra("clipPath");
                    String previewPath = intent.getStringExtra("previewPath");
                    Clip clip = new Clip(outputPath, previewPath);
                    clip.dirty = true;
                    sendStorageMessage(StorageHandler.PUSH_CLIP, clip);
                    sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,null);
                    break;
                case CS_PUBLISH_CHAPTER:
                    sendStorageMessage(StorageHandler.END_CHAPTER, null);
                    sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER, null);
                    break;
                case CS_PREVIEW_CHAPTER:
                    sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,null,CBID_CHAPTER_PREVIEW);
                    break;
                case CS_STOP_SERVICE:
                    ClipService.this.stopSelf();
                    break;
                default:
                    Log.w(LTAG,"taking default (no) action for received broadcast "+action);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void sendStorageMessage(int what,Object obj){
        sendStorageMessage(what,obj,0);
    }
    public void sendStorageMessage(int what, Object obj, int cbId) {
        try{
            if(!storageLatch.await(10,TimeUnit.SECONDS)){
                throw new RuntimeException("Timed out waiting for storage connection");
            }
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = cbId;
        msg.replyTo = mStorageReplyMessenger;
        try {
            mStorageMessenger.send(msg);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
    private Chapter mCachedActive;
	private void updateDash() {
        if(mCachedActive == null){
            return;
        }

        LiveCardBindings.buildDashView(this,mDashView,mCachedActive);

		mLiveCard.setViews(mDashView);
        if (!mLiveCard.isPublished()) {
            mLiveCard.publish(PublishMode.SILENT);
        }

	}


	private RemoteViews mDashView = null;


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Intent captureIntent = new Intent(ClipService.this,ClipCaptureActivity.class);
        captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(captureIntent);
        // We're database backed, so state should be "persistent" oustide
        // of this process lifecycle
        return START_STICKY;
	}

	@Override
	public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        isStopped = true;
		if (mLiveCard != null && mLiveCard.isPublished()) {

			mLiveCard.unpublish();
		}
        mLiveCard = null;
		unbindService(mStorageConnection);
        stopService(mStorageIntent);
		super.onDestroy();
	}

}
