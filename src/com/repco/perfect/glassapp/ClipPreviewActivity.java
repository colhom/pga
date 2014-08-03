package com.repco.perfect.glassapp;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

import com.google.android.glass.media.Sounds;
import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;

public class ClipPreviewActivity extends BaseBoundServiceActivity implements SurfaceTextureListener {

	private static final String LTAG = ClipPreviewActivity.class.getSimpleName();
	TextureView mSurfaceView;
	MediaPlayer mPlayer;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSurfaceView = new TextureView(this);
		mSurfaceView.setSurfaceTextureListener(this);
		mPlayer = new MediaPlayer();
		setContentView(mSurfaceView);
		

	}
	@Override
	protected void onClipServiceConnected() {

	}

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mPlayer != null){
			mPlayer.release();
		}
	}
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
		final Surface surface = new Surface(mSurfaceView.getSurfaceTexture());
		mPlayer.setSurface(surface);
		
		Chapter chapter = (Chapter) getIntent().getExtras().getSerializable("chapter");
		
		if(chapter == null){
			mClipService.mAudio.playSoundEffect(Sounds.DISALLOWED);
			finish();
			return;
		}
		
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

				clip = clips.poll();
				if(clip == null){
					// no clips left;
					surface.release();
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
	public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
		// TODO Auto-generated method stub
		
	}

}
