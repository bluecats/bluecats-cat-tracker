/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluecats.sdk.*;
import com.bluecats.cattracker.adapters.*;
import com.bluecats.cattracker.managers.*;
import com.bluecats.cattracker.models.*;
import com.bluecats.cattracker.services.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class BeaconsListFragment extends Fragment {
    private static final String TAG = "BeaconsListFragment";

    private OnListFragmentInteractionListener mListener;

    private Map<String, BCBeacon> mRangedBeaconsByKey;
    private List<BCBeacon> mDetectedBeacons;
    private Map<String, BeaconPayload> mBeaconPayloadsByBeaconIdentifier;
    private List<BeaconPayload> mBeaconPayloads;
    private BeaconsRecyclerViewAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BeaconsListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRangedBeaconsByKey = new ConcurrentHashMap<>();
        mDetectedBeacons = Collections.synchronizedList(new ArrayList<BCBeacon>());
        mBeaconPayloadsByBeaconIdentifier = new ConcurrentHashMap<>();
        mBeaconPayloads = Collections.synchronizedList(new ArrayList<BeaconPayload>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_beacons_list, container, false);

        ((MainActivity)getActivity()).setTitle(getResources().getString(R.string.app_name), null);

        mAdapter = new BeaconsRecyclerViewAdapter(mBeaconPayloads, mListener);

        Context context = view.getContext();
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(mAdapter);

        reloadListView();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;

            BlueCatsSDKInterfaceService.registerBlueCatsSDKServiceCallback(BeaconsListFragment.class.getName(), mBlueCatsSDKInterfaceServiceCallback);

            getActivity().registerReceiver(didUpdateLocalStorage, new IntentFilter(String.format("%s.%s", getActivity().getPackageName(), CatTracker.ACTION_DID_UPDATE_LOCAL_STORAGE)));
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        BlueCatsSDKInterfaceService.unregisterBlueCatsSDKServiceCallback(BeaconsListFragment.class.getName());

        getActivity().unregisterReceiver(didUpdateLocalStorage);
    }

    @Override
    public void onResume() {
        ((MainActivity)getActivity()).setNavigationItemChecked(R.id.nav_cat_tracker);

        super.onResume();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(BeaconPayload item);
    }

    private void reloadListView() {
        final List<BeaconPayload> beaconPayloads = LocalStorageManager.getInstance(getActivity()).getBeaconPayloads();

        // clean out old records
        for (Iterator<BeaconPayload> iterator = mBeaconPayloadsByBeaconIdentifier.values().iterator(); iterator.hasNext();) {
            BeaconPayload beaconPayload = iterator.next();
            if (!beaconPayloads.contains(beaconPayload)) {
                iterator.remove();
            }
        }

        // add current records
        for (BeaconPayload beaconPayload: beaconPayloads) {
            try {
                mBeaconPayloadsByBeaconIdentifier.put(beaconPayload.getBeaconIdentifier(), beaconPayload);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        mBeaconPayloads.clear();
        mBeaconPayloads.addAll(mBeaconPayloadsByBeaconIdentifier.values());

        mAdapter.notifyDataSetChanged();
    }

    private BroadcastReceiver didUpdateLocalStorage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CatTracker.DBG) Log.d(TAG, "didUpdateLocalStorage");

            final Activity activity = BeaconsListFragment.this.getActivity();
            if (activity == null) {
                return;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity == null) {
                        return;
                    }

                    reloadListView();
                }
            });
        }
    };

    private IBlueCatsSDKInterfaceServiceCallback mBlueCatsSDKInterfaceServiceCallback = new IBlueCatsSDKInterfaceServiceCallback() {
        @Override
        public void onDidExitBeacons(final List<BCBeacon> beacons) {
            final Activity activity = BeaconsListFragment.this.getActivity();

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity == null) {
                        return;
                    }

                    // removed exited beacons from list
                    for (BCBeacon beacon: beacons) {
                        try {
                            mRangedBeaconsByKey.remove(beacon.getSerialNumber());
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }

                    mDetectedBeacons.clear();
                    mDetectedBeacons.addAll(mRangedBeaconsByKey.values());

                    mAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDidRangeBeacons(final List<BCBeacon> beacons) {
            final Activity activity = BeaconsListFragment.this.getActivity();

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity == null) {
                        return;
                    }

                    // add ranged beacons to list
                    for (BCBeacon beacon: beacons) {
                        try {
                            mRangedBeaconsByKey.put(beacon.getSerialNumber(), beacon);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }

                    mDetectedBeacons.clear();
                    mDetectedBeacons.addAll(mRangedBeaconsByKey.values());

                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    };
}
