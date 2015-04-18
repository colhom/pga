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
import com.repco.perfect.glassapp.base.ChapterSurfaceActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;
import com.repco.perfect.glassapp.storage.StorageHandler;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClipPreviewActivity extends ChapterSurfaceActivity {

	private static final String LTAG = ClipPreviewActivity.class
			.getSimpleName();
    private MediaPlayer mPlayer;

    private Chapter mChapter;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPlayer = new MediaPlayer();
        mChapter = (Chapter) getIntent().getSerializableExtra("chapter");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTextureView.setSurfaceTextureListener(this);
	}

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlayer.release();
        }
        finish();
    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
		final Surface surface = new Surface(mTextureView.getSurfaceTexture());
		mPlayer.setSurface(surface);


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
        //Very approximate... doesn't need to be exact
        chapterDuration = clips.size()*(4500);
        firstSurfaceUpdate = false;
        //We'll start the timer when the the surface gets its first update
        mDeterminate = mSlider.startDeterminate(1,0.f);

        trigger.onCompletion(mPlayer);
	}
    private long chapterDuration;

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
            startClipTimer(chapterDuration, 250);
        }
	}

}
