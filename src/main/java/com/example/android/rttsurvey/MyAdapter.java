/*
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.rttsurvey;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.ScanResult;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.pow;

/**
 * Displays the ssid and bssid from a list of {@link ScanResult}s including a header at the top of
 * the {@link RecyclerView} to label the data.
 */
public class MyAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int HEADER_POSITION = 0;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private static ScanResultClickListener sScanResultClickListener;

    private List<ScanResult> mWifiAccessPointsWithRtt, selected;

    private static final String TAG = "Recycler";


    public MyAdapter(List<ScanResult> list, ScanResultClickListener scanResultClickListener) {
        this.mWifiAccessPointsWithRtt = list;
        this.sScanResultClickListener = scanResultClickListener;
        this.selected = new ArrayList<>();
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {
        public ViewHolderHeader(View view) {
            super(view);
        }
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mSsidTextView;
        public TextView mBssidTextView;

        public ViewHolderItem(View view) {
            super(view);
            view.setOnClickListener(this);
            mSsidTextView = view.findViewById(R.id.ssid_text_view);
            mBssidTextView = view.findViewById(R.id.bssid_text_view);
        }

        @Override
        public void onClick (View view) {

        }
    }

    public void swapData(List<ScanResult> list) {

        // Always clear with any update, as even an empty list means no WifiRtt devices were found.
        mWifiAccessPointsWithRtt.clear();

        if ((list != null) && (list.size() > 0)) {
            mWifiAccessPointsWithRtt.addAll(list);
        }

        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ViewHolder viewHolder;

        if (viewType == TYPE_HEADER) {
            viewHolder =
                    new ViewHolderHeader(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.recycler_row_header, parent, false));

        } else if (viewType == TYPE_ITEM) {
            viewHolder =
                    new ViewHolderItem(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.recycler_row_item, parent, false));
        } else {
            throw new RuntimeException(viewType + " isn't a valid view type.");
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        // Change color when the recyclerview is onclick
        if (position > 0) {
            final ScanResult item = getItem(position);

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (selected.contains(item)) {
                        selected.remove(item);
                        unhighlightView(viewHolder);
                    } else {
                        selected.add(item);
                        highlightView(viewHolder);
                    }
                    Log.d(TAG, "ScanResultItemClick ");
                }
            });
            if (selected.contains(item))
                highlightView(viewHolder);
            else
                unhighlightView(viewHolder);
        }

        if (viewHolder instanceof ViewHolderHeader) {
            // No updates need to be made to header view (defaults remain same).

        } else if (viewHolder instanceof ViewHolderItem) {
            ViewHolderItem viewHolderItem = (ViewHolderItem) viewHolder;
            ScanResult currentScanResult = getItem(position);

            viewHolderItem.mSsidTextView.setText(currentScanResult.SSID);
            viewHolderItem.mBssidTextView.setText(currentScanResult.BSSID);

        } else {
            throw new RuntimeException(viewHolder + " isn't a valid view holder.");
        }
    }

    private void highlightView(ViewHolder holder) {
        holder.itemView.setBackgroundColor(Color.parseColor("#78C5EF"));
    }

    private void unhighlightView(ViewHolder holder) {
        holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
    }

    /*
     * Because we added a header item to the list, we need to decrement the position by one to get
     * the proper place in the list.
     */
    private ScanResult getItem(int position) {
        return mWifiAccessPointsWithRtt.get(position - 1);
    }

    // Returns size of list plus the header item (adds extra item).
    @Override
    public int getItemCount() {
        return mWifiAccessPointsWithRtt.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == HEADER_POSITION) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }
    }

    public List<ScanResult> getSelected() {
        Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult ap1, ScanResult ap2) {
                int mac1 = macToInt(ap1.BSSID);
                int mac2 = macToInt(ap2.BSSID);
                return (mac1 < mac2 ? -1 : (mac1 == mac2 ? 0 : 1));
            }
        };
        Collections.sort(selected, comparator);
        return selected;
    }

    private int macToInt (String mac) {
        String[] macParts = mac.split(":");
        int macInt = 0;
        for(int i=0; i<6; i++){
            Integer hex = Integer.parseInt(macParts[i], 16);
            macInt += hex.byteValue() * pow(16, i);
        }
        return macInt;
    }

    // Used to inform the class containing the RecyclerView that one of the ScanResult items in the
    // list was clicked.
    public interface ScanResultClickListener {
        void onScanResultItemClick(ScanResult scanResult);
    }
}
