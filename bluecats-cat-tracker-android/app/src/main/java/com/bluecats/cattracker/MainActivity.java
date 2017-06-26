/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.*;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.Toolbar;
import android.util.*;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.*;
import android.widget.*;

import com.bluecats.cattracker.BeaconsListFragment.*;
import com.bluecats.cattracker.models.*;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnListFragmentInteractionListener {
    private static final String TAG = "MainActivity";

    private NavigationView mNavigationView;
    public NavigationView getNavigationView() {
        return mNavigationView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new BeaconsFragment())
                    .commit();
        }

        TextView version = (TextView)findViewById(R.id.tv_app_version);
        version.setText(String.format("CatTracker v%s %s", BuildConfig.VERSION_NAME, (CatTracker.DBG ? "DBG" : "")));
        setLastUpdatedAt();

        registerReceiver(didUploadBeaconPayloads, new IntentFilter(String.format("%s.%s", getPackageName(), CatTracker.ACTION_DID_UPLOAD_BEACON_PAYLOADS)));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(didUploadBeaconPayloads);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // check if portal is visible fragment and handle web back presses
            PortalFragment fragment = (PortalFragment)getSupportFragmentManager().findFragmentByTag(PortalFragment.class.getName());
            if (fragment != null && fragment.isVisible()) {
                WebView wv = fragment.getWebView();
                if (wv != null && wv.canGoBack()) {
                    wv.goBack();
                    return;
                }
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_cat_tracker) {
            switchFragment(BeaconsFragment.class.getName(), new BeaconsFragment());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onListFragmentInteraction(BeaconPayload item) {
        Fragment fragment = new PortalFragment();

        switchFragment(PortalFragment.class.getName(), fragment);
    }

    public void setTitle(String title, String subTitle) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayShowTitleEnabled(true);
        if (title != null) {
            actionBar.setTitle(title);
        }
        if (subTitle != null) {
            actionBar.setSubtitle(subTitle);
        } else {
            actionBar.setSubtitle(null);
        }
        setTitle(title);
    }

    public void setNavigationItemChecked(int id) {
        if (mNavigationView != null) {
            mNavigationView.setCheckedItem(id);
        }
    }

    private void switchFragment(String tag, Fragment newFragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            fragment = newFragment;
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        }

        fragmentTransaction.replace(R.id.fragment_container, fragment, fragment.getClass().getName()).commit();
    }

    private void setLastUpdatedAt() {
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        if (sp.contains(CatTracker.KEY_LAST_UPDATED_AT)) {
            TextView user = (TextView)mNavigationView.getHeaderView(0).findViewById(R.id.tv_last_updated);

            Long lastUpdateAt = sp.getLong(CatTracker.KEY_LAST_UPDATED_AT, 0);
            user.setText(Utils.getLastUpdatedAt(lastUpdateAt));
        }
    }

    private BroadcastReceiver didUploadBeaconPayloads = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CatTracker.DBG) Log.d(TAG, "didUploadBeaconPayloads");

            final Activity activity = MainActivity.this;
            if (activity == null) {
                return;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity == null) {
                        return;
                    }

                    setLastUpdatedAt();
                }
            });
        }
    };
}
