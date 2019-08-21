package com.example.android.rttsurvey;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RangingSurveyActivity extends AppCompatActivity {
    private static final String TAG = "RSActivity";

    private static final int MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT = 1000;

    public static final String SURVEY_EXTRA =
            "com.example.android.rttsurvey.extra.SURVEY";

    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    private List<ScanResult> mScanResultsList;
    private List<RangingResult> mRangingResultList;

    private RecyclerView mRecyclerView;

    private SurveyAdapter mSurveyAdapter;

    private int mMillisecondsDelayBeforeNewRangingRequest;

    final Handler mRangeRequestDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_ranging_results);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRecyclerView = findViewById(R.id.survey_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        Intent intent = getIntent();
        mScanResultsList = intent.getParcelableArrayListExtra(SURVEY_EXTRA);

        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mRttRangingResultCallback = new RttRangingResultCallback();

        mSurveyAdapter = new SurveyAdapter(mScanResultsList, mRangingResultList);
        mRecyclerView.setAdapter(mSurveyAdapter);

        resetData();
        startRangingRequest();
    }

    @Override
    public void onBackPressed() {
        mRangeRequestDelayHandler.removeCallbacksAndMessages(null);
        finish();
        super.onBackPressed();
    }

    private void resetData() {
        mMillisecondsDelayBeforeNewRangingRequest = MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT;
    }

    private void startRangingRequest() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finish();
        }

        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(mScanResultsList).build();

        mWifiRttManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);

    }

    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private class RttRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest() {
            mRangeRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            startRangingRequest();
                        }
                    },
                    mMillisecondsDelayBeforeNewRangingRequest);
        }

        @Override
        public void onRangingFailure(int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG, "onRangingResults() ");

            mSurveyAdapter.update(list);

            queueNextRangingRequest();
        }
    }
}
