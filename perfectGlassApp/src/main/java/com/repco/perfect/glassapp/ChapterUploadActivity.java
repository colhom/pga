package com.repco.perfect.glassapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.repco.perfect.glassapp.base.ChapterStatusActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by chom on 4/17/15.
 */
public class ChapterUploadActivity extends ChapterStatusActivity {
    private final String LTAG=this.getClass().getSimpleName();

    private Chapter mChapter;
    private AudioManager mAudio;

    Bitmap[] previewReel;
    TimerTask mPreviewFlipper;
    Timer mPreviewTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChapter = (Chapter) getIntent().getSerializableExtra("chapter");
        previewReel = new Bitmap[Math.min(mChapter.clips.size(), 10)];

        showStatusViews("", R.drawable.ic_video_50);
        mLoading.show();
        mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
        mLoading.hide();
        showStatusViews("Uploading", R.drawable.ic_video_50);
        mGraceSlider = mSlider.startGracePeriod(new Slider.GracePeriod.Listener() {

            @Override
            public void onGracePeriodEnd() {
                mGraceSlider = null;

                Intent intent = new Intent();
                intent.setAction(ClipService.Action.CS_PUBLISH_CHAPTER.toString());
                LocalBroadcastManager.getInstance(ChapterUploadActivity.this).sendBroadcast(intent);

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
                }else{
                    showStatusViews("Uploaded",R.drawable.ic_done_50);
                    mAudio.playSoundEffect(Sounds.SUCCESS);

                }

                finish();

            }

            @Override
            public void onGracePeriodCancel() {
                mGraceSlider = null;
                showStatusViews("",R.drawable.ic_video_50);
                finish();
            }
        });
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
    public View getContentView() {
        return getLayoutInflater().inflate(R.layout.clip_capture,null);
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
