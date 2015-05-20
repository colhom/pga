package com.repco.perfect.glassapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.base.ChapterSliderActivity;
import com.repco.perfect.glassapp.base.ChapterStatusActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by chom on 4/17/15.
 */
public class ChapterUploadActivity extends ChapterStatusActivity {

    public static final class ChapterUploadSliderActivity extends ChapterSliderActivity{
        @Override
        protected String getStatusText(boolean inProgress) {
            if(inProgress){
                return "Uploading";
            }else{
                return "Uploaded";
            }
        }

        @Override
        protected Drawable getStatusIcon(boolean inProgress) {
            int resId;
            if(inProgress){
                resId = R.drawable.ic_video_50;
            }else{
                resId = R.drawable.ic_done_50;
            }

            return getResources().getDrawable(resId);
        }
    }

    private final String LTAG=this.getClass().getSimpleName();

    private Chapter mChapter;
    private AudioManager mAudio;

    Bitmap[] previewReel;
    TimerTask mPreviewFlipper;
    Timer mPreviewTimer;
    private static final int UPLOAD_CHAPTER_SLIDER_REQUEST_CODE=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChapter = (Chapter) getIntent().getSerializableExtra("chapter");
        previewReel = new Bitmap[Math.min(mChapter.clips.size(), 10)];
        mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mStatusImageView.setImageResource(R.drawable.ic_video_50);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPreviewFlipper = new TimerTask() {

            int index=0;

            private final Runnable updateBackground = new Runnable() {
                @Override
                public void run() {
                    mBackgroundImageView.setImageBitmap(previewReel[index]);
                    index = (index+1) % previewReel.length;
                }
            };
            @Override
            public void run() {
                if(previewReel.length == 0){
                    return;
                }
                if(previewReel[index] == null){
                    Clip clip = mChapter.clips.get(index);
                    previewReel[index] = BitmapFactory.decodeFile(clip.previewPath);
                }
                runOnUiThread(updateBackground);
            }
        };

        mPreviewTimer = new Timer();

        mPreviewTimer.scheduleAtFixedRate(mPreviewFlipper, 0, 300);
        Intent uploadChapter = new Intent(this,ChapterUploadSliderActivity.class);
        boolean noWifi = false;

        ConnectivityManager connManager = (ConnectivityManager) ChapterUploadActivity.this
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo mWifi = connManager.getActiveNetworkInfo();

        if (mWifi == null) {
            Log.i(LTAG, "No active network connection");
            noWifi = true;
        }

        if (connManager.isActiveNetworkMetered()) {
            Log.i("LTAG",
                    "Active network connection is metered");
            noWifi = true;
        }

        if (noWifi){
            Intent warn = new Intent(ChapterUploadActivity.this,PublishWarningActivity.class);
            warn.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            mAudio.playSoundEffect(Sounds.ERROR);
            startActivity(warn);
        }else {
            startActivityForResult(uploadChapter, UPLOAD_CHAPTER_SLIDER_REQUEST_CODE);
        }
//        startActivityForResult(uploadChapter, UPLOAD_CHAPTER_SLIDER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case UPLOAD_CHAPTER_SLIDER_REQUEST_CODE:
                if (resultCode == RESULT_OK){
                    mAudio.playSoundEffect(Sounds.SUCCESS);
                    Intent intent = new Intent();
                    intent.setAction(ClipService.Action.CS_PUBLISH_CHAPTER.toString());
                    LocalBroadcastManager.getInstance(ChapterUploadActivity.this).sendBroadcast(intent);
                }
                finish();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPreviewFlipper.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPreviewTimer.cancel();
    }

    @Override
    public int getVoiceMenuId() {
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

}
