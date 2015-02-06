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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.WindowManager;
import android.widget.ToggleButton;

import com.google.android.glass.media.Sounds;
import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;
import com.repco.perfect.glassapp.storage.StorageHandler;

public class ClipPreviewActivity extends BaseBoundServiceActivity implements
		SurfaceTextureListener {

	private static final String LTAG = ClipPreviewActivity.class
			.getSimpleName();
	TextureView mTextureView;
	MediaPlayer mPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mChapter = (Chapter) getIntent().getExtras().getSerializable("chapter");
		mTextureView = new TextureView(this);
		mTextureView.setSurfaceTextureListener(this);
		mPlayer = new MediaPlayer();
		setContentView(mTextureView);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	}

	@Override
	protected void onClipServiceConnected() {
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mPlayer != null) {
			mPlayer.release();
		}
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private Chapter mChapter = null;
	
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
		final Surface surface = new Surface(mTextureView.getSurfaceTexture());
		mPlayer.setSurface(surface);

		Log.d(LTAG, mChapter.toString());
		final Queue<Clip> clips = new LinkedBlockingQueue<Clip>(mChapter.clips);

		for (Clip clip : clips) {
			Log.d(LTAG, clip.toString());
		}
		MediaPlayer.OnCompletionListener trigger = new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.i(LTAG,"OnCompletion " + clips.size());
				Clip clip;

				clip = clips.poll();
				if (clip == null) {
					// no clips left;
					surface.release();
					openOptionsMenu();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.preview, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mChapter == null || mChapter.clips.size() < StorageHandler.MIN_CHAPTER_SIZE) {
			menu.getItem(0).setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.publish_chapter_item:
			getClipService().publishChapter();
			finish();
			break;
		case R.id.exit_preview_item:
			finish();
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			openOptionsMenu();
			return true;
		}
		return super.onKeyDown(keyCode, event);
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
