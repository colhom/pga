package com.repco.perfect.glassapp;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Deque;
import java.util.NoSuchElementException;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.repco.perfect.glassapp.ClipService.ClipDescriptor;
import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

public class ClipPreviewActivity extends BaseBoundServiceActivity implements SurfaceHolder.Callback {

	
	SurfaceView mSurfaceView;
	MediaPlayer mPlayer;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSurfaceView = new SurfaceView(this);
		mSurfaceView.getHolder().addCallback(this);
		mPlayer = new MediaPlayer();
		setContentView(mSurfaceView);
		

	}
	@Override
	protected void onClipServiceConnected() {

	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mPlayer.setDisplay(mSurfaceView.getHolder());
		final Deque<ClipService.ClipDescriptor> clips = mClipService.getRecordedClips();
		
		MediaPlayer.OnCompletionListener trigger = new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				System.out.println("OnCompletion "+clips.size());
				ClipDescriptor clip;
				try{
					clip = clips.pop();
				}catch(NoSuchElementException e){
					// no clips left;
					finish();
					return;
				}
				
				mp.reset();
				
				try {
					System.out.println("Clip PATH "+clip.path);
					mp.setDataSource(clip.path);
					mp.prepare();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
				mp.setOnCompletionListener(this);
				
				mp.start();
			}
		};
		
		trigger.onCompletion(mPlayer);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mPlayer != null){
			mPlayer.release();
		}
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {}

}
