package com.example.watchtheweightfalloff;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // Users Table
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // Weights Table
    public static final String TABLE_WEIGHTS = "weights";
    public static final String COL_WEIGHT_ID = "weight_id";
    public static final String COL_USER_ID_FK = "user_id"; // links weights to a user
    public static final String COL_LOG_DATE = "log_date";
    public static final String COL_WEIGHT_VALUE = "weight_value";
    public static final String COL_GOAL_WEIGHT = "goal_weight";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create Users table
        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                        COL_PASSWORD + " TEXT NOT NULL" +
                        ");";
        db.execSQL(createUsersTable);

        // Create Weights table
        String createWeightsTable =
                "CREATE TABLE " + TABLE_WEIGHTS + " (" +
                        COL_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USER_ID_FK + " INTEGER NOT NULL, " +
                        COL_LOG_DATE + " TEXT NOT NULL, " +
                        COL_WEIGHT_VALUE + " REAL NOT NULL, " +
                        COL_GOAL_WEIGHT + " REAL" +
                        ");";
        db.execSQL(createWeightsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For class projects, easiest is drop + recreate
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}