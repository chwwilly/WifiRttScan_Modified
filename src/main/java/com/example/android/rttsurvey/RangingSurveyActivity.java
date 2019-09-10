package com.example.android.rttsurvey;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
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

    private static final int MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT = 1000; // Delay period
    private static final int SAMPLE_SIZE_DEFAULT = 5; // Request window size

    public static final String SURVEY_EXTRA =
            "com.example.android.rttsurvey.extra.SURVEY";

    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    private List<ScanResult> mScanResultsList;
    private List<RangingResult> mRangingResultList;

    private RecyclerView mRecyclerView;

    private SurveyAdapter mSurveyAdapter;

    private int mMillisecondsDelayBeforeNewRangingRequest;
    private int mNumberOfRangeRequests;

    final Handler mRangeRequestDelayHandler = new Handler();

    SQLiteDatabase db;
    String TableName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_ranging_results);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Database instantiate
        DatabaseContext dbConext = new DatabaseContext(this);
        RttDataBaseHelper dbHelper = new RttDataBaseHelper(dbConext);
        db = dbHelper.getWritableDatabase();
        int table_index = RttDataBaseHelper.getTableIndex(db);
        TableName = RttDatabaseContract.Table_Data.TABLE_NAME + table_index;


        mRecyclerView = findViewById(R.id.survey_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        Intent intent = getIntent();
        mScanResultsList = intent.getParcelableArrayListExtra(SURVEY_EXTRA);
        writeAPinfo(mScanResultsList);

        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mRttRangingResultCallback = new RttRangingResultCallback();

        mSurveyAdapter = new SurveyAdapter(mScanResultsList, mRangingResultList);
        mRecyclerView.setAdapter(mSurveyAdapter);

        resetData();
        startRangingRequest();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        db.close();
    }

    @Override
    public void onBackPressed () {
        mRangeRequestDelayHandler.removeCallbacksAndMessages(null);
        finish();
        db.close();
        super.onBackPressed();
    }

    private void writeAPinfo (List<ScanResult> list) {
        for (int i = 0; i < list.size(); i++) {
            ScanResult scanResult = list.get(i);
            if (RttDataBaseHelper.checkAP(db, scanResult.BSSID) == 0) {
                ContentValues values = new ContentValues();
                values.put(RttDatabaseContract.Table_AP.COLUMN_SSID, scanResult.SSID);
                values.put(RttDatabaseContract.Table_AP.COLUMN_BSSID, scanResult.BSSID);
                values.put(RttDatabaseContract.Table_AP.COLUMN_CAPABILITIES, scanResult.capabilities);
                values.put(RttDatabaseContract.Table_AP.COLUMN_CENTERFREQ0, scanResult.centerFreq0);
                values.put(RttDatabaseContract.Table_AP.COLUMN_CENTERFREQ1, scanResult.centerFreq1);
                values.put(RttDatabaseContract.Table_AP.COLUMN_CHWIDTH, scanResult.channelWidth);
                values.put(RttDatabaseContract.Table_AP.COLUMN_FREQ, scanResult.frequency);
                db.insert(RttDatabaseContract.Table_AP.TABLE_NAME, null,values);
            }
        }
    }

    private void resetData () {
        mMillisecondsDelayBeforeNewRangingRequest = MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT;
        mNumberOfRangeRequests = 0;
    }

    private void startRangingRequest () {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finish();
        }
        if (mNumberOfRangeRequests >= SAMPLE_SIZE_DEFAULT) {
            onBackPressed();
            return;
        }
        mNumberOfRangeRequests ++;

        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(mScanResultsList).build();

        mWifiRttManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);
    }

    private ContentValues insertValues (int i, RangingResult rangingResult, ContentValues values) {
        values.put(RttDatabaseContract.Table_Data.COLUMN_BSSID + i, rangingResult.getMacAddress().toString());
        if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
            values.put(RttDatabaseContract.Table_Data.COLUMN_RANGE + i, rangingResult.getDistanceMm());
            values.put(RttDatabaseContract.Table_Data.COLUMN_RANGESD + i, rangingResult.getDistanceStdDevMm());
            values.put(RttDatabaseContract.Table_Data.COLUMN_NUMATTMEAS + i, rangingResult.getNumAttemptedMeasurements());
            values.put(RttDatabaseContract.Table_Data.COLUMN_NUMSUCMEAS + i, rangingResult.getNumSuccessfulMeasurements());
            values.put(RttDatabaseContract.Table_Data.COLUMN_TIMESTAMP + i, rangingResult.getRangingTimestampMillis());
            values.put(RttDatabaseContract.Table_Data.COLUMN_RSSI + i, rangingResult.getRssi());
        }
        return values;
    }

    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private class RttRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest () {
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
        public void onRangingFailure (int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG, "onRangingResults(): "+ list.get(0).toString());

            ContentValues values = new ContentValues();
            for (int i = 0; i < list.size(); i++) {
                try {
                    values = insertValues(i, list.get(i), values);
                } catch (Exception e) {
                    // Blank
                }
            }
            db.insert(TableName, null, values);
            mSurveyAdapter.update(list); // Show updated data in UI
            queueNextRangingRequest();
        }
    }
}
