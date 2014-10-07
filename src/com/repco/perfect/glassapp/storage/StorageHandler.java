package com.repco.perfect.glassapp.storage;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.repco.perfect.glassapp.ClipService;
import com.repco.perfect.glassapp.base.Storable;
import com.repco.perfect.glassapp.sync.SyncService;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.GetChars;
import android.util.Log;

public class StorageHandler extends SQLiteOpenHelper {
	public static final int MIN_CHAPTER_SIZE = 5;
	public static final int PUSH_CLIP = 0, GET_CHAPTERS = 1,
			RECEIVE_CHAPTERS = 2, GET_ACTIVE_CHAPTER = 3,
			RECEIVE_ACTIVE_CHAPTER = 4, END_CHAPTER = 5,GET_NEXT_STORABLE=6,RECEIVE_NEXT_STORABLE=7;

	private static String DB_NAME = "PerfectDB";
	private static int DB_VERSION = 1;

	SQLiteDatabase mDb = null;

	private final Handler mHandler;
	public final Messenger mMessenger;

	public StorageHandler(Context context) {
		super(context, DB_NAME, null, DB_VERSION);

		mDb = getWritableDatabase();
		// TODO:start sync thread

		HandlerThread ht = new HandlerThread("StorageHandler");
		ht.start();
		mHandler = new Handler(ht.getLooper(), mCallback);
		mMessenger = new Messenger(mHandler);
	}

	private final Handler.Callback mCallback = new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			boolean delivered = false;
			Message reply = null;
			Chapter active = null;

