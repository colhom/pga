package com.repco.perfect.glassapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.google.android.glass.media.Sounds;
import com.repco.perfect.glassapp.base.ChapterSurfaceActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by chom on 4/17/15.
 */
public class ClipRecorderActivity extends ChapterSurfaceActivity implements MediaRecorder.OnErrorListener,MediaRecorder.OnInfoListener{
    private static final int CLIP_DURATION = 4000;
    private static final int TIMER_UPDATE_INTERVAL = 250;


    private Surface mSurface;

    private int mResultCode;
    private Intent mResultData;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mResultCode = RESULT_CANCELED;
        mResultData = new Intent();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTextureView.setSurfaceTextureListener(this);

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

        mResultData.putExtra("outputFile",outputFile.getAbsolutePath());
        setResult(mResultCode,mResultData);

        mRec.setOutputFile(outputFile.getAbsolutePath());


        Log.i(getClass().getSimpleName(), "Output path is " + outputFile);
        mRec.setOnInfoListener(this);
        am.playSoundEffect(Sounds.SELECTED);


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

    private Bitmap mPreview;

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:

                finishClipTimer();

                detachRecorder();

                mPreview = ThumbnailUtils.createVideoThumbnail(
                        outputFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);

                if(mPreview != null) {
                    File previewFile = new File(outputFile.getAbsolutePath() + ".thumb.jpg");
                    am.playSoundEffect(Sounds.SUCCESS);

                    Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                            .getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);

                    Bitmap preview = Bitmap.createScaledBitmap(mPreview, size.x,
                            size.y, true);
                    FileOutputStream previewStream = null;
                    try {
                        previewStream = new FileOutputStream(previewFile);
                        preview.compress(Bitmap.CompressFormat.JPEG, 50, previewStream);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (previewStream != null) {
                            try {
                                previewStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    mResultCode = RESULT_OK;
                    mResultData.putExtra("previewFile", previewFile.getAbsolutePath());
                }


                setResult(mResultCode,mResultData);
                finish();
                break;
        }

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
            mLoading.hide();
            startClipTimer(CLIP_DURATION+500,TIMER_UPDATE_INTERVAL);
        }

    }



    @SuppressLint("TrulyRandom")
    private final SecureRandom rand = new SecureRandom();

    File outputFile;

    private void detachRecorder(){
        if(mRec != null) {
            Log.d(getClass().getSimpleName(), "->destroying media recorder");
            try {
                mRec.stop();
            } catch (IllegalStateException e) {
                Log.d(getClass().getSimpleName(), "media recorder already stopped");
            }
            mRec.release();
            mRec = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        detachRecorder();

    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(getClass().getSimpleName(), "MediaRecorder onError: " + what
                + " " + extra);

        mResultCode = RESULT_CANCELED;
        mResultData.putExtra("error","media recorder");
        setResult(mResultCode,mResultData);
        finish();
    }


    private MediaRecorder mRec;
    private static final String LTAG = ClipRecorderActivity.class.getSimpleName();


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            am.playSoundEffect(Sounds.DISALLOWED);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
