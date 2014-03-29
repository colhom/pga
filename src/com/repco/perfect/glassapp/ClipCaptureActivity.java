package com.repco.perfect.glassapp;

import java.util.Timer;
import java.util.TimerTask;

import com.repco.perfect.glassapp.base.BaseBoundServiceActivity;

import android.app.Activity;
import android.os.Bundle;

public class ClipCaptureActivity extends BaseBoundServiceActivity {
	
	private final Timer timer = new Timer();
			
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.capture);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				finish();
			}
		}, 3000);
	}
	

}
