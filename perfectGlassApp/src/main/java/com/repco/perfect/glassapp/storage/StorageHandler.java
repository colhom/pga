package com.repco.perfect.glassapp.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.repco.perfect.glassapp.BuildConfig;
import com.repco.perfect.glassapp.ClipService;
import com.repco.perfect.glassapp.base.Storable;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class StorageHandler extends SQLiteOpenHelper {
	public static final int MIN_CHAPTER_SIZE = 3;
	public static final int PUSH_CLIP = 0, GET_ACTIVE_CHAPTER = 3,
			RECEIVE_ACTIVE_CHAPTER = 4, END_CHAPTER = 5, GET_NEXT_STORABLE = 6,
			RECEIVE_NEXT_STORABLE = 7, PUSH_STORABLE = 8, RECEIVE_END_CHAPTER = 9,
            CLEANUP_PUBLISHED_CHAPTERS = 10;

	private static String DB_NAME = "PerfectDB";
	private static int DB_VERSION = 1;

	SQLiteDatabase mDb = null;

	private final Handler mHandler;
	public final Messenger mMessenger;

	public StorageHandler(Context context) {
		super(context, DB_NAME, null, DB_VERSION);

		mDb = getWritableDatabase();

		HandlerThread ht = new HandlerThread(getClass().getSimpleName());
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
                    if (active.ts.before(clip.ts)){
                        active.ts = new Date(clip.ts.getTime() + 1000);
                        Log.i(LTAG,"Set back active chapter ts to "+active.ts);
                    }
					active.clips.add(clip);
                    active.dirty = true;
					upsertRow(active);
					delivered = true;
					mDb.setTransactionSuccessful();
					Log.i(LTAG, "PUSH_CLIP transcaction completed");

					break;
				case END_CHAPTER:
					active = getActiveChapter();
                    reply = Message.obtain(null,RECEIVE_END_CHAPTER);
					if (active.clips.size() < MIN_CHAPTER_SIZE) {
						Log.e(LTAG,"END_CHAPTER with clip count "
								+ active.clips.size() + " minimum size is "
								+ MIN_CHAPTER_SIZE);

					} else {
						active.dirty = true;
                        active.userpublished = true;
						upsertRow(active);
						upsertRow(new Chapter());
						delivered = true;
                        reply.obj = active;
					}
					break;

				case GET_ACTIVE_CHAPTER:
					active = getActiveChapter();
					Log.i(LTAG, "Get active chapter: " + active);
					reply = Message
							.obtain(null, RECEIVE_ACTIVE_CHAPTER, active);
                    reply.arg1 = msg.arg1;
					delivered = true;
					break;
				case GET_NEXT_STORABLE:
					Storable next = getNextStorable();
					Log.i(LTAG, "Get next storable: " + next);

					reply = Message.obtain(null, RECEIVE_NEXT_STORABLE, next);
					delivered = true;
					break;

				case PUSH_STORABLE:
					Storable storable = (Storable) msg.obj;
					Log.i(LTAG, "Push Storable: " + storable);
					upsertRow(storable);
					delivered = true;
                    break;
                case CLEANUP_PUBLISHED_CHAPTERS:
                    cleanupPublishedChapters();
                    delivered = true;
                    break;
				default:
					break;
				}

				if (reply != null) {
					try {
                        reply.replyTo = mMessenger;
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
				} else {
					Log.i(LTAG, "No transaction, will not end");
				}
			}
		}
	};


	public static final String TABLE_NAME = "storables";
	public static final String JSON_DATA_KEY = "data", TS_DATA_KEY = "ts",
			UUID_KEY = "uuid", TYPE_KEY = "type", DIRTY_KEY = "dirty";

	@Override
	public void onCreate(SQLiteDatabase db) {

		String createTable = "CREATE TABLE " + TABLE_NAME + " (" + UUID_KEY
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
		mHandler.getLooper().quitSafely();
		// TODO: stop sync thread

		super.close();
	}

	private static final String selectOne = "SELECT * FROM " + TABLE_NAME
			+ " WHERE " + UUID_KEY + "=\"%s\"";

	private static final String LTAG = StorageHandler.class.getSimpleName();

	private void upsertRow(Storable row) {

        Storable existing = getStorableByUUID(row.uuid);
        Boolean exists = (existing != null);
		Log.i(LTAG, "upsertRow : " + row + " --> exists : " + exists);

        if (exists){
            /**
             * Makes sure that write backs for clean versions of storables
             * don't overwrite new developments.
             *
             * Happens if a clip is added to active chapter in between sync receiving
             * the dirty active chapter and sync writing back the clean active chapter.
             *
             * The ts on the write-back of the clean chapter row will be BEFORE the ts
             * on the existing row, as ActiveChapters are always set to be AFTER the latest
             * clip
             */
            if (existing.ts.after(row.ts)){
                Log.w(LTAG,"ignoring upsert newer existing row with older incoming row:\nexisting: "+existing+"\nincoming: "+row);
                return;
            }
        }
		ContentValues cv = new ContentValues();
		cv.put(UUID_KEY, row.uuid);
		cv.put(TYPE_KEY, row.getClass().getName());
		cv.put(JSON_DATA_KEY, row.getJSONData());
		cv.put(TS_DATA_KEY, row.ts.getTime());
		cv.put(DIRTY_KEY, row.dirty ? 1 : 0);

		Log.i(LTAG, cv.toString());
		if (exists) {
			int rowsAffected = mDb.update(TABLE_NAME, cv, UUID_KEY + "=?",
					new String[] { row.uuid });

			Log.i(LTAG, "upsertRow update " + rowsAffected + " rows affected");
			if (BuildConfig.DEBUG && rowsAffected != 1) {
				throw new RuntimeException("upsertRow update affects "
						+ rowsAffected + " rows, should be 1");
			}
		} else {
			long rowId = mDb.insert(TABLE_NAME, null, cv);
			Log.i(LTAG, "upsertRow insert return rowid " + rowId);
			if (BuildConfig.DEBUG && rowId == -1) {
				throw new RuntimeException("upsertRow insert returns id = -1");
			}
		}

	}

    private int deleteStorable(Storable s){
        int affected = mDb.delete(TABLE_NAME,UUID_KEY+"=?",new String[]{s.uuid});
        if(affected != 1){
            Log.e(LTAG,"DELETE STORABLE "+s.uuid+" returns affected count of "+affected);
        }
        return affected;
    }

	private List<Chapter> getChapters(boolean dirty) {

        String selection = String.format("%s=? AND %s=?",TYPE_KEY,DIRTY_KEY);
		Cursor c = null;
		try {
			c = mDb.query(TABLE_NAME,null,selection,new String[]{Chapter.class.getName(), (dirty ? "1" : "0")},null,null,null);

			List<Chapter> chapters = new LinkedList<Chapter>();

			while (c.moveToNext()) {
                Chapter chapter = (Chapter) unmarshalStorable(c);
				chapters.add(chapter);
			}

			return chapters;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

    private void cleanupPublishedChapters(){
        int chapterCount = 0;
        int clipCount = 0;
        Log.i(LTAG,"Cleaning up chapters");
        for(Chapter chapter : getChapters(false)){
            if(!chapter.userpublished){
                continue;
            }
            if(chapter.doCleanup()){
                Log.i(LTAG,"Delete chapter "+chapter.uuid+" from database!");
                for(Clip clip : chapter.clips){
                    Log.i(LTAG,"Delete clip "+clip.uuid+" from database");
                    clipCount += deleteStorable(clip);
                }
                chapterCount += deleteStorable(chapter);
            }

        }
        Log.i(LTAG,"cleanup published chapters removed "+chapterCount+" chapters and "+clipCount+" clips");
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

	private Storable getNextStorable() {
		Cursor c = null;
		try {
			c = mDb.query(TABLE_NAME, null, DIRTY_KEY + "=1", null, null, null,
					TS_DATA_KEY + " ASC", "1");
			if (c.moveToFirst()) {
				return unmarshalStorable(c);
			}
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
    private Storable getStorableByUUID(String uuid){
        Cursor c = null;
        try {
            c = mDb.query(TABLE_NAME, null, UUID_KEY + "=?", new String[]{uuid}, null, null,null);
            if (c.moveToFirst()) {
                return unmarshalStorable(c);
            }
            return null;
        } finally {
            if (c != null) {
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
