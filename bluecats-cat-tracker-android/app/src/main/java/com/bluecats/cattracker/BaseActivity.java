/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker;

import android.content.*;
import android.os.*;
import android.support.v7.app.*;
import com.bluecats.cattracker.services.*;

public class BaseActivity extends AppCompatActivity {
    protected ApplicationPermissions mPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissions = new ApplicationPermissions(BaseActivity.this);
        mPermissions.verifyPermissions();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (mPermissions != null) {
                mPermissions.verifyPermissions();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (mPermissions != null) {
            mPermissions.onRequestPermissionResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        BlueCatsSDKInterfaceService.didEnterForeground();

        super.onResume();
    }

    @Override
    protected void onPause() {
        BlueCatsSDKInterfaceService.didEnterBackground();

        super.onPause();
    }
}
