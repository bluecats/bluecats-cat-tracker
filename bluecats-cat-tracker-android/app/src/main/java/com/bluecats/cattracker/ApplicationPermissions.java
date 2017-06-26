/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker;

import android.Manifest.*;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.DialogInterface.*;
import android.content.pm.*;
import android.os.*;
import android.os.Build.*;
import android.provider.*;
import android.support.v4.app.*;
import android.support.v4.content.*;

import com.bluecats.sdk.*;

public class ApplicationPermissions {
    public static final int REQUEST_CODE_ENABLE_BLUETOOTH = 1001;
    public static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1002;
    public static final int REQUEST_CODE_CAMERA_PERMISSIONS = 1003;
    public static final int REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATIONS = 1004;

    private Activity mActivity;
    private PowerManager mPowerManager;

    public ApplicationPermissions(Activity activity) {
        mActivity = activity;
        mPowerManager = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
    }

    public void verifyPermissions() {
        if (!BlueCatsSDK.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(intent, REQUEST_CODE_ENABLE_BLUETOOTH);
        } else if (!locationPermissionsEnabled()) {
            ActivityCompat.requestPermissions(mActivity, new String[] { permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSIONS);
        } else if (!BlueCatsSDK.isLocationAuthorized(mActivity)) {
            showLocationServicesAlert();
        } else if (ContextCompat.checkSelfPermission(mActivity, permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[] { permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSIONS);
        } else if (ContextCompat.checkSelfPermission(mActivity, permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[] { permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}, REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATIONS);
        } else if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (!mPowerManager.isIgnoringBatteryOptimizations(mActivity.getPackageName())) {
                //showIgnoreBatteryOptimizationsAlert();
            }
        }
    }

    private boolean locationPermissionsEnabled() {
        return ContextCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void showLocationServicesAlert() {
        new AlertDialog.Builder(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setMessage("This app requires Location Services to run. Would you like to enable Location Services now?")
                .setPositiveButton("Yes", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mActivity.startActivity(intent);
                    }
                })
                .setNegativeButton("No", cancelClickListener)
                .create()
                .show();
    }

    private void showLocationPermissionsAlert() {
        new AlertDialog.Builder(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setMessage("This app requires location permissions to run. Would you like to enable location permissions now?")
                .setPositiveButton("Yes", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("No", cancelClickListener)
                .create()
                .show();
    }

    private void showIgnoreBatteryOptimizationsAlert() {
        new AlertDialog.Builder(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setMessage("This app would like to ignore battery optimizations. Would you like to allow this?")
                .setPositiveButton("Yes", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        mActivity.startActivity(intent);
                    }
                })
                .setNegativeButton("No", cancelClickListener)
                .create()
                .show();
    }

    private OnClickListener cancelClickListener =  new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    };

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults){
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verifyPermissions();
            }
        }
    }
}
