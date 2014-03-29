package com.repco.perfect.glassapp;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
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
		case R.id.stop_service_item:
			mClipService.stop();
			break;
		default:
			return false;
		}
		return true;
	
	}
}
  