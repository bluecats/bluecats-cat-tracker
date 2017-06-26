/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences.*;
import android.content.pm.*;
import android.content.pm.PackageManager.*;
import android.os.*;
import android.support.multidex.*;
import android.util.*;

import com.bluecats.sdk.*;
import com.bluecats.cattracker.services.*;

public class CatTracker extends MultiDexApplication {
    private static final String TAG = "CatTracker";

    public static final Boolean DBG = true;

    public static final String KEY_APP_TOKEN = "com.bluecats.sdk.AppToken";

    public static final String KEY_LAST_UPDATED_AT = "KEY_LAST_UPDATED_AT";
    public static final String KEY_INSTALLATION_ID = "KEY_INSTALLATION_ID";

    public static final int SIGHTINGS_UPLOAD_REQUEST_CODE = 2001;
    public static final String SERVER_ENDPOINT_URL = "http://...";
    public static final int SIGHTINGS_SERVER_TIME_OUT_IN_MILLISECONDS = 60 * 1000; // 60 seconds

    public static final String ACTION_DID_UPDATE_LOCAL_STORAGE = "ACTION_DID_UPDATE_LOCAL_STORAGE";
    public static final String ACTION_DID_UPLOAD_BEACON_PAYLOADS = "ACTION_DID_UPLOAD_BEACON_PAYLOADS";

    public static final String EXTRA_BEACON_IDENTIFIER = "EXTRA_BEACON_IDENTIFIER";

    public static final int SIGHTING_UPLOAD_INTERVAL_IN_MILLISECONDS = 60 * 1000; // 60 seconds
    public static final int SQLITE_DATABASE_VERSION = 2;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            String appToken = bundle.getString(KEY_APP_TOKEN);

            Intent intent = new Intent(CatTracker.this, BlueCatsSDKInterfaceService.class);
            intent.putExtra(BlueCatsSDK.EXTRA_APP_TOKEN, appToken);
            startService(intent);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }
    }

    private static void redirect(Activity fromActivity, Class toActivityClass) {
        Intent intent = new Intent(fromActivity, toActivityClass);
        fromActivity.startActivity(intent);

        fromActivity.finish();
    }
}
