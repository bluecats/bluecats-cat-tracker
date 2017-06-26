/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.services;

import java.util.List;

import com.bluecats.sdk.BCBeacon;

public interface IBlueCatsSDKInterfaceServiceCallback {
	void onDidExitBeacons(List<BCBeacon> beacons);
	void onDidRangeBeacons(List<BCBeacon> beacons);
}