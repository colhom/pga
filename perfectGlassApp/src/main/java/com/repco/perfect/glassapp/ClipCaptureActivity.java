package com.repco.perfect.glassapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.base.ChapterStatusActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;

import java.io.File;

public class ClipCaptureActivity extends ChapterStatusActivity {


    @Override
    public int getLayoutId() {
        return R.layout.clip_capture;
    }

    private boolean clipSaved = false;
    private Clip mClip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clipSaved = false;
        mClip = null;
        captureClip();
    }

    private static final int PREVIEW_REQUEST_CODE = 1,
            CAPTURE_REQUEST_CODE = 2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAPTURE_REQUEST_CODE:
                showStatusViews("",R.drawable.ic_video_50);
                mClip = new Clip(
                        data.getStringExtra("outputFile"),
                        data.getStringExtra("previewFile")
                );

                Bitmap bg = BitmapFactory.decodeFile(mClip.previewPath);
                if(bg != null){
                    mBackgroundImageView.setImageBitmap(bg);
                }
                if (resultCode == RESULT_CANCELED) {
                    finish();
                }

                break;
            case PREVIEW_REQUEST_CODE:
                showStatusViews("",R.drawable.ic_video_50);
                break;

            default:

                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        Intent returnIntent = new Intent();
        if (clipSaved) {
            //Save clip --> send files to service, leave running
            returnIntent.setAction(ClipService.Action.CS_SAVE_CLIP.toString());
            Bundle b = new Bundle();
            b.putSerializable("clip", mClip);
            returnIntent.putExtras(b);

        } else {
            //No save clip --> delete files, stop service
            cleanupFiles();
            returnIntent.setAction(ClipService.Action.CS_STOP_SERVICE.toString());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
        closeOptionsMenu();
    }

    private void captureClip() {
        hideStatusView();
        if(mClip != null){
            cleanupFiles();
            mClip = null;
        }
        startActivityForResult(new Intent(this, ClipRecorderActivity.class),CAPTURE_REQUEST_CODE);

    }
    private void cleanupFiles(){
        if(mClip == null){
            return;
        }
        for (String path : new String[]{mClip.videoPath,mClip.previewPath}) {
            if(path == null) {
                continue;
            }
            File f = new File(path);
            if (f.exists()) {
                Log.i(LTAG, "Cleaning up " + f.getAbsolutePath());
                if (!f.delete()) {
                    throw new RuntimeException("Could not delete output file " + f.getAbsolutePath());
                }
            }else{
                Log.w(LTAG,"Could not find file "+path+", won't clean up");
            }
        }
    }



    @Override
    public int getVoiceMenuId() {
        return R.menu.capture_voice;
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater ifl = getMenuInflater();
		ifl.inflate(R.menu.capture, menu);
		return true;
	}

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if(featureId == WindowUtils.FEATURE_VOICE_COMMANDS){
            switch (item.getItemId()){
                case R.id.add_clip_voice_mi:

                    addClipToChapter();
                    break;

                case R.id.retake_clip_voice_mi:

                    captureClip();
                    break;
                case R.id.replay_clip_voice_mi:
                    replayClip();
                    break;
                case R.id.delete_clip_voice_mi:
                    finish();
                    break;
                default:
                    return true;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void addClipToChapter(){
        showStatusViews("Adding clip",R.drawable.ic_video_50);
        mGraceSlider = mSlider.startGracePeriod(new Slider.GracePeriod.Listener() {
            @Override
            public void onGracePeriodEnd() {
                showStatusViews("Clip Added!",R.drawable.ic_done_50);
                clipSaved = true;
                finish();
            }

            @Override
            public void onGracePeriodCancel() {
                System.out.println("[saveClip] grace period cancelled");
                clipSaved = false; // just to be "safe".
                finish();
            }
        });
    }

    private void replayClip(){
        hideStatusView();
        Chapter clipChapter = new Chapter();
        clipChapter.clips.add(mClip);

        Intent replayClip = new Intent(this,ClipPreviewActivity.class);
        Bundle b = new Bundle();
        b.putSerializable("chapter",clipChapter);
        replayClip.putExtras(b);

        startActivityForResult(replayClip, PREVIEW_REQUEST_CODE);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case R.id.add_clip_mi:
            addClipToChapter();
			break;
		case R.id.replay_clip_mi:
			replayClip();
			break;
		case R.id.retake_clip_mi:
            captureClip();
			break;
		default:
			return false;
		}
		am.playSoundEffect(Sounds.SELECTED);

		return true;
	}

    private static final String LTAG = ClipCaptureActivity.class.getSimpleName();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            openOptionsMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



}
