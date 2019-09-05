package com.example.android.rttsurvey;

import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.List;

public class SurveyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    SurveyAdapter.ViewHolderItem mViewHolderItem;

    private List<ScanResult> mScanResultsList;
    private List<RangingResult> mRangingResultList;
    private ScanResult mScanResult;
    private RangingResult mRangingResult;

    public SurveyAdapter(List<ScanResult> ScanResultsList, List<RangingResult> RangingResultList) {
        this.mScanResultsList = ScanResultsList;
        this.mRangingResultList = RangingResultList;
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder {

        public TextView mSsidTextView, mBssidTextView;
        public TextView mRangeTextView, mRssiTextView;

        public ViewHolderItem(View view) {
            super(view);
            mSsidTextView = view.findViewById(R.id.survey_ssid_text_view);
            mBssidTextView = view.findViewById(R.id.survey_bssid_text_view);
            mRangeTextView = view.findViewById(R.id.survey_range_text_view);
            mRssiTextView = view.findViewById(R.id.survey_rssi_text_view);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = new SurveyAdapter.ViewHolderItem(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.recycler_row_survey, parent, false));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {

        mViewHolderItem = (SurveyAdapter.ViewHolderItem) viewHolder;

        mScanResult = mScanResultsList.get(position);
        mViewHolderItem.mSsidTextView.setText(mScanResult.SSID);
        mViewHolderItem.mBssidTextView.setText(mScanResult.BSSID);

        if (mRangingResultList != null &&
                mRangingResultList.get(position).getStatus() == RangingResult.STATUS_SUCCESS) {
            mRangingResult = mRangingResultList.get(position);
            mViewHolderItem.mRangeTextView.setText(String.valueOf(mRangingResult.getDistanceMm()));
            mViewHolderItem.mRssiTextView.setText(String.valueOf(mRangingResult.getRssi()));
        } else {
            mViewHolderItem.mRangeTextView.setText("0");
            mViewHolderItem.mRssiTextView.setText("0");
        }

    }

    public void update(List<RangingResult> list) {
        mRangingResultList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mScanResultsList.size();
    }
}
