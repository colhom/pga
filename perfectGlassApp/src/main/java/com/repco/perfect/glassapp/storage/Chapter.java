package com.repco.perfect.glassapp.storage;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Multipart;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.annotations.Expose;
import com.repco.perfect.glassapp.DevData;
import com.repco.perfect.glassapp.base.Storable;

public final class Chapter extends Storable {

	@Expose
	public final List<Clip> clips;

    @Expose
    public boolean userpublished = false;

	public Chapter() {
		super("chapter");
		this.clips = new ArrayList<Clip>();
	}

	@Override
	protected Response makeSyncRequest() throws RetrofitError {

		return chapterService.postSyncData(new TypedString(getJSONData()), new TypedString(DevData.GOOGLE_ID));
	}

    @Override
    protected boolean doCleanup() {
        boolean noError = true;
        for(Clip clip : clips){
            noError = clip.doCleanup() && noError;
        }
        if(!noError){
            Log.e(LTAG, "Cleanup chapter " + uuid + " failed! Will be retried");
        }else{
            Log.i(LTAG,"Cleanup chapter "+uuid+" completed!");
        }
        return noError;

    }


    private static final ChapterService chapterService = storableAdatper.create(ChapterService.class);
	
	private interface ChapterService {
		@Multipart
		@PUT("/api/storable")
		Response postSyncData(@Part("json_data") TypedString jsonData,
				@Part("dummy_gid") TypedString dummyGid);
	}
	
}