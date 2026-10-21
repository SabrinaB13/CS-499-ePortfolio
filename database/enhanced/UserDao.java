package com.example.watchtheweightfalloff;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserDao {

    private final DBHelper dbHelper;

    public UserDao(Context context) {
        dbHelper = new DBHelper(context);
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBHelper.TABLE_USERS,
                new String[]{DBHelper.COL_USER_ID},
                DBHelper.COL_USERNAME + " = ?",
                new String[]{username},
                null,
                null,
                null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();

        return exists;
    }

    public boolean createUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_USERNAME, username);
        values.put(DBHelper.COL_PASSWORD, password);

        long result = db.insert(DBHelper.TABLE_USERS, null, values);

        db.close();
        return result != -1;
    }

    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBHelper.TABLE_USERS,
                new String[]{DBHelper.COL_USER_ID},
                DBHelper.COL_USERNAME + " = ? AND " + DBHelper.COL_PASSWORD + " = ?",
                new String[]{username, password},
                null,
                null,
                null
        );

        boolean valid = cursor.moveToFirst();
        cursor.close();
        db.close();

        return valid;
    }

    public int getUserIdByUsername(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBHelper.TABLE_USERS,
                new String[]{DBHelper.COL_USER_ID},
                DBHelper.COL_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );

        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return id;
    }

    //this saves the user's starting weight
    public boolean setStartingWeight(int userId, double startingWeight) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STARTING_WEIGHT, startingWeight);

        int rows = db.update(
                DBHelper.TABLE_USERS,
                values,
                DBHelper.COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        db.close();
        return rows > 0;
    }


    //this gets the user's starting weight
    public Double getStartingWeight(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBHelper.TABLE_USERS,
                new String[]{DBHelper.COL_STARTING_WEIGHT},
                DBHelper.COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        );

        Double startingWeight = null;

        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            startingWeight = cursor.getDouble(0);
        }

        cursor.close();
        db.close();

        return startingWeight;
    }
}