package com.repco.perfect.glassapp;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Thumbnails;

import com.google.android.glass.media.CameraManager;
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

public class ClipCaptureActivity extends BaseBoundServiceActivity {

	private AudioManager am;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(!mStateDone){
			File f = new File(outputPath);
			f.delete();
		}
	}
	
	@SuppressLint("TrulyRandom")
	private final SecureRandom rand = new SecureRandom();

	String outputPath = ClipService.clipRoot.getAbsolutePath() + "/"
			+ new BigInteger(130, rand).toString(32) + ".mp4";

	private boolean mStateDone = false;

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
			finish();
			break;
		case R.id.replay_clip_mi:
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
	private static final int RECORD_VIDEO_REQUEST = 1;
	
	private void recordVideo(){
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3);
		startActivityForResult(intent, RECORD_VIDEO_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == RECORD_VIDEO_REQUEST && resultCode == RESULT_OK){
			String path = data.getStringExtra(CameraManager.EXTRA_VIDEO_FILE_PATH);
			System.out.println("output: "+path);
			if(!new File(path).delete()){
				System.err.println("Could not delete "+path);
			}
			openOptionsMenu();
		}
	}


	@Override
	protected void onStart() {
		super.onStart();
		recordVideo();
	}
	@Override
	protected void onClipServiceConnected() {
		//TODO: loading screen
		
//		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//		SurfaceView previewSurface = new SurfaceView(this);
//		mRec = mClipService.getMediaRecorder();
//		mRec.setMaxDuration(4000);
//		mRec.setOutputFile(outputPath);
//		mRec.setPreviewDisplay(previewSurface.getHolder().getSurface());
//		mRec.setOnInfoListener(this);
//		am.playSoundEffect(Sounds.SELECTED);
//		
//
//		previewSurface.getHolder().addCallback(this);
//		setContentView(previewSurface);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

}
