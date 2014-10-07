package com.repco.perfect.glassapp;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;
import com.repco.perfect.glassapp.sync.SyncService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Video.Thumbnails;

import com.google.android.glass.media.Sounds;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.view.TextureView.SurfaceTextureListener;

public class ClipCaptureActivity extends BaseBoundServiceActivity implements
		MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener,
		SurfaceTextureListener {

	private AudioManager am;
	private TextureView mTextureView = null;
	private Surface mSurface = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mTextureView = new TextureView(this);
		setContentView(mTextureView);
		
	}


	@SuppressLint("TrulyRandom")
	private final SecureRandom rand = new SecureRandom();

	File outputFile;

	private Bitmap mPreview;

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		switch (what) {
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			resetRecorder();
			
			mPreview = ThumbnailUtils.createVideoThumbnail(
					outputFile.getAbsolutePath(), Thumbnails.FULL_SCREEN_KIND);

			
			am.playSoundEffect(Sounds.SUCCESS);
			openOptionsMenu();
			break;
		}

	}
	
	@Override
	public void openOptionsMenu() {

		if(mPreview != null){
			Canvas c = mSurface.lockCanvas(null);
			try{
				c.drawBitmap(mPreview, (float) 0.0, (float) 0.0, null);
			}finally{
				mSurface.unlockCanvasAndPost(c);
			}
		}
		super.openOptionsMenu();
	}

	@Override
	protected void onStop() {
		super.onStop();
		destroy();
	}
	private void resetRecorder() {
		mRec.stop();
		mRec.reset();
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater ifl = getMenuInflater();
		ifl.inflate(R.menu.capture, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.keep_clip_mi:
			mClipService.saveClip(outputFile.getAbsolutePath(), mPreview);
			finish();
			break;
		case R.id.replay_clip_mi:
			MediaPlayer mp = new MediaPlayer();
			try {
				mp.setDataSource(this, Uri.fromFile(outputFile));
			} catch (IOException e) {
				e.printStackTrace();
				am.playSoundEffect(Sounds.ERROR);
				finish();
			}
			mSurface.release();
			mSurface = new Surface(mTextureView.getSurfaceTexture());
			mp.setSurface(mSurface);
			mp.setLooping(false);

			mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				
				@Override
				public void onPrepared(MediaPlayer mp) {
					mp.start();	
				}
			});
			
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					mp.setSurface(null);
					mp.release();
					openOptionsMenu();
				}
			});
			mp.prepareAsync();
			
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
	protected void onClipServiceConnected() {
		mTextureView.setSurfaceTextureListener(this);
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		Log.e(getClass().getSimpleName(), "MediaRecorder onError: " + what
				+ " " + extra);
		finish();
	}




	private MediaRecorder mRec;



	private synchronized void destroy(){
		
		if(mRec != null){
			Log.d(getClass().getSimpleName(),"->destroying media recorder");
			try{
				mRec.stop();
			}catch(IllegalStateException e){
				Log.d(getClass().getSimpleName(),"media recorder already stopped");
			}
			mRec.release();
			mRec = null;
		}
		if(mSurface != null && mSurface.isValid()){
			mSurface.release();
		}

	}


	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture texture, int width,
			int height) {
		
		mSurface = new Surface(texture);
		
		mRec = new MediaRecorder();

		mRec.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mRec.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

		mRec.setPreviewDisplay(mSurface);

		mRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		mRec.setMaxDuration(4000);

		outputFile = new File(getFilesDir().getAbsolutePath() + "/"
				+ new BigInteger(130, rand).toString(32) + ".mp4");

		try {
			if (!outputFile.createNewFile()) {
				Log.e(getClass().getSimpleName(), outputFile
						+ " already exists...");
				// TODO: deal with this "unlikely" error... maybe
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		mRec.setOutputFile(outputFile.getAbsolutePath());

		Log.i(getClass().getSimpleName(), "Output path is " + outputFile);
		mRec.setOnInfoListener(this);
		am.playSoundEffect(Sounds.SELECTED);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		try {
			mRec.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			finish();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			finish();
		}

		mRec.start();
		
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
		destroy();
		return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {}
}
