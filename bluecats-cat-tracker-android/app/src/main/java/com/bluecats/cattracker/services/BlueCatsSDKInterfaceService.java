/* 
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.services;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.bluecats.cattracker.helpers.*;
import com.bluecats.cattracker.managers.*;
import com.bluecats.sdk.*;
import com.bluecats.cattracker.*;
import com.bluecats.cattracker.models.*;
import com.bluecats.cattracker.providers.*;
import com.bluecats.sdk.BCBeacon.*;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.util.Log;

import org.json.*;

public class BlueCatsSDKInterfaceService extends Service {
	private static final String TAG = "SDKInterfaceService";

	private static WeakHashMap<String, IBlueCatsSDKInterfaceServiceCallback> mBlueCatsSDKServiceCallbacks;

	private static WeakHashMap<String, IBlueCatsSDKInterfaceServiceCallback> getBlueCatsSDKServiceCallbacks() {
		if (mBlueCatsSDKServiceCallbacks == null) {
			mBlueCatsSDKServiceCallbacks = new WeakHashMap<>();
		}
		synchronized (mBlueCatsSDKServiceCallbacks) {
			return mBlueCatsSDKServiceCallbacks;
		}
	}

	private static Context mServiceContext;
	private static Object mServiceContextLock = new Object();
	public static Context getServiceContext() {
		synchronized (mServiceContextLock) {
			return mServiceContext;
		}
	}

	private static BCBeaconManager mBeaconManager;
	private static LocationManager mLocationManager;
	private static IdentityProvider mIdentityProvider;
	private static Thread mAlarmThread;

	private static Set<String> mPayloadsInProgress;
	public static Set<String> getPayloadsInProgress() {
		return mPayloadsInProgress;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (CatTracker.DBG) Log.d(TAG, "onCreate");

		mServiceContext = BlueCatsSDKInterfaceService.this;

		mPayloadsInProgress = Collections.synchronizedSet(new HashSet<String>());

		mBeaconManager = new BCBeaconManager();
		mBeaconManager.registerCallback(mBeaconManagerCallback);

		mLocationManager = (LocationManager) getServiceContext().getSystemService(Context.LOCATION_SERVICE);

		/*
		 * The device's identifier is used to determine which device is reporting the beacon sightings
		 */
		mIdentityProvider = new IdentityProvider();
		mIdentityProvider.prepareAll(getServiceContext());

		/*
		 * use a thread to trigger a beacon payload.
		 */
		if (mAlarmThread == null) {
			mAlarmThread = new Thread(mAlarmThreadRunnable, "mAlarmThread");
			mAlarmThread.setPriority(Thread.MAX_PRIORITY);
			mAlarmThread.start();
		}
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);

		if (CatTracker.DBG) Log.d(TAG, "onStartCommand");

		String appToken = "";
		if (intent != null && intent.getStringExtra(BlueCatsSDK.EXTRA_APP_TOKEN) != null) {
			appToken = intent.getStringExtra(BlueCatsSDK.EXTRA_APP_TOKEN);
		}

		// add any options here
		Map<String, String> options = new HashMap<String, String>();
		BlueCatsSDK.setOptions(options);
		BlueCatsSDK.startPurringWithAppToken(getApplicationContext(), appToken);

		if (CatTracker.DBG) Log.d(TAG, "startPurringWithAppToken " + appToken);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (CatTracker.DBG) Log.d(TAG, "onDestroy");

		if (mBeaconManager != null) {
			mBeaconManager.unregisterCallback(mBeaconManagerCallback);
		}

		mLocationManager = null;

		super.onDestroy();
	}

	/**
	 * Register a callback for your activity to receive updates from any SDK events you have defined.
	 */
	public static void registerBlueCatsSDKServiceCallback(String className, IBlueCatsSDKInterfaceServiceCallback callback) {
		getBlueCatsSDKServiceCallbacks().put(className, callback);

		if (CatTracker.DBG) Log.d(TAG, "registerBlueCatsSDKServiceCallback");
	}

	/**
	 * Unregister your activity's callback when the activity is closed or destroyed.
	 */
	public static void unregisterBlueCatsSDKServiceCallback(String className) {
		getBlueCatsSDKServiceCallbacks().remove(className);

		if (CatTracker.DBG) Log.d(TAG, "unregisterBlueCatsSDKServiceCallback");
	}

	/**
	 * Let the SDK know when the app has entered the foreground to increase Beacon scanning rate.
	 */
	public static void didEnterForeground() {
		if (CatTracker.DBG) Log.d(TAG, "didEnterForeground");

		BlueCatsSDK.didEnterForeground();
	}

	/**
	 * Let the SDK know when the app has entered the foreground to decrease Beacon scanning rate.
	 */
	public static void didEnterBackground() {
		if (CatTracker.DBG) Log.d(TAG, "didEnterBackground");

		BlueCatsSDK.didEnterBackground();
	}

	public static Location getLastKnownLocation() {
		if (getServiceContext() == null) {
			return null; // if service has not started yet
		}
		if (mLocationManager == null) {
			return null; // if location manager has not been created yet
		}

		try {
			return mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
		} catch (SecurityException se) {
			Log.e(TAG, se.toString());
		}

		return null;
	}

	private String getJSONPayload(BeaconSighting beaconSighting) {
		JSONObject payload = new JSONObject();

		int batteryLevel = 0;
		if (beaconSighting.getBeacon().getLastKnownBatteryLevel() != null) {
			batteryLevel = beaconSighting.getBeacon().getLastKnownBatteryLevel();
		}

		try {
			/*
			 * this payload can be whatever data is required
			 */
			payload.put("teamID", beaconSighting.getBeacon().getTeamID());

			JSONObject client = new JSONObject();
			client.put("installationID", mIdentityProvider.getSessionIdentifier());
			client.put("idfa", ""); // apple id
			client.put("gaid", mIdentityProvider.getDeviceIdentifier(getServiceContext())); // google ad id
			client.put("mycustomid", mIdentityProvider.getSessionIdentifier());
			payload.put("client", client);

			JSONObject reportedLocation = new JSONObject();
			reportedLocation.put("latitude", beaconSighting.getLocation().getLatitude());
			reportedLocation.put("longitude", beaconSighting.getLocation().getLongitude());
			reportedLocation.put("locatedAt", beaconSighting.getLocation().getTime());
			payload.put("reportedLocation", reportedLocation);

			JSONObject events = new JSONObject();
			events.put("event", "ENTERED_BEACON"); // event is a custom string, can be anything
			events.put("detectedAt", beaconSighting.getTimestamp());
			events.put("rssi", beaconSighting.getRSSI());

			JSONObject beacon = new JSONObject();
			beacon.put("beaconIdentifier", beaconSighting.getBeacon().getSerialNumber());
			beacon.put("batteryLevel", batteryLevel);
			beacon.put("mycustomid", "any custom beacon property");

			events.put("beacon", beacon);
			payload.put("events", events);
		} catch (JSONException jsonException) {
			Log.e(TAG, jsonException.getMessage());
		}

		return payload.toString();
	}

	private final Runnable mAlarmThreadRunnable = new Runnable() {
		@Override
		public void run() {
			if (CatTracker.DBG) Log.d(TAG, "started mAlarmThreadRunnable");

			while (!Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(CatTracker.SIGHTING_UPLOAD_INTERVAL_IN_MILLISECONDS);

					if (CatTracker.DBG) Log.d(TAG, "sendBroadcast");

					Intent intent = new Intent(getServiceContext(), SightingsUploadHelper.class);
					sendBroadcast(intent);
				} catch (InterruptedException e) {
					Log.e(TAG, e.toString());
					if (CatTracker.DBG) Log.d(TAG, "mAlarmThreadRunnable interrupted");

					Thread.currentThread().interrupt();

					break;
				}
			}

			if (CatTracker.DBG) Log.d(TAG, "stopped mAlarmThreadRunnable");
		}
	};

	BCBeaconManagerCallback mBeaconManagerCallback = new BCBeaconManagerCallback() {
		@Override
		public void didEnterSite(BCSite site) {
			super.didEnterSite(site);
			if (CatTracker.DBG) Log.d(TAG, "didEnterSite "+site.getName());
		}

		@Override
		public void didExitSite(BCSite site) {
			super.didExitSite(site);
			if (CatTracker.DBG) Log.d(TAG, "didExitSite "+site.getName());
		}

		@Override
		public void didDetermineState(BCSite.BCSiteState state, BCSite forSite) {
			super.didDetermineState(state, forSite);
			if (CatTracker.DBG) Log.d(TAG, "didDetermineState");
		}

		@Override
		public void didEnterBeacons(List<BCBeacon> beacons) {
			super.didEnterBeacons(beacons);
			if (CatTracker.DBG) Log.d(TAG, "didEnterBeacons");
		}

		@Override
		public void didExitBeacons(List<BCBeacon> beacons) {
			super.didExitBeacons(beacons);
			if (CatTracker.DBG) Log.d(TAG, "didExitBeacons");
		}

		@Override
		public void didDetermineState(BCBeacon.BCBeaconState state, BCBeacon forBeacon) {
			super.didDetermineState(state, forBeacon);
			if (CatTracker.DBG) Log.d(TAG, "didDetermineState");
		}

		@Override
		public void didRangeBeacons(List<BCBeacon> beacons) {
			super.didRangeBeacons(beacons);
			if (CatTracker.DBG) Log.d(TAG, "didRangeBeacons");

			for (BCBeacon beacon : beacons) {
				String identifier = beacon.getSerialNumber(); // can use any relevant identifier

				// create a beacon sighting if can get a valid location
				Location location = getLastKnownLocation();
				if (location != null && !Utils.isNullOrEmpty(identifier)) {
					BeaconSighting beaconSighting = new BeaconSighting(beacon, beacon.getRSSI(), new Date(), location);

					String payload = getJSONPayload(beaconSighting);

					LocalStorageManager.getInstance(getServiceContext()).storeBeaconPayloadForBeaconIdentifier(
							identifier,
							beaconSighting.getTimestamp().getTime(),
							payload);

					// broadcast updated local storage for list fragments
					Intent updatedLocalStorageIntent = new Intent(String.format("%s.%s", BlueCatsSDKService.getServiceContext().getPackageName(), CatTracker.ACTION_DID_UPDATE_LOCAL_STORAGE));
					sendBroadcast(updatedLocalStorageIntent);
				}
			}
		}

		@Override
		public void didRangeBlueCatsBeacons(List<BCBeacon> beacons) {
			super.didRangeBlueCatsBeacons(beacons);
			if (CatTracker.DBG) Log.d(TAG, "didRangeBlueCatsBeacons");
		}

		@Override
		public void didRangeNewbornBeacons(List<BCBeacon> newBornBeacons) {
			super.didRangeNewbornBeacons(newBornBeacons);
			if (CatTracker.DBG) Log.d(TAG, "didRangeNewbornBeacons");
		}

		@Override
		public void didRangeIBeacons(List<BCBeacon> iBeacons) {
			super.didRangeIBeacons(iBeacons);
			if (CatTracker.DBG) Log.d(TAG, "didRangeIBeacons");
		}

		@Override
		public void didRangeEddystoneBeacons(List<BCBeacon> eddystoneBeacons) {
			super.didRangeEddystoneBeacons(eddystoneBeacons);
			if (CatTracker.DBG) Log.d(TAG, "didRangeEddystoneBeacons");
		}

		@Override
		public void didDiscoverEddystoneURL(URL eddystoneUrl) {
			super.didDiscoverEddystoneURL(eddystoneUrl);
			if (CatTracker.DBG) Log.d(TAG, "didDiscoverEddystoneURL");
		}
	};

	public class LocalBinder extends Binder {
		public BlueCatsSDKInterfaceService getService() {
			return BlueCatsSDKInterfaceService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBlueCatsSDKServiceBinder;
	}

	private final IBinder mBlueCatsSDKServiceBinder = new LocalBinder();
}
