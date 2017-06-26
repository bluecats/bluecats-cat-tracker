/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.providers;

import android.content.*;
import android.content.SharedPreferences.*;
import android.os.*;
import android.provider.Settings.*;
import android.util.*;

import com.bluecats.cattracker.*;

import java.lang.reflect.*;
import java.util.*;

public class IdentityProvider {
    protected static final String TAG = "IdentityProvider";

    private static final String IDENTIFIER_DEVICE_UUID = "IDENTIFIER_DEVICE_UUID";
    private static final String IDENTIFIER_USER_DEFINED_DEVICE_UUID = "IDENTIFIER_USER_DEFINED_DEVICE_UUID";

    private Boolean mIsGoogleAdvertisingIdAvailable = Boolean.FALSE;
    private String mSessionIdentifier;
    private String mDeviceIdentifier = null;
    private Object mDeviceIdentifierLock = new Object();
    private String mInstallationId = null;
    private Object mLock = new Object();

    public IdentityProvider() {
        mSessionIdentifier = "";

        mIsGoogleAdvertisingIdAvailable = checkGoogleAdvertisingIdAvailability();
        Log.d(TAG, "checkGooglePlayService " + mIsGoogleAdvertisingIdAvailable);
    }

    public void prepareAll(Context context) {
        getInstallationId(context);
        getDeviceIdentifier(context);
        preparingGoogleAdvertisingId(context);
    }

    public String getDeviceIdentifier(Context context) {
        synchronized (mDeviceIdentifierLock) {
            if (Utils.isNullOrEmpty(mDeviceIdentifier)) {
                if (context != null) {
                    SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
                    String userDefinedId = sp.getString(IDENTIFIER_USER_DEFINED_DEVICE_UUID, null);
                    if (Utils.isNullOrEmpty(userDefinedId)) {
                        String value = sp.getString(IDENTIFIER_DEVICE_UUID, null);
                        if (Utils.isNullOrEmpty(value)) {
                            ContentResolver resolver = context.getContentResolver();
                            String aid = Secure.getString(resolver, Secure.ANDROID_ID);
                            if (Utils.isNullOrEmpty(aid)) {
                                mDeviceIdentifier = getInstallationId(context);
                            } else {
                                mDeviceIdentifier = "aid:" + aid; //android id
                            }
                            Editor editor = sp.edit();
                            editor.putString(IDENTIFIER_DEVICE_UUID, mDeviceIdentifier);
                            editor.commit();
                        } else {
                            mDeviceIdentifier = value;
                        }
                    } else {
                        mDeviceIdentifier = userDefinedId;
                    }
                }
            }
            return mDeviceIdentifier;
        }
    }

    public String getSessionIdentifier() {
        synchronized (mSessionIdentifier) {
            if (Utils.isNullOrEmpty(mSessionIdentifier)) {
                mSessionIdentifier = UUID.randomUUID().toString();
                mSessionIdentifier = new StringBuilder().append("bc:").append(mSessionIdentifier).toString();
            }
            return mSessionIdentifier;
        }
    }

    private String getInstallationId(Context ctx) {
        synchronized (mLock) {
            if (Utils.isNullOrEmpty(mInstallationId)) {
                if (ctx != null) {
                    SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
                    mInstallationId = sp.getString(CatTracker.KEY_INSTALLATION_ID, null);
                    if (Utils.isNullOrEmpty(mInstallationId)) {
                        Editor edit = sp.edit();
                        mInstallationId = "bc:"+UUID.randomUUID().toString();
                        if (CatTracker.DBG) Log.d(TAG, "a new installation/advertising id has been generated: " + mInstallationId);
                        edit.putString(CatTracker.KEY_INSTALLATION_ID, mInstallationId);
                        edit.commit();
                    }
                } else {
                    if (CatTracker.DBG) Log.d(TAG, "service is null, installation/advertising id returns null as well.");
                }
            }
            return mInstallationId;
        }
    }

    private boolean checkGoogleAdvertisingIdAvailability() {
        try {
            Class client = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private void preparingGoogleAdvertisingId(final Context context) {
        if (mIsGoogleAdvertisingIdAvailable.booleanValue() == false) {
            return;
        }
        if (context != null) {
            AsyncTask<Void, Void, String> getAdvertisingIdTask = new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... params) {
                    try {
                        Class client = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                        Method getAidInfo = client.getDeclaredMethod("getAdvertisingIdInfo", Context.class);
                        Object infoObj = getAidInfo.invoke(client, context);

                        if (infoObj == null) {
                            return null;
                        }

                        Class infoclz = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info");
                        Method limitTracking = infoclz.getDeclaredMethod("isLimitAdTrackingEnabled");
                        Boolean enabled = (Boolean)limitTracking.invoke(infoObj);
                        if (enabled != null && !enabled.booleanValue()) {
                            Method getId = infoclz.getDeclaredMethod("getId");
                            String gaid = (String) getId.invoke(infoObj);
                            if (Utils.isNullOrEmpty(gaid)) {
                                return null;
                            }
                            gaid = "gaid:" + gaid;
                            synchronized (mDeviceIdentifierLock) {
                                if (context != null) {
                                    SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
                                    Editor editor = sp.edit();
                                    editor.putString(IDENTIFIER_DEVICE_UUID, gaid);
                                    editor.commit();
                                    String userDefinedId = sp.getString(IDENTIFIER_USER_DEFINED_DEVICE_UUID, null);
                                    if (Utils.isNullOrEmpty(userDefinedId)) {
                                        mDeviceIdentifier = gaid;
                                    } else {
                                        mDeviceIdentifier = userDefinedId;
                                    }
                                }
                            }
                            return gaid;
                        }
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG, "ClassNotFoundException");
                    } catch (NoSuchMethodException e) {
                        Log.e(TAG, "NoSuchMethodException");
                    } catch (InvocationTargetException e) {
                        Log.e(TAG, "InvocationTargetException");
                    } catch (IllegalAccessException e) {
                        Log.e(TAG, "IllegalAccessException");
                    } catch (NullPointerException e) {
                        Log.e(TAG, "NullPointerException");
                    }
                    return null;
                }

            };
            getAdvertisingIdTask.execute();
        }
    }
}
