package com.repco.perfect.glassapp;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;

public class ClipPreviewActivity extends BaseBoundServiceActivity implements SurfaceHolder.Callback {

	private static final String LTAG = ClipPreviewActivity.class.getSimpleName();
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
	
		Chapter chapter = (Chapter) getIntent().getExtras().getSerializable("chapter");
		Log.d(LTAG,chapter.toString());
		final Queue<Clip> clips = new LinkedBlockingQueue<Clip>(chapter.clips);
		
		for (Clip clip : clips){
			Log.d(LTAG,clip.toString());
		}
		MediaPlayer.OnCompletionListener trigger = new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				System.out.println("OnCompletion "+clips.size());
				Clip clip;
				try{
					clip = clips.poll();
				}catch(NoSuchElementException e){
					// no clips left;
					finish();
					return;
				}
				
				mp.reset();
				
				try {
					mp.setDataSource(clip.videoPath);
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
