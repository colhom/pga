package com.repco.perfect.glassapp;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.WindowManager;

import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.base.ChapterImmersionActivity;
import com.repco.perfect.glassapp.storage.Clip;
import com.repco.perfect.glassapp.storage.StorageHandler;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClipPreviewActivity extends ChapterImmersionActivity implements
		SurfaceTextureListener {

	private static final String LTAG = ClipPreviewActivity.class
			.getSimpleName();
	TextureView mTextureView;
	MediaPlayer mPlayer;

    @Override
    public int getLayoutId() {
        return R.layout.video_immersion;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTextureView = (TextureView) findViewById(R.id.video_texture_view);
		mTextureView.setSurfaceTextureListener(this);
		mPlayer = new MediaPlayer();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mPlayer != null) {
			mPlayer.release();
		}
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
		final Surface surface = new Surface(mTextureView.getSurfaceTexture());
		mPlayer.setSurface(surface);


        Log.d(LTAG, mChapter.toString());
        final Queue<Clip> clips = new LinkedBlockingQueue<Clip>(mChapter.clips);


        MediaPlayer.OnCompletionListener trigger = new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(LTAG,"OnCompletion " + clips.size());
                Clip clip;

                clip = clips.poll();
                if (clip == null) {
                    // no clips left;
                    surface.release();
                    finishClipTimer();
                    showStatusViews("Tap for Options",R.drawable.ic_video_50);
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
        //Very approximate... doesn't need to be exact
        chapterDuration = clips.size()*(4500);
        firstSurfaceUpdate = false;
        //We'll start the timer when the the surface gets its first update
        mDeterminate = mSlider.startDeterminate(1,0.f);

        trigger.onCompletion(mPlayer);
	}
    private long chapterDuration;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.preview, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {

        if (mChapter.clips.size() < StorageHandler.MIN_CHAPTER_SIZE) {
            menu.getItem(0).setEnabled(false);
        }

        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.publish_chapter_item:
            showStatusViews("Uploading Chapter...",R.drawable.ic_video_50);
            mGraceSlider = mSlider.startGracePeriod(new Slider.GracePeriod.Listener() {
                @Override
                public void onGracePeriodEnd() {
                    showStatusViews("Chapter Uploaded!",R.drawable.ic_done_50);
                    Intent intent = new Intent();
                    intent.setAction(ClipService.Action.CS_PUBLISH_CHAPTER.toString());
                    LocalBroadcastManager.getInstance(ClipPreviewActivity.this).sendBroadcast(intent);
                    finish();
                }

                @Override
                public void onGracePeriodCancel() {

                }
            });

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
    private boolean firstSurfaceUpdate;
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
        if(!firstSurfaceUpdate){
            firstSurfaceUpdate = true;
            mLoading.hide();
            startClipTimer(chapterDuration,250);
        }
	}

}
