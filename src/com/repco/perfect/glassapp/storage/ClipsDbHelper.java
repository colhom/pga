package com.repco.perfect.glassapp.storage;

import java.sql.Timestamp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ClipsDbHelper extends SQLiteOpenHelper {

	public static class UserDataException extends Exception{
		private static final long serialVersionUID = 6150296549294621725L;
		
		public UserDataException(String message){
			super(message);
		}
		
		public UserDataException(){
			this("An error has occurred!");
		}

	}
	private static final int MAX_CLIPS = 50;
	private static final int MIN_CLIPS = 5;
	
	private static final String DB_NAME = "Clips.db";
	private static final int DB_VERSION = 1;
	
	private static enum ClipTable {
		PATH,
		CHAPTER_ID,
		SYNC_TS,
		TS;
		@Override
		public String toString(){
			return name().toLowerCase();
		}
		
		public static final String name = "clips";
	}
	
	private static enum ChapterTable{
		ID,
		SYNC_TS,
		END_TS,
		DONE,
		TS;
		
		@Override
		public String toString(){
			return name().toLowerCase();
		}
		
		public static final String name = "chapters";
	}
	private static final String[] DB_INIT = {

				"CREATE TABLE "+ClipTable.name+"("
				+ ClipTable.PATH +" STRING PRIMARY KEY, "
				+ ClipTable.CHAPTER_ID+" INTEGER NOT NULL,"
				+ ClipTable.SYNC_TS + " STRING, "
				+ ClipTable.TS + " STRING DEFAULT CURRENT_TIMESTAMP,"
				+ "FOREIGN KEY("+ClipTable.CHAPTER_ID+") references "+ChapterTable.name+"("+ChapterTable.ID+") "
				+ "CREATE INDEX ic1 on "+ClipTable.name+"("+ClipTable.CHAPTER_ID+") "
				+ "CREATE INDEX ic2 on "+ClipTable.name+"("+ClipTable.TS+") ",
				
				
				"CREATE TABLE "+ChapterTable.name+"("
				+ ChapterTable.ID +"INT PRIMARY KEY DESC "
				+ ChapterTable.SYNC_TS + " STRING "
				+ ChapterTable.TS+" DEFAULT CURRENT_TIMESTAMP "
				+ ChapterTable.END_TS+" STRING "
				+ ChapterTable.DONE+" INTEGER DEFAULT 0 "
				+ "CREATE INDEX ich1 on "+ChapterTable.name+"("+ChapterTable.DONE+") ",
		
	};

	public ClipsDbHelper(Context context){
		super(context,DB_NAME,null,DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		for(String cmd : DB_INIT){
			System.out.println(cmd);
			db.execSQL(cmd);
		}
		ContentValues initialChapter = new ContentValues();
		
		
		long id = db.insert(ChapterTable.name, null, initialChapter);
		if(id == -1){
			throw new RuntimeException("Could not insert initial chapter");
		}
	}

	private Cursor getInProgressChapter(SQLiteDatabase db){
		Cursor c = db.query(
				ChapterTable.name,
				null,
				"where "+ChapterTable.DONE.toString()+"=0",
				null,
				null,
				null,
				null);
		
		if(c.getCount() != 1){
			c.close();
			throw new RuntimeException("Corrupt database, found "+c.getCount()+" in-progress chapters");
		};
		
		c.moveToFirst();
		
		return c;
	}
	public void insertClip(String path){

		SQLiteDatabase db = getWritableDatabase();
		try{
			db.beginTransaction();
			
			

			
			ContentValues cv = new ContentValues();
			cv.put(ClipTable.PATH.toString(),path);
			
			long chapterId;
			Cursor chapter = getInProgressChapter(db);
			try{
				chapterId = chapter.getLong(chapter.getColumnIndexOrThrow(ChapterTable.ID.toString()));
			}finally{
				chapter.close();
			}
			
			
			cv.put(ClipTable.CHAPTER_ID.toString(), chapterId);
			long id = db.insert(ClipTable.name, null, cv);
			assert(id != -1);
			
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
			db.close();
		}

	}
	
	private int getClipCount(SQLiteDatabase db, long chapterId){
		Cursor c = db.rawQuery(
				"select count(*) from "+ClipTable.name+" where "+ClipTable.CHAPTER_ID+"="+Long.toString(chapterId),null);
		try{
			c.moveToFirst();
			return c.getInt(0);
		}finally{
			c.close();
		}
	}

	public int getClipCount(long chapterId){
		SQLiteDatabase db = getReadableDatabase();
		try{
			return getClipCount(db,chapterId);
		}finally{
			db.close();
		}
	}
	
	public int getInProgressClipCount(){
		SQLiteDatabase db = getReadableDatabase();
		try{
			Cursor chapter = getInProgressChapter(db);
			try{
				return getClipCount(
						db, 
						chapter.getLong(
								chapter.getColumnIndexOrThrow(ChapterTable.ID.toString())
								));
			}finally{
				chapter.close();
			}
		}finally{
			db.close();
		}
	}
	public void endChapter() throws UserDataException{

		SQLiteDatabase db = getWritableDatabase();
		try{
			db.beginTransaction();
			
			Cursor chapter = getInProgressChapter(db);
			try{
				long chapterId = chapter.getLong(chapter.getColumnIndexOrThrow(ChapterTable.ID.toString()));
				if(getClipCount(db, chapterId) < MIN_CLIPS){
					throw new UserDataException("Chapters need at least "+MIN_CLIPS+" clips");
				}				
				ContentValues cv = new ContentValues();
				cv.put(ChapterTable.DONE.toString(), true);
				cv.put(ChapterTable.END_TS.toString(), new Timestamp(System.currentTimeMillis()).toString());
				
				assert(db.update(ChapterTable.name, cv, "where id="+Long.toString(chapterId),null) == 1);
				db.setTransactionSuccessful();
				
			}finally{
				chapter.close();
			}
		}finally{
			db.endTransaction();
			db.close();
		}

	}
	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		
	}

}
