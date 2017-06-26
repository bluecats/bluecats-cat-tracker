/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.models;

import android.location.*;

import com.bluecats.sdk.*;

import java.util.*;

public class BeaconSighting {
    private BCBeacon mBeacon;
    public BCBeacon getBeacon() {
        return mBeacon;
    }

    private int mRSSI;
    public int getRSSI() {
        return mRSSI;
    }

    private Date mTimestamp;
    public Date getTimestamp() {
        return mTimestamp;
    }

    private Location mLocation;
    public Location getLocation() {
        return mLocation;
    }

    public BeaconSighting(BCBeacon beacon, int rssi, Date timestamp, Location location) {
        mBeacon = beacon;
        mRSSI = rssi;
        mTimestamp = timestamp;
        mLocation = location;
    }
}
