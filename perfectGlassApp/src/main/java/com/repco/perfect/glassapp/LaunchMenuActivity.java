package com.repco.perfect.glassapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.repco.perfect.glassapp.base.ChapterActivity;
import com.repco.perfect.glassapp.storage.Chapter;

public class LaunchMenuActivity extends ChapterActivity {
    private final String LTAG=this.getClass().getSimpleName();

    private Chapter mChapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChapter = (Chapter) getIntent().getSerializableExtra("chapter");
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.launcher, menu);
		return true;
	}
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// TODO Auto-generated method stub
		super.onOptionsMenuClosed(menu);
		finish();
	}

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {

        if (mChapter != null) {
            boolean canPreview = (mChapter.clips.size() > 0);
            menu.getItem(0).setEnabled(canPreview);


            boolean canPublish = (mChapter.clips.size() >= 3);
            menu.getItem(1).setEnabled(canPublish);
        }

        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {

        ClipService.Action action;
        switch(item.getItemId()){
            case R.id.preview_chapter_item:
                action = ClipService.Action.CS_PREVIEW_CHAPTER;
                break;
            case R.id.write_chapter_item:
                action = ClipService.Action.CS_PUBLISH_CHAPTER;
                break;
            case R.id.stop_service_item:
                action= ClipService.Action.CS_STOP_SERVICE;
                break;
            default:
                action = null;
                break;
        }

        if(action == null){
            Log.e(LTAG,"Could not find action for menu item id ="+item.getItemId());
            return false;
        }
        Intent intent = new Intent();
        intent.setAction(action.toString());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		return true;
	
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
        Log.i(LTAG, "onAttachedToWindow "+mChapter.clips.size());
		openOptionsMenu();
	}

}
  