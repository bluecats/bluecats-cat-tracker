/*
 * Copyright (c) 2017 BlueCats. All rights reserved.
 * http://www.bluecats.com
 */

package com.bluecats.cattracker;

import android.content.*;
import android.os.*;
import android.util.*;

import com.bluecats.sdk.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

public class Utils {
    private static final String TAG = "Utils";

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().equals("");
    }

    public static String getUTCDateTimeString(long millis) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        return formatter.format(new Date(millis));
    }

    public static String getLastUpdatedAt(long timestamp) {
        String lastUpdatedAtString = "";

        if (timestamp > 0) {
            Calendar today = Calendar.getInstance();
            Calendar lastUpdated = Calendar.getInstance();
            today.setTime(new Date());
            lastUpdated.setTime(new Date(timestamp));

            // if today
            if (today.get(Calendar.YEAR) == lastUpdated.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == lastUpdated.get(Calendar.DAY_OF_YEAR)) {
                DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
                lastUpdatedAtString = String.format("Last updated at %s", timeFormat.format(new Date(timestamp)));
            } else {
                // get difference in days
                long difference = today.getTimeInMillis() - lastUpdated.getTimeInMillis();
                long differenceInDays = difference / (24 * 60 * 60 * 1000);

                if (differenceInDays > 1) {
                    lastUpdatedAtString = String.format("Last updated %s days ago", differenceInDays);
                } else {
                    lastUpdatedAtString = "Last updated yesterday";
                }
            }
        }

        return lastUpdatedAtString;
    }

    public static String getLastHeardAt(long timestamp) {
        String lastHeardAtString = "";

        if (timestamp > 0) {
            Calendar today = Calendar.getInstance();
            Calendar lastUpdated = Calendar.getInstance();
            today.setTime(new Date());
            lastUpdated.setTime(new Date(timestamp));

            // if today
            if (today.get(Calendar.YEAR) == lastUpdated.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == lastUpdated.get(Calendar.DAY_OF_YEAR)) {
                DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
                lastHeardAtString = String.format("Last heard at %s", timeFormat.format(new Date(timestamp))); // TODO fix last heard / updated
            } else {
                // get difference in days
                long difference = today.getTimeInMillis() - lastUpdated.getTimeInMillis();
                long differenceInDays = difference / (24 * 60 * 60 * 1000);

                if (differenceInDays > 1) {
                    lastHeardAtString = String.format("Last heard %s days ago", differenceInDays); // TODO fix last heard / updated
                } else {
                    lastHeardAtString = "Last heard yesterday"; // TODO fix last heard / updated
                }
            }
        }

        return lastHeardAtString;
    }
}
