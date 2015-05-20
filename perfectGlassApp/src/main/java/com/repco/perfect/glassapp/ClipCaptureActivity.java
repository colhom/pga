package com.repco.perfect.glassapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.Slider;
import com.repco.perfect.glassapp.base.ChapterSliderActivity;
import com.repco.perfect.glassapp.base.ChapterStatusActivity;
import com.repco.perfect.glassapp.storage.Chapter;
import com.repco.perfect.glassapp.storage.Clip;

import java.io.File;

public class ClipCaptureActivity extends ChapterStatusActivity {

    public static final class ClipAddSliderActivity extends ChapterSliderActivity{

        @Override
        protected String getStatusText(boolean inProgress) {
            if(inProgress){
                return "Adding";
            }else{
                return "Added";
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

    public static final class ClipDeleteSliderActivity extends ChapterSliderActivity{

        @Override
        protected String getStatusText(boolean inProgress) {
            if(inProgress){
                return "Deleting";
            }else{
                return "Deleted";
            }
        }

        @Override
        protected Drawable getStatusIcon(boolean inProgress) {
            int resId;
            if(inProgress){
                resId = R.drawable.ic_delete_50;
            }else{
                resId = R.drawable.ic_done_50;
            }
            return getResources().getDrawable(resId);
        }
    }

    private boolean clipSaved = false;
    private Clip mClip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clipSaved = false;
        mClip = null;
        mStatusImageView.setImageResource(R.drawable.ic_video_100);
        captureClip();
    }

    private static final int PREVIEW_REQUEST_CODE = 1,
            CAPTURE_REQUEST_CODE = 2, ADD_CLIP_SLIDER_REQUEST_CODE =3,
    DELETE_CLIP_SLIDER_REQUEST_CODE=4;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAPTURE_REQUEST_CODE:
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
                break;

            case ADD_CLIP_SLIDER_REQUEST_CODE:
                clipSaved = (resultCode == RESULT_OK);
                if(clipSaved){
                    finish();
                }
                break;
            case DELETE_CLIP_SLIDER_REQUEST_CODE:
                boolean clipDeleted = (resultCode == RESULT_OK);
                if(clipDeleted){
                    am.playSoundEffect(Sounds.SUCCESS);
                    clipSaved = false;
                    finish();
                }
                break;
            default:

                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


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
            returnIntent.setAction(ClipService.Action.CS_MAYBE_STOP_SERVICE.toString());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
        closeOptionsMenu();
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
                    deleteClip();
                    break;
                default:
                    return true;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void addClipToChapter(){
        Intent addClip = new Intent(this,ClipAddSliderActivity.class);
        startActivityForResult(addClip,ADD_CLIP_SLIDER_REQUEST_CODE);
    }
    private void captureClip() {
        if(mClip != null){
            cleanupFiles();
            mClip = null;
        }
        startActivityForResult(new Intent(this, ClipRecorderActivity.class),CAPTURE_REQUEST_CODE);
    }
    private void replayClip(){

        Chapter clipChapter = new Chapter();
        clipChapter.clips.add(mClip);

        Intent replayClip = new Intent(this,ClipPreviewActivity.class);
        Bundle b = new Bundle();
        b.putSerializable("chapter",clipChapter);
        replayClip.putExtras(b);

        startActivityForResult(replayClip, PREVIEW_REQUEST_CODE);
    }

    private void deleteClip(){
        Intent deleteClip = new Intent(this,ClipDeleteSliderActivity.class);
        startActivityForResult(deleteClip,DELETE_CLIP_SLIDER_REQUEST_CODE);
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
        case R.id.delete_clip_mi:
            deleteClip();
            break;
		default:
			return false;
		}
		am.playSoundEffect(Sounds.SELECTED);

		return true;
	}

    private static final String LTAG = ClipCaptureActivity.class.getSimpleName();




}
