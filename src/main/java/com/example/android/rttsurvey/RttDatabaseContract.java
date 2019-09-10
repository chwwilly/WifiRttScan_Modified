package com.example.android.rttsurvey;

import android.provider.BaseColumns;

public class RttDatabaseContract {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RttSurvey.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    private RttDatabaseContract() {}

    public static class Table_Data implements BaseColumns {
        public static final String TABLE_NAME = "Position";
        public static final String COLUMN_BSSID =  "bssid";
        public static final String COLUMN_RANGE =  "range";
        public static final String COLUMN_RANGESD = "rangeSD";
        public static final String COLUMN_RSSI = "rssi";
        public static final String COLUMN_NUMATTMEAS =  "numAttMeas";
        public static final String COLUMN_NUMSUCMEAS = "numSucMeas";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_PERIOD = "period";

        public static final String COLUMN = columnConstruct();

        public static final String CREATE_TABLE_PREFIX = "CREATE TABLE " + TABLE_NAME;
        public static final String CREATE_TABLE_SUFFIX =  "(" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN + COLUMN_PERIOD + " INTEGER)";

        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        private static String columnConstruct() {
            String COL = "";
            for (int i = 0; i < 8; i++) {
                COL +=  COLUMN_BSSID + i + TEXT_TYPE + COMMA_SEP +
                        COLUMN_RANGE + i + INT_TYPE + COMMA_SEP +
                        COLUMN_RANGESD + i + INT_TYPE + COMMA_SEP +
                        COLUMN_RSSI + i + INT_TYPE + COMMA_SEP +
                        COLUMN_NUMATTMEAS + i + INT_TYPE + COMMA_SEP +
                        COLUMN_NUMSUCMEAS + i + INT_TYPE + COMMA_SEP +
                        COLUMN_TIMESTAMP + i + INT_TYPE + COMMA_SEP;
            }
            return COL;
        }
    }

    public static class Table_AP implements BaseColumns {
        public static final String TABLE_NAME = "AP_info";
        public static final String COLUMN_SSID = "ssid";
        public static final String COLUMN_BSSID = "bssid";
        public static final String COLUMN_CAPABILITIES = "capabilities";
        public static final String COLUMN_CENTERFREQ0 = "centerFreq0";
        public static final String COLUMN_CENTERFREQ1 = "centerFreq1";
        public static final String COLUMN_CHWIDTH = "channelWidth";
        public static final String COLUMN_FREQ = "freq";

        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "+ TABLE_NAME +
                "(" + _ID + " INTEGER PRIMARY KEY," +
                COLUMN_SSID + TEXT_TYPE + COMMA_SEP +
                COLUMN_BSSID + TEXT_TYPE + COMMA_SEP +
                COLUMN_CAPABILITIES + TEXT_TYPE + COMMA_SEP +
                COLUMN_CENTERFREQ0 + INT_TYPE + COMMA_SEP +
                COLUMN_CENTERFREQ1 + INT_TYPE + COMMA_SEP +
                COLUMN_CHWIDTH + INT_TYPE + COMMA_SEP +
                COLUMN_FREQ + INT_TYPE + " )";

        public static final String CHECK_AP = "SELECT EXISTS(SELECT 1 FROM " + TABLE_NAME +
                " WHERE bssid = ";
    }
}
