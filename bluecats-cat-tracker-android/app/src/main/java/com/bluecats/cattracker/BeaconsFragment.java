package com.bluecats.cattracker;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.bluecats.cattracker.BeaconsListFragment.*;

import static android.content.Context.MODE_PRIVATE;

public class BeaconsFragment extends Fragment {
    private static final String TAG = "BeaconsFragment";

    private RelativeLayout rlLastUpdated;
    private TextView tvLastUpdated;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BeaconsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_beacons, container, false);

        Fragment fragment = new BeaconsListFragment();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_beacons_list, fragment);
        fragmentTransaction.commit();

        rlLastUpdated = (RelativeLayout) view.findViewById(R.id.rl_last_updated);
        tvLastUpdated = (TextView) view.findViewById(R.id.tv_last_updated);
        rlLastUpdated.setVisibility(View.GONE);

        setLastUpdatedAt();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            getActivity().registerReceiver(didUploadBeaconPayloads, new IntentFilter(String.format("%s.%s", getActivity().getPackageName(), CatTracker.ACTION_DID_UPLOAD_BEACON_PAYLOADS)));
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        getActivity().unregisterReceiver(didUploadBeaconPayloads);
    }

    private void setLastUpdatedAt() {
        SharedPreferences sp = getActivity().getSharedPreferences(getActivity().getPackageName(), MODE_PRIVATE);
        if (sp.contains(CatTracker.KEY_LAST_UPDATED_AT)) {
            Long lastUpdateAt = sp.getLong(CatTracker.KEY_LAST_UPDATED_AT, 0);
            tvLastUpdated.setText(Utils.getLastUpdatedAt(lastUpdateAt));
            rlLastUpdated.setVisibility(View.VISIBLE);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    final Activity activity = BeaconsFragment.this.getActivity();
                    if (activity == null) {
                        return;
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activity == null) {
                                return;
                            }

                            rlLastUpdated.setVisibility(View.GONE);
                        }
                    });
                }
            }, 20000);
        }
    }

    private BroadcastReceiver didUploadBeaconPayloads = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CatTracker.DBG) Log.d(TAG, "didUploadBeaconPayloads");

            final Activity activity = BeaconsFragment.this.getActivity();
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
