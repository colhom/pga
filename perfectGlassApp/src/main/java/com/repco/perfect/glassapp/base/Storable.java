package com.repco.perfect.glassapp.base;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;

import com.github.julman99.gsonfire.DateSerializationPolicy;
import com.github.julman99.gsonfire.GsonFireBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.repco.perfect.glassapp.BuildConfig;
import com.repco.perfect.glassapp.DevData;
import com.repco.perfect.glassapp.sync.SyncService;

import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateFormat;
import android.util.Log;

public abstract class Storable implements Serializable{
	protected final String LTAG = getClass().getSimpleName();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Expose
	public final String uuid;
	
	@Expose
	public Date ts;

    @Expose
    public Date creationts;

	@Expose
	public boolean dirty = false;
	
	@Expose
	public final String objecttype;
	
	protected Storable(String objecttype){
		this.uuid = UUID.randomUUID().toString();
		this.ts = new Date();
        this.creationts = new Date();
		this.objecttype = objecttype;
	}
	
	
	//wow
	private static final Gson mGson = new GsonFireBuilder().
			dateSerializationPolicy(DateSerializationPolicy.rfc3339).createGsonBuilder().
			excludeFieldsWithoutExposeAnnotation().create();
	
	private static final Gson mPrettyGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
	public static Storable unmarshal(String type,String jsonData){
		try {
			
			Class<?> clz = Class.forName(type);
			
			if(BuildConfig.DEBUG && !Storable.class.isAssignableFrom(clz)){
				throw new RuntimeException(Storable.class.getCanonicalName()+" is not assignable from "+clz.getCanonicalName());
			}
			
			return (Storable) mGson.fromJson(jsonData, clz);
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getJSONData(){
		return mGson.toJson(this);
	}
	public static final RestAdapter storableAdatper = new RestAdapter.Builder()
			.setEndpoint(DevData.API_HOST).build();
	

	public final boolean doSync(){
		Response res;
		try{
			
			res = makeSyncRequest();
		}catch(RetrofitError e){
			e.printStackTrace();
			return false;
		}
		
		if(res.getStatus() == 200){
			Log.i(LTAG, "Successful sync!");
			return true;
		}
		Log.w(LTAG, "Retrofit: "+res.getStatus()+" : "+res.getReason());
		return false;
	}
	
	protected abstract Response makeSyncRequest() throws RetrofitError;
	
	@Override
    public String toString() { return uuid+" ("+objecttype+") "+ts;};

    public String jsonString(){
        return mPrettyGson.toJson(this);
    }
	
	
}
