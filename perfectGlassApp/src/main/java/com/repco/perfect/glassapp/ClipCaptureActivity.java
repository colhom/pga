package com.repco.perfect.glassapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.WindowManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.base.ChapterImmersionActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

public class ClipCaptureActivity extends ChapterImmersionActivity implements
		MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener,
		SurfaceTextureListener {


	private TextureView mTextureView = null;

	private Surface mSurface = null;

    @Override
    public int getLayoutId() {
        return R.layout.video_immersion;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        System.out.println("on create");
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTextureView = (TextureView) findViewById(R.id.video_texture_view);
        mTextureView.setSurfaceTextureListener(this);
        doneRecording = false;
	}


    @SuppressLint("TrulyRandom")
	private final SecureRandom rand = new SecureRandom();

	File outputFile;

	private Bitmap mPreview;

    private boolean doneRecording;
	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		switch (what) {
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:

            am.playSoundEffect(Sounds.SUCCESS);

            showStatusViews("Tap for Options",R.drawable.ic_video_50);

            finishClipTimer();

			resetRecorder();

			mPreview = ThumbnailUtils.createVideoThumbnail(
					outputFile.getAbsolutePath(), Thumbnails.FULL_SCREEN_KIND);


			doneRecording = true;
			break;
		}

	}

	@Override
	public void openOptionsMenu() {
        if(mSurface.isValid()) {
            if (mPreview != null) {
                Canvas c = mSurface.lockCanvas(null);
                try {
                    c.drawBitmap(mPreview, (float) 0.0, (float) 0.0, null);
                } finally {
                    mSurface.unlockCanvasAndPost(c);
                }
            }
            super.openOptionsMenu();
        }else{
            Log.w(LTAG,"not opening options menu because surface is invalid. assuming we're dead");
        }
	}

	@Override
	protected void onStop() {
        super.onStop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        destroy();
        closeOptionsMenu();
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

    private void saveClip(){
        File previewFile = new File(outputFile.getAbsolutePath() + ".thumb.jpg");

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Bitmap preview = Bitmap.createScaledBitmap(mPreview, size.x,
                size.y, true);
        FileOutputStream previewStream = null;
        try {
            previewStream = new FileOutputStream(previewFile);
            preview.compress(Bitmap.CompressFormat.JPEG, 50,previewStream);
        }catch(FileNotFoundException e){
            throw new RuntimeException(e);
        }finally {
            if(previewStream != null){
                try {
                    previewStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Intent returnIntent = new Intent();
        returnIntent.setAction(ClipService.Action.CS_SAVE_CLIP.toString());
        returnIntent.putExtra("clipPath",outputFile.getAbsolutePath());
        returnIntent.putExtra("previewPath",previewFile.getAbsolutePath());
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
        mCleanup = false;
        finish();

    }
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case R.id.keep_clip_mi:
            showStatusViews("Adding clip",R.drawable.ic_video_50);
            mGraceSlider = mSlider.startGracePeriod(new Slider.GracePeriod.Listener() {
                @Override
                public void onGracePeriodEnd() {
                    showStatusViews("Clip Added!",R.drawable.ic_done_50);
                    saveClip();
                }

                @Override
                public void onGracePeriodCancel() {
                    System.out.println("[saveClip] grace period cancelled");
                }
            });

			break;
		case R.id.replay_clip_mi:
            hideStatusView();
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
                    startClipTimer(CLIP_DURATION+100,TIMER_UPDATE_INTERVAL);
                    mp.start();
				}
			});
			
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
                    finishClipTimer();
					mp.setSurface(null);
					mp.release();
					showStatusViews("Tap for Options",R.drawable.ic_video_50);
				}
			});
			mp.prepareAsync();
			
			break;
		case R.id.retake_clip_mi:
            Intent intent = getIntent();
			finish();
            startActivity(intent);
			break;
		default:
			return false;
		}
		am.playSoundEffect(Sounds.SELECTED);

		return true;
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		Log.e(getClass().getSimpleName(), "MediaRecorder onError: " + what
				+ " " + extra);
		finish();
	}




	private MediaRecorder mRec;
	private static final String LTAG = ClipCaptureActivity.class.getSimpleName();


	private synchronized void destroy(){

		if(mRec != null){
			Log.d(getClass().getSimpleName(),"->destroying media recorder");
			try{
				mRec.stop();
			}catch(IllegalStateException e){
				Log.d(getClass().getSimpleName(),"media recorder already stopped");
			}
            mRec.reset();
			mRec.release();
			mRec = null;
		}

		
		if(mCleanup){
			if(outputFile != null && outputFile.exists()){
				Log.i(LTAG, "Cleaning up "+outputFile.getAbsolutePath());
				if(!outputFile.delete()){
					throw new RuntimeException("Could not delete output file "+outputFile.getAbsolutePath());
				}
			}
		}
	}

	private boolean mCleanup = true;
    private static final int CLIP_DURATION = 4000;
    private static final int TIMER_UPDATE_INTERVAL = 250;
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture texture, int width,
			int height) {
		
		mSurface = new Surface(texture);
		
		mRec = new MediaRecorder();

		mRec.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mRec.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

		mRec.setPreviewDisplay(mSurface);

		mRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		mRec.setMaxDuration(CLIP_DURATION);

		outputFile = new File(getFilesDir().getAbsolutePath() + "/"
				+ new BigInteger(130, rand).toString(32) + ".mp4");

		try {
			if (!outputFile.createNewFile()) {
				throw new RuntimeException(outputFile.getAbsolutePath()+" already exists!");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		mRec.setOutputFile(outputFile.getAbsolutePath());
		mCleanup = true;
		
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
        mDeterminate = mSlider.startDeterminate(1, 0.f);
        firstSurfaceUpdate = false;
	}

    private boolean firstSurfaceUpdate;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            if(doneRecording){
                openOptionsMenu();
            }else{
                am.playSoundEffect(Sounds.DISALLOWED);
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        System.out.println("Surface texture destroyed");
        mSurface.release();
		return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
        if(!firstSurfaceUpdate){
            firstSurfaceUpdate = true;
            startClipTimer(CLIP_DURATION+500,TIMER_UPDATE_INTERVAL);
        }

    }


}
