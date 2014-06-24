package com.repco.perfect.glassapp.base;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import android.database.sqlite.SQLiteDatabase;

public abstract class Storable implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Expose
	public final String id;
	@Expose
	public final Date ts;
	
	public boolean dirty = true;
	public Storable(){
		this.id = UUID.randomUUID().toString();
		this.ts = new Date();
	}
	
	
	private static final Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	private static final Gson mPrettyGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
	public static Storable unmarshal(String type,String jsonData){
		try {
			
			Class<?> clz = Class.forName(type);
			
			assert(Storable.class.isAssignableFrom(clz));
			
			return (Storable) mGson.fromJson(jsonData, clz);
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getJSONData(){
		return mGson.toJson(this);
	}
	
	@Override
	public String toString(){
		return mPrettyGson.toJson(this);
	}
	
}
