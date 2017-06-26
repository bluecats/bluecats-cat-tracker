/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.helpers;

import android.content.*;
import android.location.*;
import android.support.v4.content.*;
import android.util.*;

import com.bluecats.cattracker.*;
import com.bluecats.cattracker.managers.*;
import com.bluecats.cattracker.providers.*;
import com.bluecats.cattracker.services.*;

import org.json.*;

import java.util.*;

public class SightingsUploadHelper extends WakefulBroadcastReceiver {
    private static final String TAG = "SightingsUploadHelper";

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CatTracker.DBG) Log.d(TAG, "onReceive");

        mContext = context;

        // if valid location create payload for user report payload
        Location location = BlueCatsSDKInterfaceService.getLastKnownLocation();
        if (location != null) {
            String payload = getJSONPayload(location);

            LocalStorageManager.getInstance(mContext).storeBeaconPayloadForBeaconIdentifier(UUID.randomUUID().toString(), System.currentTimeMillis(), payload);
        }

        Intent beaconPayloadIntent = new Intent(mContext, BeaconPayloadService.class);
        startWakefulService(mContext, beaconPayloadIntent);

        if (CatTracker.DBG) Log.d(TAG, "startBeaconPayloadService");
    }

    private String getJSONPayload(Location lastKnownLocation) {
        JSONObject payload = new JSONObject();

        try {
            IdentityProvider identityProvider = new IdentityProvider();

			/*
			 * this payload can be whatever data is required
			 */
            JSONObject client = new JSONObject();
            client.put("installationID", identityProvider.getSessionIdentifier());
            client.put("idfa", ""); // apple id
            client.put("gaid", identityProvider.getDeviceIdentifier(mContext)); // google ad id
            client.put("mycustomid", identityProvider.getSessionIdentifier());
            payload.put("client", client);

            JSONObject reportedLocation = new JSONObject();
            reportedLocation.put("latitude", lastKnownLocation.getLatitude());
            reportedLocation.put("longitude", lastKnownLocation.getLongitude());
            reportedLocation.put("locatedAt", lastKnownLocation.getTime());
            payload.put("reportedLocation", reportedLocation);

            JSONObject events = new JSONObject();
            events.put("event", "USER_REPORT"); // event is a custom string, can be anything
            payload.put("events", events);
        } catch (JSONException jsonException) {
            Log.e(TAG, jsonException.getMessage());
        }

        return payload.toString();
    }
}
