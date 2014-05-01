package com.repco.perfect.glassapp;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Video.Thumbnails;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

public class ClipCaptureActivity extends BaseBoundServiceActivity implements
		MediaRecorder.OnInfoListener, 
		MediaRecorder.OnErrorListener,
		SurfaceHolder.Callback {

	

	
	private AudioManager am;

	@Override
	protected void onStop() {
		super.onStop();
		mClipService.destroyRecorder();
		if(!mStateDone){
			File f = new File(outputPath);
			f.delete();
		}
	}
	
	@SuppressLint("TrulyRandom")
	private final SecureRandom rand = new SecureRandom();

	String outputPath = ClipService.clipRoot.getAbsolutePath() + "/"
			+ new BigInteger(130, rand).toString(32) + ".mp4";

	
	private Bitmap mPreview;
	private ImageView mPreviewView;
	private boolean mStateDone = false;
	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		switch (what) {
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			mClipService.destroyRecorder();
			mPreview = ThumbnailUtils.createVideoThumbnail(outputPath, Thumbnails.FULL_SCREEN_KIND);
			mPreviewView = new ImageView(this);
			mPreviewView.setImageBitmap(mPreview);
			setContentView(mPreviewView);
			openOptionsMenu();
			am.playSoundEffect(Sounds.SUCCESS);
			break;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater ifl = getMenuInflater();
		ifl.inflate(R.menu.capture, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.keep_clip_mi:
			mClipService.saveClip(outputPath, mPreview);
			finish();
			break;
		case R.id.replay_clip_mi:
			final SurfaceView previewSurface = new SurfaceView(this);
			previewSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
				
				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {}
				
				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					MediaPlayer mp = MediaPlayer.create(ClipCaptureActivity.this, Uri.fromFile(new File(outputPath)), previewSurface.getHolder());
					mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mp) {
							setContentView(mPreviewView);
							openOptionsMenu();
						}
					});
					mp.start();
				}
				
				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width,
						int height) {}
			});
			setContentView(previewSurface);

			break;
		case R.id.discard_clip_mi:
			finish();
			break;
		default:
			return false;
		}
		am.playSoundEffect(Sounds.SELECTED);
		return true;
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		try {
			mRec.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		mRec.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	private MediaRecorder mRec;
	@Override
	protected void onClipServiceConnected() {
		//TODO: loading screen
		
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		SurfaceView previewSurface = new SurfaceView(this);
		mRec = mClipService.getMediaRecorder();
		mRec.setMaxDuration(4000);
		mRec.setOutputFile(outputPath);
		mRec.setPreviewDisplay(previewSurface.getHolder().getSurface());
		mRec.setOnInfoListener(this);
		am.playSoundEffect(Sounds.SELECTED);
		

		previewSurface.getHolder().addCallback(this);
		setContentView(previewSurface);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}


	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		System.err.println("MediaRecorder onError: "+what+" "+extra);
		finish();
	}
}
