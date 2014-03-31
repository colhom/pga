package com.repco.perfect.glassapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.google.android.glass.app.Card;
import com.google.android.glass.app.Card.ImageLayout;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;

public class ClipService extends Service {

	private TimelineManager mTimelineManager;
	private LiveCard mLiveCard;

	@Override
	public void onCreate() {
		super.onCreate();
		mTimelineManager = TimelineManager.from(this);
	}

	public class ClipServiceBinder extends Binder {

		public void stop() {
			ClipService.this.stopSelf();
		}

		public void recordClip() {
			for (int retry = 0; retry < 20; retry++) {
				try {
					System.out.println("Camera attempt #" + retry);
					mCamera = Camera.open(); // attempt to get a Camera instance
				} catch (Exception e) {
					System.err.println(e);
					try {
						Thread.sleep(60);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					continue;
				}
				break;
			}

			if (mCamera == null) {
				throw new RuntimeException("Couldn't get camera service");
			}
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
		}
 
		private Long mCardId = null;
		private static final int MAX_IMG = 4;
		

		public void saveClip(String outputPath, Bitmap rawPreview) {
			try {
				File previewFile = new File(outputPath + ".thumb.jpg");
				
				Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				
				Bitmap preview = Bitmap.createScaledBitmap(rawPreview, size.x, size.y, true);
				
				preview.compress(CompressFormat.JPEG, 50, new FileOutputStream(
						previewFile));
				Date now = new Date();
				
				//TODO: upload queue
				
				Card card; 
				if(mCardId == null){
					card = new Card(getBaseContext());
					card.addImage(Uri.fromFile(previewFile)).setImageLayout(ImageLayout.FULL);
					card.setFootnote(DateFormat.getDateTimeInstance().format(now));
					mCardId = mTimelineManager.insert(card);
				}else{
				
					card = mTimelineManager.query(mCardId);
					card.setFootnote(DateFormat.getDateTimeInstance().format(now));
					List<Uri> images = new LinkedList<Uri>();
					
					images.add(Uri.fromFile(previewFile));
					for(int i = 0; (i < card.getImageCount() && i < MAX_IMG-1);i++){
						images.add(card.getImage(i));
					}
					card.clearImages();
					for(Uri image : images){
						card.addImage(image);
					}
					mTimelineManager.update(mCardId, card);
				}
				mVidCount++;
				updateDash();

			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		public MediaRecorder getMediaRecorder() {
			return mRec;
		}

		public void destroyRecorder() {
			System.out.println("DestroyRecorder: " + mRec + " : " + mCamera);
			try {
				if (mRec != null) {
						mRec.stop();
						mRec.release();
						mRec = null;
				}
			} finally {
				if (mCamera != null) {
					mCamera.release();
					mCamera = null;
				}
			}
		}
	}
	//TODO: make real
	private int mVidCount = 0;
	private void updateDash(){
		if(mLiveCard.isPublished()){
			mLiveCard.unpublish();
		}
		mDashView.setTextViewText(R.id.dash_main, mVidCount+" clips taken");
		mLiveCard.setViews(mDashView);
		mLiveCard.publish(PublishMode.SILENT);
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
	private RemoteViews mDashView = null;
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (mLiveCard == null) {
			mLiveCard = mTimelineManager.createLiveCard("perfect");

			mLiveCard.setDirectRenderingEnabled(false);

			mDashView = new RemoteViews(getPackageName(), R.layout.dash);
			mLiveCard.setViews(mDashView);

			Intent menuIntent = new Intent(this, LaunchMenuActivity.class);
			menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent,
					0));
			
			updateDash();
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
		mBinder.destroyRecorder();
		if (mLiveCard != null && mLiveCard.isPublished()) {

			mLiveCard.unpublish();
			mLiveCard = null;
		}
		super.onDestroy();
	}

}
