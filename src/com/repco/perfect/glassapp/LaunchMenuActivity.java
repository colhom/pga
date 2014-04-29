package com.repco.perfect.glassapp;

import java.io.File;

import com.google.android.glass.media.CameraManager;
import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

import android.content.Intent;
import android.provider.MediaStore;
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
			break;
		case R.id.stop_service_item:
			mClipService.kill();
			finish();
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
	protected void onClipServiceConnected() {}
}
  