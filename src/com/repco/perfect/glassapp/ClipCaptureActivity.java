package com.repco.perfect.glassapp;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ClipCaptureActivity extends BaseBoundServiceActivity implements
		MediaRecorder.OnInfoListener, 
		MediaRecorder.OnErrorListener,
		SurfaceHolder.Callback,
		GestureDetector.BaseListener {

	
	private SurfaceView mPreviewSurface;

	


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mClipService.destroyRecorder();
	}
	
	@SuppressLint("TrulyRandom")
	private final SecureRandom rand = new SecureRandom();

	String outputPath = ClipService.clipRoot.getAbsolutePath() + "/"
			+ new BigInteger(130, rand).toString(32) + ".mp4";

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		switch (what) {
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			mClipService.saveClip(outputPath);
			finish();
			break;
		}

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
		setContentView(R.layout.capture);


		
		mPreviewSurface = (SurfaceView) findViewById(R.id.capture_preview_surface);
		mPreviewSurface.getHolder().addCallback(this);
		
		mRec = mClipService.getMediaRecorder();
		mRec.setMaxDuration(4000);
		mRec.setOutputFile(outputPath);
		mRec.setPreviewDisplay(mPreviewSurface.getHolder().getSurface());
		mRec.setOnInfoListener(this);
	}

	@Override
	public boolean onGesture(Gesture arg0) {
		switch (arg0) {
		case SWIPE_DOWN:
			mClipService.abortClip(outputPath);
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		System.err.println("MediaRecorder onError: "+what+" "+extra);
		
	}
}