			try {

				switch (msg.what) {
				case PUSH_CLIP:
					mDb.beginTransaction();

					Clip clip = (Clip) msg.obj;
					upsertRow(clip);
					active = getActiveChapter();

					if (active == null) {
						active = new Chapter();
						Log.i(LTAG, "Creating new Chapter");
					}

					active.clips.add(clip);

					upsertRow(active);
					delivered = true;
					mDb.setTransactionSuccessful();
					requestSync();
					Log.i(LTAG, "PUSH_CLIP transcaction completed");

					break;
				case END_CHAPTER:
					active = getActiveChapter();
					if (active != null
							&& active.clips.size() < MIN_CHAPTER_SIZE) {
						System.err.println("END_CHAPTER with clip count "
								+ active.clips.size() + " minimum size is "
								+ MIN_CHAPTER_SIZE);
						delivered = false;
					} else {
						upsertRow(new Chapter());
						delivered = true;
						requestSync();
					}
					break;
				case GET_CHAPTERS:
					List<Chapter> chapters = getChapters();
					reply = Message.obtain(null, RECEIVE_CHAPTERS, chapters);
					delivered = true;
					break;
				case GET_ACTIVE_CHAPTER:
					active = getActiveChapter();
					Log.i(LTAG, "Get active chapter: " + active);
					reply = Message
							.obtain(null, RECEIVE_ACTIVE_CHAPTER, active);

					delivered = true;
					break;
				case GET_NEXT_STORABLE:
					Storable next = getNextStorable();
					Log.i(LTAG,"Get next storable: "+next);
					reply = Message.obtain(null,RECEIVE_NEXT_STORABLE,next);
					delivered = true;
					break;
				default:
					break;
				}

				if (reply != null) {
					try {
						msg.replyTo.send(reply);
					} catch (RemoteException e) {
						Log.e("StorageHandler", "could not send reply: "
								+ reply, e);
					}
				}

				return delivered;
			} finally {
				if (mDb.inTransaction()) {
					Log.i(LTAG, "Ending transaction");
					mDb.endTransaction();
				}else{
					Log.i(LTAG,"No transaction, will not end");
				}
			}
		}
	};
	
	private void requestSync(){
		Bundle extras = new Bundle();
		extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		
		ContentResolver.requestSync(ClipService.ACCOUNT,ClipService.AUTHORITY,extras);
	}
	public static final String TABLE_NAME = "storables";
	public static final String JSON_DATA_KEY = "data", TS_DATA_KEY = "ts",
			ID_KEY = "uuid", TYPE_KEY = "type", DIRTY_KEY = "dirty";

	@Override
	public void onCreate(SQLiteDatabase db) {

		String createTable = "CREATE TABLE " + TABLE_NAME + " (" + ID_KEY
				+ " TEXT PRIMARY KEY, " + JSON_DATA_KEY + " TEXT NOT NULL, "
				+ TYPE_KEY + " TEXT NOT NULL, " + DIRTY_KEY
				+ " INTEGER NOT NULL," + TS_DATA_KEY + " INTEGER NOT NULL"
				+ ")";

		String[] createIndices = new String[] {
				"CREATE INDEX idx1 ON " + TABLE_NAME + "(" + TS_DATA_KEY
						+ " ASC);",
				"CREATE INDEX idx2 on " + TABLE_NAME + "(" + TYPE_KEY + ");",
				"CREATE INDEX idx3 on " + TABLE_NAME + "(" + DIRTY_KEY + ");" };
		Log.i(LTAG, createTable);
		db.execSQL(createTable);
		for (String idx : createIndices) {
			Log.i(LTAG, idx);
			db.execSQL(idx);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		mHandler.getLooper().quit();

		// TODO: stop sync thread

		super.close();
	}

	private static final String selectOne = "SELECT * FROM " + TABLE_NAME
			+ " WHERE " + ID_KEY + "=\"%s\"";

	private static final String LTAG = StorageHandler.class.getSimpleName();

	private void upsertRow(Storable row) {

		Cursor c = null;
		boolean exists;
		try {
			c = mDb.rawQuery(String.format(selectOne, row.id), null);
			exists = c.moveToFirst();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		Log.i(LTAG, "upsertRow : " + row + " --> exists : " + exists);

		ContentValues cv = new ContentValues();
		cv.put(ID_KEY, row.id);
		cv.put(TYPE_KEY, row.getClass().getName());
		cv.put(JSON_DATA_KEY, row.getJSONData());
		cv.put(TS_DATA_KEY, row.ts.getTime());
		cv.put(DIRTY_KEY, row.dirty ? 1 : 0);

		Log.i(LTAG, cv.toString());
		if (exists) {
			int rowsAffected = mDb.update(TABLE_NAME, cv, ID_KEY + "=?",
					new String[] { row.id });

			Log.i(LTAG, "upsertRow update " + rowsAffected + " rows affected");
			assert (rowsAffected == 1);
		} else {
			long rowId = mDb.insert(TABLE_NAME, null, cv);
			Log.i(LTAG, "upsertRow insert return rowid " + rowId);
			assert (rowId != -1);
		}

	}

	private static final String selectChapters = "SELECT * FROM " + TABLE_NAME
			+ " WHERE " + TYPE_KEY + "=\"%s\" ORDER BY " + TS_DATA_KEY + " ASC";

	private List<Chapter> getChapters() {
		String sql = String.format(selectChapters, Chapter.class.getName());

		Cursor c = null;
		try {
			c = mDb.rawQuery(sql, null);

			List<Chapter> chapters = new LinkedList<Chapter>();

			while (c.moveToNext()) {

				chapters.add((Chapter) unmarshalStorable(c));
			}

			return chapters;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	private Chapter getActiveChapter() {
		Cursor c = null;
		try {
			c = mDb.query(TABLE_NAME, null, TYPE_KEY + "=?",
					new String[] { Chapter.class.getName() }, null, null,
					TS_DATA_KEY + " DESC", "1");
			if (c.moveToFirst()) {
				return (Chapter) unmarshalStorable(c);
			} 
			return null;
			
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
	
	private Storable getNextStorable(){
		Cursor c = null;
		try{
			c = mDb.query(TABLE_NAME, null, DIRTY_KEY+"=1",null,null, TS_DATA_KEY+" DESC","1");
			if (c.moveToFirst()){
				return (Storable) unmarshalStorable(c);
			}
			return null;
		}finally{
			if (c != null){
				c.close();
			}
		}
	}

	private static Storable unmarshalStorable(Cursor c) {
		String jsonData = c.getString(c.getColumnIndexOrThrow(JSON_DATA_KEY));
		String type = c.getString(c.getColumnIndexOrThrow(TYPE_KEY));
		return Storable.unmarshal(type, jsonData);
	}

}
