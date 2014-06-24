package com.repco.perfect.glassapp.storage;

import java.util.Date;

import com.google.gson.annotations.Expose;
import com.repco.perfect.glassapp.base.Storable;

public final class Clip extends Storable{
	
	@Expose
	public final String videoPath;
	@Expose
	public final String previewPath;




	public Clip(String videoPath, String previewPath) {
		super();
		this.videoPath = videoPath;
		this.previewPath = previewPath;
	}

}