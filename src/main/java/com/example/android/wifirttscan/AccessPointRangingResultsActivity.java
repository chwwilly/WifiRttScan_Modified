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
package com.example.android.wifirttscan;
//import com.example.android.wifirttscan.MailService;


import android.Manifest;
import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Displays ranging information about a particular access point chosen by the user. Uses {@link
 * Handler} to trigger new requests based on
 */
public class AccessPointRangingResultsActivity extends AppCompatActivity {
    private static final String TAG = "APRRActivity";

    public static final String SCAN_RESULT_EXTRA =
            "com.example.android.wifirttscan.extra.SCAN_RESULT";

    private static final int SAMPLE_SIZE_DEFAULT = 10000;
    private static final int MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT = 1000;
    private static final int WINDOW_SIZE = 100;

    // UI Elements.
    private TextView mSsidTextView;
    private TextView mBssidTextView;

    private TextView mRangeTextView;
    private TextView mRangeMeanTextView;
    private TextView mRangeSDTextView;
    private TextView mRangeSDMeanTextView;
    private TextView mRssiTextView;
    private TextView mSuccessesInBurstTextView;
    private TextView mSuccessRatioTextView;
    private TextView mNumberOfRequestsTextView;
    private TextView mChannelTextView;
    private TextView mBandwidthTextView;

    private EditText mSampleSizeEditText;
    private EditText mMillisecondsDelayBeforeNewRangingRequestEditText;

    // Non UI variables.
    private ScanResult mScanResult;
    private String mMAC;

    private int mNumberOfRangeRequests;
    private int mNumberOfSuccessfulRangeRequests;

    private Date mStartTime;
    private Date mEndTime;

    private int mMillisecondsDelayBeforeNewRangingRequest;

    // Max sample size to calculate average for
    // 1. Distance to device (getDistanceMm) over time
    // 2. Standard deviation of the measured distance to the device (getDistanceStdDevMm) over time
    // Note: A RangeRequest result already consists of the average of 7 readings from a burst,
    // so the average in (1) is the average of these averages.
    private int mSampleSize;

    // Used to loop over a list of distances to calculate averages (ensures data structure never
    // get larger than sample size).
    private int mStatisticRangeHistoryEndIndex;
    private ArrayList<Integer> mStatisticRangeHistory;

    // Used to loop over a list of the standard deviation of the measured distance to calculate
    // averages  (ensures data structure never get larger than sample size).
    private int mStatisticRangeSDHistoryEndIndex;
    private ArrayList<Integer> mStatisticRangeSDHistory;

    private int mRssiHistoryEndIndex;
    private ArrayList<Integer> mRssiHistory;

    private int mNumSuccessfulMeasurementsHistoryEndIndex;
    private ArrayList<Integer> mNumSuccessfulMeasurementsHistory;

    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;

    // Triggers additional RangingRequests with delay (mMillisecondsDelayBeforeNewRangingRequest).
    final Handler mRangeRequestDelayHandler = new Handler();

