package com.repco.perfect.glassapp;

import java.io.File;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.gms.auth.GoogleAuthUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.widget.RemoteViews;

public class ClipService extends Service {

	private LiveCard mLiveCard;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public class ClipServiceBinder extends Binder {


		public void updateDash() {
			mLiveCard.setViews(mDashView);
		} 
		
		public void kill(){
			ClipService.this.stopSelf();
		}
	}
	private final ClipServiceBinder mBinder = new ClipServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static File clipRoot = new File(
			Environment.getExternalStorageDirectory() + "/.perfect_cache");

	private RemoteViews mDashView = null;
	private Account mGoogleAccount;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mGoogleAccount == null) {
			AccountManager am = AccountManager.get(this);
			Account[] accounts = am
					.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

			System.out.println("AccountManager returns " + accounts.length
					+ " google accounts");
			if (accounts.length == 0) {
				throw new RuntimeException("Could not find any google accounts");
			}
			mGoogleAccount = accounts[0];
		}
		System.out.println("Using acccount " + mGoogleAccount.name);

		if (mLiveCard == null) {
			mLiveCard = new LiveCard(this, "GlassDeere");

			mLiveCard.setDirectRenderingEnabled(false);

			mDashView = new RemoteViews(getPackageName(), R.layout.dash);
			mLiveCard.setViews(mDashView);

			Intent menuIntent = new Intent(this, LaunchMenuActivity.class);
			menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent,
					0));

			mBinder.updateDash();
			mLiveCard.publish(PublishMode.REVEAL);
			
		}
		if (!clipRoot.exists()) {
			if (!clipRoot.mkdir()) {
				throw new RuntimeException("Cannot mkdir "
						+ clipRoot.getAbsolutePath());
			}
		}

		startClipCapture();
		return START_STICKY;
	}
	
	private void startClipCapture(){
		Intent captureIntent = new Intent(this, ClipCaptureActivity.class);
		captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		getApplication().startActivity(captureIntent);
	}

	@Override
	public void onDestroy() {
		if (mLiveCard != null && mLiveCard.isPublished()) {

			mLiveCard.unpublish();
			mLiveCard = null;
		}
		super.onDestroy();
	}

}
