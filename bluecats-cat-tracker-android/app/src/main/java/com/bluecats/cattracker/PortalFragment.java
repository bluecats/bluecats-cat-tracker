/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.*;

public class PortalFragment extends Fragment {
    private ProgressBar mProgressBar;

    private WebView mWebView;
    public WebView getWebView() {
        return mWebView;
    }

    public PortalFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portal, container, false);

        return view;
    }

    @Override
    public void onResume() {
        // ((MainActivity)getActivity()).setNavigationItemChecked(R.id.nav_portal);

        super.onResume();
    }
}
