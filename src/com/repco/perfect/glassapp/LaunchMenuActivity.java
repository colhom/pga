package com.repco.perfect.glassapp;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;
import com.repco.perfect.glassapp.storage.StorageHandler;
import com.repco.perfect.glassapp.sync.SyncService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class LaunchMenuActivity extends BaseBoundServiceActivity {

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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.record_clip_item:
			mClipService.recordClip();
			break;
		case R.id.preview_chapter_item:
			mClipService.sendMessage(StorageHandler.GET_ACTIVE_CHAPTER, null);
			break;
		case R.id.stop_service_item:
			mClipService.stop();
			break;
		default:
			return false;
		}
		return true;
	
	}
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		openOptionsMenu();
	}
	@Override
	protected void onClipServiceConnected() {
		// TODO Auto-generated method stub
		
	}
}
  