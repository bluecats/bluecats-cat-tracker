/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.managers;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.util.*;

import com.bluecats.cattracker.*;
import com.bluecats.cattracker.helpers.*;
import com.bluecats.cattracker.models.*;

import java.util.*;

public class LocalStorageManager {
    private static final String TAG = "LocalStorageManager";

    private static Context mContext;

    private static class Holder {
        static final LocalStorageManager INSTANCE = new LocalStorageManager();
    }

    public static LocalStorageManager getInstance(Context context) {
        mContext = context;

        return LocalStorageManager.Holder.INSTANCE;
    }

    public static String[] mBeaconPayloadKeys = new String[] {
            "beaconPayloadID",
            "beaconIdentifier",
            "timestamp",
            "beaconPayload"
    };

    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase getReadableDatabase() {
        if (mDatabaseHelper == null) {
            mDatabaseHelper = DatabaseHelper.getInstance(mContext);
        }
        if (mDatabaseHelper != null) {
            try {
                return mDatabaseHelper.getReadableDatabase();
            } catch (SQLiteDatabaseLockedException e) {
                mDatabaseHelper.close();
                mDatabaseHelper.clear();
                return null;
            }
        }
        return null;
    }
    private SQLiteDatabase getWritableDatabase() {
        if (mDatabaseHelper == null) {
            mDatabaseHelper = DatabaseHelper.getInstance(mContext);
        }
        if (mDatabaseHelper != null) {
            try {
                return mDatabaseHelper.getWritableDatabase();
            } catch (SQLiteDatabaseLockedException e) {
                mDatabaseHelper.close();
                mDatabaseHelper.clear();
                mDatabaseHelper = DatabaseHelper.getInstance(mContext);
                return mDatabaseHelper.getWritableDatabase();
            }
        }
        return null;
    }

    public void storeBeaconPayloadForBeaconIdentifier(String beaconIdentifier, long timestamp, String beaconPayload) {
        SQLiteDatabase db = getWritableDatabase();
        if (db == null) {
            return;
        }

        db.beginTransaction();
        try {
            insertOrUpdateBeaconPayloadForBeaconIdentifier(beaconIdentifier, timestamp, beaconPayload, db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<BeaconPayload> getBeaconPayloads() {
        List<BeaconPayload> beaconPayloads = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        if (db == null) {
            return beaconPayloads;
        }

        db.beginTransaction();
        try {
            Cursor cursor = db.query(DatabaseHelper.TABLE_BEACON_PAYLOAD, mBeaconPayloadKeys, null, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    BeaconPayload beaconPayload = new BeaconPayload();
                    beaconPayload.setBeaconPayloadID(cursor.getString(0));
                    beaconPayload.setBeaconIdentifier(cursor.getString(1));
                    beaconPayload.setTimestamp(cursor.getLong(2));
                    beaconPayload.setBeaconPayload(cursor.getString(3));
                    beaconPayloads.add(beaconPayload);

                    cursor.moveToNext();
                }
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return beaconPayloads;
    }

    public void deleteBeaconPayloadByBeaconPayloadID(String beaconPayloadID) {
        SQLiteDatabase db = getWritableDatabase();
        if (db == null) {
            return;
        }

        db.beginTransaction();
        try {
            db.delete(DatabaseHelper.TABLE_BEACON_PAYLOAD, " beaconPayloadID = ?", new String[] { beaconPayloadID });
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertOrUpdateBeaconPayloadForBeaconIdentifier(String beaconIdentifier, long timestamp, String beaconPayload, SQLiteDatabase db) {
        List<BeaconPayload> beaconPayloads = getBeaconPayloadsForBeaconIdentifier(beaconIdentifier, db);
        if (beaconPayloads.size() == 0) {
            // insert new beacon payload
            ContentValues values = new ContentValues();
            values.put(mBeaconPayloadKeys[0], UUID.randomUUID().toString());
            values.put(mBeaconPayloadKeys[1], beaconIdentifier);
            values.put(mBeaconPayloadKeys[2], timestamp);
            values.put(mBeaconPayloadKeys[3], beaconPayload);

            db.insert(DatabaseHelper.TABLE_BEACON_PAYLOAD, null, values);
            if (CatTracker.DBG) Log.d(TAG, "insert BeaconPayload for " + beaconIdentifier);
        } else {
            // update existing beacon payload
            ContentValues values = new ContentValues();
            values.put(mBeaconPayloadKeys[2], timestamp);
            values.put(mBeaconPayloadKeys[3], beaconPayload);

            db.update(DatabaseHelper.TABLE_BEACON_PAYLOAD, values, "beaconIdentifier = ?", new String[] { beaconIdentifier });
            if (CatTracker.DBG) Log.d(TAG, "update BeaconPayload for " + beaconIdentifier);
        }
    }

    private List<BeaconPayload> getBeaconPayloadsForBeaconIdentifier(String beaconIdentifier, SQLiteDatabase db) {
        List<BeaconPayload> beaconPayloads = new ArrayList<>();

        Cursor cursor = db.query(DatabaseHelper.TABLE_BEACON_PAYLOAD, mBeaconPayloadKeys, "beaconIdentifier = ?", new String[] { beaconIdentifier }, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BeaconPayload beaconPayload = new BeaconPayload();
                beaconPayload.setBeaconPayloadID(cursor.getString(0));
                beaconPayload.setBeaconIdentifier(cursor.getString(1));
                beaconPayload.setTimestamp(cursor.getLong(2));
                beaconPayload.setBeaconPayload(cursor.getString(3));
                beaconPayloads.add(beaconPayload);

                cursor.moveToNext();
            }
            cursor.close();
        }

        return beaconPayloads;
    }
}
