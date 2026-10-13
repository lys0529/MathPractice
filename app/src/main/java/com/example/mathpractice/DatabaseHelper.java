package com.example.mathpractice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "MathPractice.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_RECORDS = "records";
    private static final String COL_ID = "id";
    private static final String COL_DATE = "date";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_TOTAL = "total";
    private static final String COL_CORRECT = "correct";
    private static final String COL_TIME_SECONDS = "time_seconds";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_RECORDS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DATE + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_TOTAL + " INTEGER, " +
                COL_CORRECT + " INTEGER, " +
                COL_TIME_SECONDS + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        onCreate(db);
    }

    public void saveRecord(int total, int correct, int timeSeconds) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        values.put(COL_DATE, today);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_TOTAL, total);
        values.put(COL_CORRECT, correct);
        values.put(COL_TIME_SECONDS, timeSeconds);
        db.insert(TABLE_RECORDS, null, values);
        db.close();
    }

    public List<DailyStats> getAllDailyStats() {
        List<DailyStats> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT " + COL_DATE +
                ", COUNT(*) as sessions" +
                ", SUM(" + COL_TOTAL + ") as total" +
                ", SUM(" + COL_CORRECT + ") as correct" +
                ", SUM(" + COL_TIME_SECONDS + ") as totalTime" +
                " FROM " + TABLE_RECORDS +
                " GROUP BY " + COL_DATE +
                " ORDER BY " + COL_DATE + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            DailyStats stats = new DailyStats();
            stats.date = cursor.getString(0);
            stats.sessions = cursor.getInt(1);
            stats.totalQuestions = cursor.getInt(2);
            stats.correctAnswers = cursor.getInt(3);
            stats.totalTimeSeconds = cursor.getInt(4);
            list.add(stats);
        }
        cursor.close();
        db.close();
        return list;
    }

    public static class DailyStats {
        public String date;
        public int sessions;
        public int totalQuestions;
        public int correctAnswers;
        public int totalTimeSeconds;

        public int getAccuracy() {
            if (totalQuestions == 0) return 0;
            return (int) (correctAnswers * 100.0 / totalQuestions);
        }

        public String getTimeFormatted() {
            int min = totalTimeSeconds / 60;
            int sec = totalTimeSeconds % 60;
            return min + "分" + sec + "秒";
        }
    }
}
