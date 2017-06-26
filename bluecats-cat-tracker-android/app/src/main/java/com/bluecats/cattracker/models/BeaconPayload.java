/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.models;

import android.os.*;

public class BeaconPayload implements Parcelable {
    private String mBeaconPayloadID;

    public String getBeaconPayloadID() {
        return mBeaconPayloadID;
    }
    public void setBeaconPayloadID(String value) {
        mBeaconPayloadID = value;
    }

    private String mBeaconIdentifier;
    public String getBeaconIdentifier() {
        return mBeaconIdentifier;
    }
    public void setBeaconIdentifier(String value) {
        mBeaconIdentifier = value;
    }

    private Long mTimestamp;
    public Long getTimestamp() {
        return mTimestamp;
    }
    public void setTimestamp(Long value) {
        mTimestamp = value;
    }

    private Integer mMajor;
    public Integer getMajor() {
        return mMajor;
    }
    public void setMajor(Integer value) {
        mMajor = value;
    }

    private String mBeaconPayload;
    public String getBeaconPayload() {
        return mBeaconPayload;
    }
    public void setBeaconPayload(String value) {
        mBeaconPayload = value;
    }

    public BeaconPayload() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BeaconPayload> CREATOR = new Creator<BeaconPayload>() {
        @Override
        public BeaconPayload createFromParcel(Parcel in) {
            return new BeaconPayload(in);
        }

        @Override
        public BeaconPayload[] newArray(int size) {
            return new BeaconPayload[size];
        }
    };

    protected BeaconPayload(Parcel in) {
        mBeaconPayloadID = in.readString();
        mBeaconIdentifier = in.readString();
        mTimestamp = in.readLong();
        mBeaconPayload = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mBeaconPayloadID);
        dest.writeString(mBeaconIdentifier);
        dest.writeLong(mTimestamp);
        dest.writeString(mBeaconPayload);
    }
}
