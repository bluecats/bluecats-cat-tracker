/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker.adapters;

import android.content.*;
import android.media.*;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.bluecats.cattracker.services.*;
import com.bluecats.sdk.*;
import com.bluecats.cattracker.*;
import com.bluecats.cattracker.BeaconsListFragment.*;
import com.bluecats.cattracker.R;
import com.bluecats.cattracker.models.*;
import com.squareup.picasso.*;

import java.util.*;

/**
 * {@link RecyclerView.Adapter} that can display a {@link BCBeacon} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class BeaconsRecyclerViewAdapter extends RecyclerView.Adapter<BeaconsRecyclerViewAdapter.ViewHolder> {
    private Context mContext;

    private final List<BeaconPayload> mValues;
    private final OnListFragmentInteractionListener mListener;

    public BeaconsRecyclerViewAdapter(List<BeaconPayload> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_beacon, parent, false);

        mContext = parent.getContext();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final BeaconPayload beaconPayload = mValues.get(position);
        holder.mIdentifier.setText(beaconPayload.getBeaconIdentifier());
        holder.mLastHeard.setText(Utils.getLastHeardAt(mValues.get(position).getTimestamp()));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(beaconPayload);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mIcon;
        public final TextView mIdentifier;
        public final TextView mLastHeard;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIcon = (ImageView) view.findViewById(R.id.iv_icon);
            mIdentifier = (TextView) view.findViewById(R.id.tv_identifier);
            mLastHeard = (TextView) view.findViewById(R.id.tv_last_heard);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mIdentifier.getText() + "'";
        }
    }
}