    private FileOutputStream mfos = null;
    private String mFilename;
    private File mfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_point_ranging_results);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initializes UI elements.
        mSsidTextView = findViewById(R.id.ssid);
        mBssidTextView = findViewById(R.id.bssid);

        mRangeTextView = findViewById(R.id.range_value);
        mRangeMeanTextView = findViewById(R.id.range_mean_value);
        mRangeSDTextView = findViewById(R.id.range_sd_value);
        mRangeSDMeanTextView = findViewById(R.id.range_sd_mean_value);
        mRssiTextView = findViewById(R.id.rssi_value);
        mSuccessesInBurstTextView = findViewById(R.id.successes_in_burst_value);
        mSuccessRatioTextView = findViewById(R.id.success_ratio_value);
        mNumberOfRequestsTextView = findViewById(R.id.number_of_requests_value);

        mChannelTextView = findViewById(R.id.channel);
        mBandwidthTextView = findViewById(R.id.bandwidth);

        mSampleSizeEditText = findViewById(R.id.stats_window_size_edit_value);
        mSampleSizeEditText.setText(SAMPLE_SIZE_DEFAULT + "");

        mMillisecondsDelayBeforeNewRangingRequestEditText =
                findViewById(R.id.ranging_period_edit_value);
        mMillisecondsDelayBeforeNewRangingRequestEditText.setText(
                MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT + "");

        // Retrieve ScanResult from Intent.
        Intent intent = getIntent();
        mScanResult = intent.getParcelableExtra(SCAN_RESULT_EXTRA);

        if (mScanResult == null) {
            finish();
        }

        mMAC = mScanResult.BSSID;

        mSsidTextView.setText(mScanResult.SSID);
        mBssidTextView.setText(mScanResult.BSSID);

        mChannelTextView.setText(Integer.toString(mScanResult.frequency));

        int wifiwidth = mScanResult.channelWidth;
        if (wifiwidth == 0) {
            mBandwidthTextView.setText("20 MHz");
        } else if (wifiwidth == 1) {
            mBandwidthTextView.setText("40 MHz");
        } else if (wifiwidth == 2) {
            mBandwidthTextView.setText("80 MHz");
        } else if (wifiwidth == 3) {
            mBandwidthTextView.setText("160 MHz");
        } else {
            mBandwidthTextView.setText("80 + 80 MHz");
        }


        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mRttRangingResultCallback = new RttRangingResultCallback();

        // Used to store range (distance) and rangeSd (standard deviation of the measured distance)
        // history to calculate averages.
        mStatisticRangeHistory = new ArrayList<>();
        mStatisticRangeSDHistory = new ArrayList<>();
        mRssiHistory = new ArrayList<>();
        mNumSuccessfulMeasurementsHistory = new ArrayList<>();

        resetData();
        openFile();

        startRangingRequest();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        saveData();
        mfile.delete();
        finish();
    }

    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1024:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void resetData() {
        mSampleSize = Integer.parseInt(mSampleSizeEditText.getText().toString());

        mMillisecondsDelayBeforeNewRangingRequest =
                Integer.parseInt(
                        mMillisecondsDelayBeforeNewRangingRequestEditText.getText().toString());

        mNumberOfSuccessfulRangeRequests = 0;
        mNumberOfRangeRequests = 0;

        mStatisticRangeHistoryEndIndex = 0;
        mStatisticRangeHistory.clear();

        mRssiHistoryEndIndex = 0;
        mRssiHistory.clear();

        mNumSuccessfulMeasurementsHistoryEndIndex = 0;
        mNumSuccessfulMeasurementsHistory.clear();
    }

    private void startRangingRequest() {
        // Permission for fine location should already be granted via MainActivity (you can't get
        // to this class unless you already have permission. If they get to this class, then disable
        // fine location permission, we kick them back to main activity.
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finish();
        }

        mNumberOfRangeRequests++;

        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoint(mScanResult).build();

        mWifiRttManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);

        if (mNumberOfRangeRequests == mSampleSize + 1) {
            saveData();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            writeDescribeFile();
            resetData();
            //sendEmail();
            openFile();
        }
    }

    // Calculates average distance based on stored history.
    private float getDistanceMean() {
        float distanceSum = 0;

        for (int distance : mStatisticRangeHistory) {
            distanceSum += distance;
        }

        return distanceSum / mStatisticRangeHistory.size();
    }

    // Adds distance to history. If larger than sample size value, loops back over and replaces the
    // oldest distance record in the list.
    private void addDistanceToHistory(int distance) {

        if (mStatisticRangeHistory.size() >= WINDOW_SIZE) {


            if (mStatisticRangeHistoryEndIndex >= WINDOW_SIZE) {
                mStatisticRangeHistoryEndIndex = 0;
            }

            mStatisticRangeHistory.set(mStatisticRangeHistoryEndIndex, distance);
            mStatisticRangeHistoryEndIndex++;

        } else {
            mStatisticRangeHistory.add(distance);
        }
    }

    // Calculates standard deviation of the measured distance based on stored history.
    private float getStandardDeviationOfDistanceMean() {
        float distanceSdSum = 0;

        for (int distanceSd : mStatisticRangeSDHistory) {
            distanceSdSum += distanceSd;
        }

        return distanceSdSum / mStatisticRangeHistory.size();
    }

    // Adds standard deviation of the measured distance to history. If larger than sample size
    // value, loops back over and replaces the oldest distance record in the list.
    private void addStandardDeviationOfDistanceToHistory(int distanceSd) {

        if (mStatisticRangeSDHistory.size() >= WINDOW_SIZE) {

            if (mStatisticRangeSDHistoryEndIndex >= WINDOW_SIZE) {
                mStatisticRangeSDHistoryEndIndex = 0;
            }

            mStatisticRangeSDHistory.set(mStatisticRangeSDHistoryEndIndex, distanceSd);
            mStatisticRangeSDHistoryEndIndex++;

        } else {
            mStatisticRangeSDHistory.add(distanceSd);
        }
    }

    private void addRssiToHistory(int Rssi) {
        if (mRssiHistory.size() >= WINDOW_SIZE) {

            if (mRssiHistoryEndIndex >= WINDOW_SIZE) {
                mRssiHistoryEndIndex = 0;
            }

            mRssiHistory.set(mRssiHistoryEndIndex, Rssi);
            mRssiHistoryEndIndex++;

        } else {
            mRssiHistory.add(Rssi);
        }
    }

    private void addNumSuccessfulMeasurementsToHistory(int NumSuccessfulMeasurements) {
        if (mNumSuccessfulMeasurementsHistory.size() >= WINDOW_SIZE) {

            if (mNumSuccessfulMeasurementsHistoryEndIndex >= WINDOW_SIZE) {
                mNumSuccessfulMeasurementsHistoryEndIndex = 0;
            }

            mNumSuccessfulMeasurementsHistory.set(mNumSuccessfulMeasurementsHistoryEndIndex, NumSuccessfulMeasurements);
            mNumSuccessfulMeasurementsHistoryEndIndex++;

        } else {
            mNumSuccessfulMeasurementsHistory.add(NumSuccessfulMeasurements);
        }
    }

    public void onResetButtonClick(View view) {
        saveData();
        mfile.delete();
        resetData();
        openFile();
    }

    private void saveData() {
        try {
            mfos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void wrtieData(int i, int distance, int distanceSd, int Rssi, int NumSuccessfulMeasurements) {
        String content = String.format("% 8d % 8d % 8d % 8d % 8d \n", i, distance, distanceSd, Rssi, NumSuccessfulMeasurements);
        try {
            mfos.write(content.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFile() {
        if (ActivityCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }

        mStartTime = Calendar.getInstance().getTime();
        mFilename =  mStartTime.toString() + ".txt";

        mfile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), mFilename);
        try {
            mfos = new FileOutputStream(mfile);
            String header = String.format("%8s %8s %8s %8s %8s\n", "Num", "Range", "RangeSD", "Rssi", "NumFTM");
            mfos.write(header.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeDescribeFile() {
        try {
            mEndTime = Calendar.getInstance().getTime();
            long elapsed = (mEndTime.getTime() -mStartTime.getTime()) / 1000;

            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "RTT.txt");

            if(file.exists()) {
                FileOutputStream fos = new FileOutputStream(file, true);
                String content = String.format("%8s %20s %20s %20s %10s %10s %10s %10s %10s\n",
                        1, mScanResult.SSID, mMAC, mScanResult.frequency, mScanResult.channelWidth, mSampleSize, mMillisecondsDelayBeforeNewRangingRequest, elapsed);
                fos.write(content.getBytes());
                fos.close();
            } else {
                FileOutputStream fos = new FileOutputStream(file);
                String header = String.format("%8s %20s %20s %20s %10s %10s %10s %10s %10s\n", "Num", "SSID", "BSSID", "Channel", "Bandwidth","Requests", "Period", "Elapsed");
                String content = String.format("%8s %20s %20s %20s %10s %10s %10s %10s %10s\n",
                        1, mScanResult.SSID, mMAC, mScanResult.frequency, mScanResult.channelWidth, mSampleSize, mMillisecondsDelayBeforeNewRangingRequest, elapsed);
                fos.write(header.getBytes());
                fos.write(content.getBytes());
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendEmail() {
        // TODO (chanwilly): Send email notification when measurement is done.
        MailService mailer = new MailService("jhw1994@gmail.com","r05942023@g.ntu.edu.tw","Subject","TextBody", "<b>HtmlBody</b>");
        try {
            mailer.sendAuthenticated();
        } catch (Exception e) {
            Log.e(TAG, "Failed sending email.", e);
        }

        /*MailService mailer = new MailService("from@mydomain.com","to@domain.com","Subject","TextBody", "<b>HtmlBody</b>", (Attachment) null);
        try {
            mailer.sendAuthenticated();
        } catch (Exception e) {
            Log.e(AskTingTing.APP, "Failed sending email.", e);
        }*/
    }

    public void onSaveButtonClick(View view) {
        saveData();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        resetData();
        openFile();
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
            Log.d(TAG, "onRangingResults(): " + list);

            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)
            if (list.size() == 1) {

                RangingResult rangingResult = list.get(0);

                if (mMAC.equals(rangingResult.getMacAddress().toString())) {

                    if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {

                        mNumberOfSuccessfulRangeRequests++;

                        mRangeTextView.setText((rangingResult.getDistanceMm() / 1000f) + "");
                        addDistanceToHistory(rangingResult.getDistanceMm());
                        mRangeMeanTextView.setText((getDistanceMean() / 1000f) + "");

                        mRangeSDTextView.setText(
                                (rangingResult.getDistanceStdDevMm() / 1000f) + "");
                        addStandardDeviationOfDistanceToHistory(
                                rangingResult.getDistanceStdDevMm());
                        mRangeSDMeanTextView.setText(
                                (getStandardDeviationOfDistanceMean() / 1000f) + "");

                        addRssiToHistory(rangingResult.getRssi());
                        mRssiTextView.setText(rangingResult.getRssi() + "");

                        addNumSuccessfulMeasurementsToHistory(rangingResult.getNumSuccessfulMeasurements());
                        mSuccessesInBurstTextView.setText(
                                rangingResult.getNumSuccessfulMeasurements()
                                        + "/"
                                        + rangingResult.getNumAttemptedMeasurements());

                        float successRatio =
                                ((float) mNumberOfSuccessfulRangeRequests
                                                / (float) mNumberOfRangeRequests)
                                        * 100;
                        mSuccessRatioTextView.setText(successRatio + "%");

                        mNumberOfRequestsTextView.setText(mNumberOfRangeRequests + "");


                        wrtieData(mNumberOfRangeRequests, rangingResult.getDistanceMm(), rangingResult.getDistanceStdDevMm(),
                                rangingResult.getRssi(), rangingResult.getNumSuccessfulMeasurements());
                    } else if (rangingResult.getStatus()
                            == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                        Log.d(TAG, "RangingResult failed (AP doesn't support IEEE80211 MC.");

                    } else {
                        Log.d(TAG, "RangingResult failed.");
                    }

                } else {
                    Toast.makeText(
                                    getApplicationContext(),
                                    R.string
                                            .mac_mismatch_message_activity_access_point_ranging_results,
                                    Toast.LENGTH_LONG)
                            .show();
                }
            }

            queueNextRangingRequest();
        }
    }
}
