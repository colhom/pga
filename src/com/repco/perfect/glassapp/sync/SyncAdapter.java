package com.repco.perfect.glassapp.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import com.repco.perfect.glassapp.BuildConfig;
import com.repco.perfect.glassapp.base.Storable;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.storage.StorageService;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String LTAG = SyncAdapter.class.getSimpleName();

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
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
			serviceBarrier.countDown();
		}
	};
	
	private CountDownLatch serviceBarrier;

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult result) {

		Log.i(LTAG, "Sync started");

		ConnectivityManager connManager = (ConnectivityManager) getContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo mWifi = connManager.getActiveNetworkInfo();

		if(mWifi == null){
			Log.i(LTAG, "No active network connection, skipping sync");
			return;
		}
		
		if(connManager.isActiveNetworkMetered()){
			Log.i("LTAG","Active network connection is metetered, skipping sync");
			return;
		}
		
		// TODO: this is gross, architect your software properly plz
		final Semaphore syncBarrier = new Semaphore(1);
		final CountDownLatch doneBarrier = new CountDownLatch(1);

		final Handler.Callback mReplyCallback = new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				boolean delivered = false;
				Log.i(LTAG, "handleMessage " + msg.what);

				switch (msg.what) {
				case StorageHandler.RECEIVE_NEXT_STORABLE:
					if (msg.obj == null) {
						// this will end the sync, once syncBarrier is released
						doneBarrier.countDown();
					} else {
						Storable storable = (Storable) msg.obj;

						if (BuildConfig.DEBUG && !storable.dirty) {
							throw new RuntimeException(
									"We got a clean storable in the SyncAdapter! "
											+ storable);
						}

						Log.i(LTAG, "sync storable " + storable);
						// TODO: actually sync storable

						storable.dirty = false;

						Message storemsg = Message.obtain();
						storemsg.what = StorageHandler.PUSH_STORABLE;
						storemsg.obj = storable;

						try {
							mStorageMessenger.send(storemsg);
						} catch (RemoteException e) {
							throw new RuntimeException(e);
						}
					}

					delivered = true;
					break;
				default:
					Log.e(LTAG, "unknown message type " + msg.what);
					break;

				}
				Log.i(LTAG, "releasing sync barrier");
				syncBarrier.release();
				return delivered;
			}
		};

		HandlerThread ht = new HandlerThread("StorageReplyHandler");
		ht.start();
		Handler mStorageReplyHandler = new Handler(ht.getLooper(),
				mReplyCallback);
		final Messenger mStorageReplyMessenger = new Messenger(
				mStorageReplyHandler);

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
		
		while (doneBarrier.getCount() != 0) {
			try {
				// will block here waiting for RECEIVE_NEXT_STORABLE message
				syncBarrier.acquire();

			} catch (InterruptedException e) {
				Log.w(LTAG, "SyncBarrier interrupted, aborting sync");
				e.printStackTrace();
				break;
			}
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

		// Cleanup
		getContext().unbindService(mStorageConnection);

	}

}
