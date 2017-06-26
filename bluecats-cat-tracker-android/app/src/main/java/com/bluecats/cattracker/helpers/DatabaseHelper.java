/* 
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.helpers;

import android.content.*;
import android.database.sqlite.*;
import android.util.*;

import com.bluecats.cattracker.*;

public class DatabaseHelper extends android.database.sqlite.SQLiteOpenHelper {
	private static final String TAG = "DatabaseHelper";

	public static final String TABLE_BEACON_PAYLOAD = "BeaconPayload";

	private static DatabaseHelper mSQLiteOpenHelper;
	private static boolean mPendingClearHelper = false;
	public static synchronized DatabaseHelper getInstance(Context context) {
		if (mPendingClearHelper) {
			mPendingClearHelper = false;
			if (mSQLiteOpenHelper != null) {
				try {
					mSQLiteOpenHelper.close();
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
				mSQLiteOpenHelper = null;
			}
		}
		if (mSQLiteOpenHelper == null) {
			mSQLiteOpenHelper = new DatabaseHelper(context);
		}
		return mSQLiteOpenHelper;
	}

	public static synchronized void clear() {
		mPendingClearHelper = true;
	}
	private DatabaseHelper(Context context) {
		super(context, String.format("%s.SQLite.db", context.getPackageName()), null, CatTracker.SQLITE_DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createDatabase(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		upgradeDatabase(db);
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		upgradeDatabase(db);
	}

	private void createDatabase(SQLiteDatabase db) {
		Log.d(TAG, "createDatabase");

		db.beginTransaction();
		try {
			createBeaconPayloadTable(db);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private void upgradeDatabase(SQLiteDatabase db) {
		Log.d(TAG, "upgradeDatabase");

		db.beginTransaction();
		try {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_BEACON_PAYLOAD + ";");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		createDatabase(db);
	}

	private void createBeaconPayloadTable(SQLiteDatabase db) {
		String beaconTable = "CREATE TABLE " + TABLE_BEACON_PAYLOAD + " ( " +
				"beaconPayloadID TEXT, " +
				"beaconIdentifier TEXT, " +
				"timestamp INTEGER, " +
				"beaconPayload TEXT" +
				");";
		db.execSQL(beaconTable);
	}
}
