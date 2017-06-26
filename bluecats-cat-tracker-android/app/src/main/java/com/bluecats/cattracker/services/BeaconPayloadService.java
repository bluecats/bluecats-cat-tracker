/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.services;

import android.app.*;
import android.content.*;
import android.util.*;

import com.bluecats.sdk.*;
import com.bluecats.cattracker.*;
import com.bluecats.cattracker.helpers.*;
import com.bluecats.cattracker.managers.*;
import com.bluecats.cattracker.models.*;

import org.json.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class BeaconPayloadService extends IntentService {
    private static final String TAG = "BeaconPayloadService";

    private Context mContext;
    private int mBaconPayloadsUploaded;

    public BeaconPayloadService() {
        super(TAG);
    }
    @Override
    public void onCreate() {
        if (CatTracker.DBG) Log.d(TAG, "onCreate");

        super.onCreate();
    }
    @Override
    public void onStart(Intent intent, int startId) {
        if (CatTracker.DBG) Log.d(TAG, "onStart");

        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (CatTracker.DBG) Log.d(TAG, "onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = getApplicationContext();
        if (mContext == null) {
            return;
        }

        if (CatTracker.DBG) Log.d(TAG, "BeaconPayloadService onHandleIntent");

        // loop through entries in db to be uploaded
        List<BeaconPayload> beaconPayloads = LocalStorageManager.getInstance(mContext).getBeaconPayloads();
        if (beaconPayloads == null) {
            if (CatTracker.DBG) Log.d(TAG, "no beaconPayloads to process");
            return;
        }

        if (CatTracker.DBG) Log.d(TAG, String.format("%d beaconPayloads to process", beaconPayloads.size()));

        mBaconPayloadsUploaded = 0;
        for (Iterator<BeaconPayload> i = beaconPayloads.iterator(); i.hasNext();) {
            // check for network reachability
            if (!BlueCatsSDK.isNetworkReachable(mContext)) {
                if (CatTracker.DBG) Log.d(TAG, "network unreachable");

                continue;
            }

            BeaconPayload beaconPayload = i.next();

            // check if this payload is still in progress from the previous upload
            if (BlueCatsSDKInterfaceService.getPayloadsInProgress().contains(beaconPayload.getBeaconPayloadID())) {
                if (CatTracker.DBG) Log.d(TAG, String.format("Payload In Progress for %s: %s", beaconPayload.getBeaconIdentifier(), beaconPayload.getBeaconPayloadID()));

                continue;
            }

            HttpURLConnection connection = null;
            try {
                if (CatTracker.DBG) Log.d(TAG, "sightingsEndpoint: " + CatTracker.SERVER_ENDPOINT_URL);

                connection = (HttpURLConnection)(new URL(CatTracker.SERVER_ENDPOINT_URL).openConnection());
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CatTracker.SIGHTINGS_SERVER_TIME_OUT_IN_MILLISECONDS);
                connection.setReadTimeout(CatTracker.SIGHTINGS_SERVER_TIME_OUT_IN_MILLISECONDS);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");

                String postData = beaconPayload.getBeaconPayload();
                if (!Utils.isNullOrEmpty(postData)) {
                    // write payload
                    connection.setDoOutput(true);

                    connection.setFixedLengthStreamingMode(postData.getBytes().length);
                    DataOutputStream output = new DataOutputStream(connection.getOutputStream());
                    output.writeBytes(postData);
                    output.flush();
                    output.close();
                }

                // check response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readInputStream(connection.getInputStream());
                    if (!Utils.isNullOrEmpty(response)) {
                        JSONObject result = (JSONObject)new JSONTokener(response).nextValue();
                        JSONObject messageObject = result.getJSONObject("d");

                        String message = messageObject.getString("Message");
                        if (CatTracker.DBG) Log.d(TAG, message);

                        if (message.equalsIgnoreCase("Record saved!")) {
                            if (CatTracker.DBG) Log.d(TAG, "POSTED Beacon Payload : " + beaconPayload.getMajor() + " " + beaconPayload.getBeaconPayload());

                            // set the payload id for deleting if successful
                            onDidUploadBeaconPayload(beaconPayload.getBeaconPayloadID());
                        } else {
                            onBeaconPayloadFailed(beaconPayload.getBeaconPayloadID());
                        }
                    } else {
                        if (CatTracker.DBG) Log.d(TAG, "response is empty");

                        onBeaconPayloadFailed(beaconPayload.getBeaconPayloadID());
                    }
                } else {
                    if (CatTracker.DBG) Log.d(TAG, String.format("Error %s: %s", responseCode, connection.getResponseMessage()));

                    onBeaconPayloadFailed(beaconPayload.getBeaconPayloadID());
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());

                onBeaconPayloadFailed(beaconPayload.getBeaconPayloadID());
            } catch (JSONException e) {
                Log.e(TAG, e.toString());

                onBeaconPayloadFailed(beaconPayload.getBeaconPayloadID());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            // remove the payload upload
            BlueCatsSDKInterfaceService.getPayloadsInProgress().remove(beaconPayload.getBeaconPayloadID());
        }

        // broadcast updated local storage for list fragments
        Intent updatedLocalStorageIntent = new Intent(String.format("%s.%s", BlueCatsSDKService.getServiceContext().getPackageName(), CatTracker.ACTION_DID_UPDATE_LOCAL_STORAGE));
        sendBroadcast(updatedLocalStorageIntent);

        // if uploaded at least one beacon payload broadcast finished upload payloads intent
        if (mBaconPayloadsUploaded > 0) {
            // save the last updated date
            SharedPreferences sp = mContext.getSharedPreferences(mContext.getPackageName(), MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong(CatTracker.KEY_LAST_UPDATED_AT, new Date().getTime());
            editor.commit();

            Intent uploadedBeaconPayloadsIntent = new Intent(String.format("%s.%s", BlueCatsSDKService.getServiceContext().getPackageName(), CatTracker.ACTION_DID_UPLOAD_BEACON_PAYLOADS));
            sendBroadcast(uploadedBeaconPayloadsIntent);
        }

        // release wakelock for service
        SightingsUploadHelper.completeWakefulIntent(intent);
    }

    @Override
    public void onDestroy() {
        if (CatTracker.DBG) Log.d(TAG, "onDestroy");

        super.onDestroy();
    }

    private void onDidUploadBeaconPayload(String beaconPayloadID) {
        if (Utils.isNullOrEmpty(beaconPayloadID)) {
            return;
        }

        // remove successful payload from local storage
        LocalStorageManager.getInstance(mContext).deleteBeaconPayloadByBeaconPayloadID(beaconPayloadID);

        mBaconPayloadsUploaded++;
    }

    private void onBeaconPayloadFailed(String beaconPayloadID) {
        if (Utils.isNullOrEmpty(beaconPayloadID)) {
            return;
        }

        // handle failed payload upload
        LocalStorageManager.getInstance(mContext).deleteBeaconPayloadByBeaconPayloadID(beaconPayloadID);
    }

    private String readInputStream(InputStream inputStream) {
        StringBuffer response = new StringBuffer();

        BufferedReader reader = null;
        String inputLine = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        return response.toString();
    }
}
