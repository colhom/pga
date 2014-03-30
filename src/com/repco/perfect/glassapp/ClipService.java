package com.repco.perfect.glassapp;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.widget.RemoteViews;

public class ClipService extends Service {

	private TimelineManager mTimelineManager;
	private LiveCard mLiveCard;

	@Override
	public void onCreate() {
		super.onCreate();
		mTimelineManager = TimelineManager.from(this);
		for (int retry = 0; retry < 20; retry++) {
			try {
				System.out.println("Camera attempt #" + retry);
				mCamera = Camera.open(); // attempt to get a Camera instance
			} catch (Exception e) {
				System.err.println(e);
				try {
					Thread.sleep(250);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		if (mCamera == null) {
			throw new RuntimeException("Couldn't get camera service");
		}
	}

	private AudioManager am;

	public class ClipServiceBinder extends Binder {

		public void stop() {
			ClipService.this.stopSelf();
		}

		public void recordClip() {
			try {
				mCamera.reconnect();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			try {
				mCamera.setPreviewDisplay(null);
			} catch (java.io.IOException ioe) {
				System.err.println("IOException nullifying preview display: "
						+ ioe.getMessage());
			}
			mCamera.stopPreview();
			mCamera.unlock();

			mRec = new MediaRecorder();
			mRec.setCamera(mCamera);
			mRec.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

			Intent captureIntent = new Intent(ClipService.this,
					ClipCaptureActivity.class);
			captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			getApplication().startActivity(captureIntent);
			am.playSoundEffect(Sounds.SELECTED);
		}

		public MediaRecorder getMediaRecorder() {
			return mRec;
		}

		public void abortClip(String outputPath) {
			try {
				File f = new File(outputPath);
				f.delete();
			} finally {
				destroyRecorder();
			}
		}

		public void saveClip(String outputPath) {
			am.playSoundEffect(Sounds.SUCCESS);
			destroyRecorder();
		}

		public void destroyRecorder() {
			if (mRec != null) {
				try {
					mRec.stop();
					mRec.reset();
					mRec.release();
				} finally {					
					mRec = null;
					mCamera.unlock();
				}
			}
		}
	}

	private final ClipServiceBinder mBinder = new ClipServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static File clipRoot = new File(
			Environment.getExternalStorageDirectory() + "/.perfect_cache");

	private MediaRecorder mRec;
	private Camera mCamera = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		if (mLiveCard == null) {
			mLiveCard = mTimelineManager.createLiveCard("perfect");

			mLiveCard.setDirectRenderingEnabled(false);

			mLiveCard
					.setViews(new RemoteViews(getPackageName(), R.layout.dash));

			Intent menuIntent = new Intent(this, LaunchMenuActivity.class);
			menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent,
					0));

			mLiveCard.publish(LiveCard.PublishMode.SILENT);
		}
		if (!clipRoot.exists()) {
			if (!clipRoot.mkdir()) {
				throw new RuntimeException("Cannot mkdir "
						+ clipRoot.getAbsolutePath());
			}
		}

		mBinder.recordClip();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mLiveCard != null && mLiveCard.isPublished()) {

			mLiveCard.unpublish();
			mLiveCard = null;
		}
		mBinder.destroyRecorder();
		super.onDestroy();
	}

}
