package com.repco.perfect.glassapp;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

import android.annotation.SuppressLint;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ClipCaptureActivity extends BaseBoundServiceActivity implements
		MediaRecorder.OnInfoListener,
		SurfaceHolder.Callback{

	private MediaRecorder mRec;
	private SurfaceView mPreviewSurface;

	private File clipRoot = new File(Environment.getExternalStorageDirectory()
			+ "/.perfect_cache");
	
	private String outputPath;
	
	@SuppressLint("TrulyRandom")
	private final SecureRandom rand = new SecureRandom();
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyRecorder();
		mClipService.abortClip(outputPath);
	}

	private void destroyRecorder(){
		if(mRec != null){
			mRec.stop();
			mRec.release();
			mRec = null;
		}
	}

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		switch (what) {
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			destroyRecorder();
			mClipService.saveClip(outputPath);
			finish();
			break;
		}

	}


	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {}


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
	public void surfaceDestroyed(SurfaceHolder holder) {}


	@Override
	protected void onClipServiceConnected() {
		setContentView(R.layout.capture);

		if (!clipRoot.exists()) {
			if (!clipRoot.mkdir()) {
				throw new RuntimeException("Cannot mkdir "
						+ clipRoot.getAbsolutePath());
			}
		}
		outputPath = clipRoot.getAbsolutePath() + "/"
				+ new BigInteger(130, rand).toString(32) + ".mp4";

		mRec = new MediaRecorder();

		mRec.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		
		mRec.setMaxDuration(4000);
		mRec.setOnInfoListener(this);
		mPreviewSurface = (SurfaceView) findViewById(R.id.capture_preview_surface);
		mPreviewSurface.getHolder().addCallback(this);
		mRec.setPreviewDisplay(mPreviewSurface.getHolder().getSurface());
		mRec.setOutputFile(outputPath);
	}

}
