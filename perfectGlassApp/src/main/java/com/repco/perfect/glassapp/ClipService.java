package com.repco.perfect.glassapp;

import android.accounts.Account;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.widget.CardBuilder;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.storage.StorageService;
import com.repco.perfect.glassapp.sync.SyncTask;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClipService extends Service {
	private static final String LTAG = ClipService.class.getSimpleName();
	private LiveCard mLiveCard;


	private Messenger mStorageMessenger;
	private Messenger mStorageReplyMessenger;
	private Handler mStorageReplyHandler;
    private Handler mDashHandler = new Handler();


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
		}
	};

    private CountDownLatch storageLatch;

    private boolean clipTaken;

    private SyncTask mSyncTask;

    private final BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("[Sync]","received network state change");

            NetworkInfo netInfo = mConnManager.getActiveNetworkInfo();
            //should check null because in air plan mode it will be null
            if (netInfo != null && netInfo.isConnected()){
                mSyncTask.requestSync();
            }else{
                Log.i("[Sync]","network not connected");
            }

        }
    };

    ConnectivityManager mConnManager;

    private Account mAccount;
	@Override
	public void onCreate() {
		super.onCreate();

        mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);



        mConnManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mSyncTask = new SyncTask(this);

        clipTaken = false;


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

            //This is the recurring 30 second check to update
            //the relative timestamp on the live card
            mDashHandler.postDelayed(this,UPDATE_DASH_DELAY);
        }
    };
    private static final long PERIODIC_SYNC_PERIOD = 10 * DateUtils.MINUTE_IN_MILLIS;
    private Runnable mPeriodicSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if(isStopped){
                return;
            }
            Log.i(LTAG,"periodic sync poke");
            mSyncTask.requestSync();
            mDashHandler.postDelayed(mPeriodicSyncRunnable,PERIODIC_SYNC_PERIOD);
        }
    };

    public static final int CBID_CHAPTER_PREVIEW=1,CBID_INITIALIZE_LIVECARD=3;

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
                        case CBID_INITIALIZE_LIVECARD:
                            if (mLiveCard == null) {
                                mSyncTask.requestSync();
                                IntentFilter connIntent = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                                registerReceiver(mNetworkReceiver, connIntent);
                                mLiveCard = new LiveCard(ClipService.this, "pga_dash");
                                mLiveCard.attach(ClipService.this);
                                mLiveCard.setDirectRenderingEnabled(false);

                                mDashView = new RemoteViews(getPackageName(), R.layout.dash);
                                mLiveCard.setViews(mDashView);
                            }
                            break;
                        default:
                            mSyncTask.requestSync();
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
        CS_STOP_SERVICE("CS_STOP_SERVICE"),
        CS_SYNC_END("CS_SYNC_END"),
        CS_FORCE_STOP_SERVICE("CS_FORCE_STOP_SERVICE"),
        CS_MAYBE_STOP_SERVICE("CS_MAYBE_STOP_SERVICE")
        ;

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
                    if(!clipTaken){
                        sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,null,CBID_INITIALIZE_LIVECARD);
                        //This runnable re-adds itself to queue every 30 seconds
                        //so that the relative timestamp stays up to date on the
                        //live card
                        mDashHandler.post(mDashUpdateRunnable);
                        mDashHandler.postDelayed(mPeriodicSyncRunnable,PERIODIC_SYNC_PERIOD);
                        clipTaken = true;
                    }
                    Clip clip = (Clip) intent.getSerializableExtra("clip");
                    clip.dirty = true;
                    sendStorageMessage(StorageHandler.PUSH_CLIP, clip);
                    sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,null);
                    mAudio.playSoundEffect(Sounds.SUCCESS);
                    break;
                case CS_PUBLISH_CHAPTER:
                    sendStorageMessage(StorageHandler.END_CHAPTER, null);
                    sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER, null);
                    break;
                case CS_PREVIEW_CHAPTER:
                    sendStorageMessage(StorageHandler.GET_ACTIVE_CHAPTER,null,CBID_CHAPTER_PREVIEW);
                    break;
                case CS_STOP_SERVICE:
                    if(mSyncTask.getSyncStatus() == SyncTask.SYNC_SUCCESS) {
                        ClipService.this.stopSelf();
                    }else{
                        Intent i = new Intent(ClipService.this,SyncExitActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                    break;
                case CS_MAYBE_STOP_SERVICE:
                    if(clipTaken){
                        Log.i(LTAG,"Clip taken, will not stop service");
                    }else{
                        Log.i(LTAG,"No clip taken, will STOP service");
                        ClipService.this.stopSelf();
                    }
                    break;
                case CS_FORCE_STOP_SERVICE:
                    ClipService.this.stopSelf();
                    break;
                case CS_SYNC_END:
                    Log.i(LTAG,"Sync end");
                    if(mCachedActive == null || mCachedActive.clips.size() == 0){
                        Log.i(LTAG,"Killing service on sync end");
                        ClipService.this.stopSelf();
                    }
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


    public  void sendStorageMessage(final int what, final Object obj, final int callbackID) {
        mDashHandler.post(
                new Runnable() {
                    @Override
                    public void run() {

                        try {
                            if (!storageLatch.await(10, TimeUnit.SECONDS)) {
                                throw new RuntimeException("Timed out waiting for storage connection");
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        Message msg = Message.obtain();
                        msg.what = what;
                        msg.obj = obj;
                        msg.arg1 = callbackID;
                        msg.replyTo = mStorageReplyMessenger;
                        try {
                            mStorageMessenger.send(msg);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }

                    }
                });
    }

    private Chapter mCachedActive;


	private void updateDash() {
        if(mCachedActive == null){
            return;
        }

        RemoteViews dashView;
        if(mCachedActive.clips.size() == 0){
            mSyncTask.getSyncStatus();
            String footnote;
            String mainText = "Upload in progress";
                switch (mSyncTask.getSyncStatus()){

                    case SyncTask.SYNC_ERROR:
                        footnote = "There was an error";
                        break;

                    case SyncTask.SYNC_NO_NETWORK:
                        footnote = "We need wifi to continue sync";
                        break;
                    case SyncTask.SYNC_ONLY_METERED:
                        footnote = "We're saving your 4G data";
                        break;
                    case SyncTask.SYNC_INITIALIZING:
                    case SyncTask.SYNC_IN_PROGRESS:
                        footnote = "Sit tight!";
                        break;
                    case SyncTask.SYNC_SUCCESS:
                        mainText = "Upload complete!";
                        footnote = "Watch on chapterapp.io";
                        break;
                    default:
                        footnote = "default";
                        break;
                }
//            SYNC_SUCCESS = 0,
//                    SYNC_NO_NETWORK = 1,
//                    SYNC_ONLY_METERED=2,
//                    SYNC_ERROR =3,
//                    SYNC_IN_PROGRESS=4,
//                    SYNC_INITIALIZING=5

            CardBuilder card = new CardBuilder(this,CardBuilder.Layout.ALERT)
                    .setIcon(R.drawable.ic_sync_100)
                    .setText(mainText)
                    .setFootnote(footnote);
            dashView = card.getRemoteViews();
        }else{

            long chapterStart = mCachedActive.clips.get(0).ts.getTime();
            long now = new Date().getTime();
            String diffString = DateUtils.getRelativeDateTimeString(this,chapterStart,DateUtils.MINUTE_IN_MILLIS,DateUtils.WEEK_IN_MILLIS,0).toString().split(",")[0];
            String momentColor;
            int clipCount = mCachedActive.clips.size();

            if (clipCount < 3)
                momentColor = "#cc3333";
            else
                momentColor = "#99cc33";

            String dashString = String.format("Your chapter has <font color='%s'>%d</font> moments and started <font color='#ddbb11'>%s</font>", momentColor,clipCount, diffString);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();

            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                    .setText(Html.fromHtml(dashString));

            for (int i=0; i< mCachedActive.clips.size() && i<5; i++) {

                Bitmap bitmap = BitmapFactory.decodeFile(mCachedActive.clips.get(i).previewPath, bmOptions);
                card.addImage(bitmap);

            }

            dashView = card.getRemoteViews();
        }
		mLiveCard.setViews(dashView);
        if (!mLiveCard.isPublished()) {
            mLiveCard.publish(PublishMode.SILENT);
        }

	}


	private RemoteViews mDashView = null;


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LTAG,"onStartCommand "+intent);

        /*
            Really should always check if intent is null before you assume that this invocation
            is being generated in response to user action
            
            http://developer.android.com/reference/android/app/Service.html#onStartCommand(android.content.Intent, int, int)

            about the "intent" parameter

            "This may be null if the service is being restarted after its process has gone away,
            and it had previously returned anything except START_STICKY_COMPATIBILITY."

         */
        if(intent != null) {
            Intent captureIntent = new Intent(ClipService.this, ClipCaptureActivity.class);
            captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(captureIntent);
        }
        // We're database backed, so state should be "persistent" oustide
        // of this process lifecycle
        return START_STICKY_COMPATIBILITY;
	}

	@Override
	public void onDestroy() {
        try {
            unregisterReceiver(mNetworkReceiver);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
            Log.w(LTAG,"This exception (should!!) mean that the receiver was never registered. This should be ok");
        }
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
