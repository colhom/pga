package com.repco.perfect.glassapp.storage;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

import com.google.gson.annotations.Expose;
import com.repco.perfect.glassapp.base.Storable;

public final class Chapter extends Storable {

	@Expose
	public final List<Clip> clips;
	
	public Chapter() {
		super();
		this.clips = new ArrayList<Clip>();
	}

	
	
}