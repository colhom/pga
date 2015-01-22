package com.repco.perfect.glassapp.storage;

import java.io.File;
import java.util.Date;

import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Multipart;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;

import com.google.gson.annotations.Expose;
import com.repco.perfect.glassapp.DevData;
import com.repco.perfect.glassapp.base.Storable;

public final class Clip extends Storable{
	
	@Expose
	public final String videoPath;
	@Expose
	public final String previewPath;




	public Clip(String videoPath, String previewPath) {
		super("clip");
		this.videoPath = videoPath;
		this.previewPath = previewPath;
	}
	
	private static final ClipService clipService = storableAdatper.create(ClipService.class);
	
	@Override
	protected Response makeSyncRequest() throws RetrofitError {
		File videoFile = new File(videoPath);
		
		if(!videoFile.exists()){
			throw new RuntimeException("Invalid video path for clip: "+getJSONData());
		}
		
		return clipService.postSyncData(new TypedString(getJSONData()),
					new TypedString(DevData.GOOGLE_ID),
					new TypedFile("video/mp4", videoFile)
				);
	}
	
	private interface ClipService {
		@Multipart
		@PUT("/api/storable")
		Response postSyncData(@Part("json_data") TypedString jsonData,
				@Part("dummy_gid") TypedString dummyGid,
				@Part("clip") TypedFile clipFile
				);
	}
	


}