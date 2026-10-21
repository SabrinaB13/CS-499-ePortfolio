package com.example.watchtheweightfalloff;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

//CRUD for the weights table
public class WeightDao {

    private final DBHelper dbHelper;

    public WeightDao(Context context) {
        dbHelper = new DBHelper(context);
    }

    // This creates and adds a new weight entry
    public boolean addWeight(int userId, String date, double weightValue, Double goalWeight) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_USER_ID_FK, userId);
        values.put(DBHelper.COL_LOG_DATE, date);
        values.put(DBHelper.COL_WEIGHT_VALUE, weightValue);

        if (goalWeight != null) {
            values.put(DBHelper.COL_GOAL_WEIGHT, goalWeight);
        }

        long result = db.insert(DBHelper.TABLE_WEIGHTS, null, values);
        db.close();

        return result != -1;
    }

    // Gets all weights for a user
    // Grid Display

    public List<WeightEntry> getAllWeights(int userId) {
        List<WeightEntry> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBHelper.TABLE_WEIGHTS,
                new String[]{
                        DBHelper.COL_WEIGHT_ID,
                        DBHelper.COL_LOG_DATE,
                        DBHelper.COL_WEIGHT_VALUE,
                        DBHelper.COL_GOAL_WEIGHT
                },
                DBHelper.COL_USER_ID_FK + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                DBHelper.COL_WEIGHT_ID + " DESC"
        );

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String date = cursor.getString(1);
            double weight = cursor.getDouble(2);

            Double goal = null;
            if (!cursor.isNull(3)) {
                goal = cursor.getDouble(3);
            }

            list.add(new WeightEntry(id, date, weight, goal));
        }

        cursor.close();
        db.close();
        return list;
    }

    // Update
    // change a weight entry
    public boolean updateWeight(int weightId, String newDate, double newWeightValue, Double newGoalWeight) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_LOG_DATE, newDate);
        values.put(DBHelper.COL_WEIGHT_VALUE, newWeightValue);

        if (newGoalWeight != null) {
            values.put(DBHelper.COL_GOAL_WEIGHT, newGoalWeight);
        } else {
            values.putNull(DBHelper.COL_GOAL_WEIGHT);
        }

        int rows = db.update(
                DBHelper.TABLE_WEIGHTS,
                values,
                DBHelper.COL_WEIGHT_ID + "=?",
                new String[]{String.valueOf(weightId)}
        );

        db.close();
        return rows > 0;
    }

    // delete
    // remove a weight entry
    public boolean deleteWeight(int weightId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows = db.delete(
                DBHelper.TABLE_WEIGHTS,
                DBHelper.COL_WEIGHT_ID + "=?",
                new String[]{String.valueOf(weightId)}
        );

        db.close();
        return rows > 0;
    }

    //this adds the 5-pound milestone to the database
    public boolean addMilestone(int userId, double milestoneWeight, String date) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_USER_ID_FK, userId);
        values.put(DBHelper.COL_MILESTONE_WEIGHT, milestoneWeight);
        values.put(DBHelper.COL_MILESTONE_DATE, date);

        long result = db.insert(DBHelper.TABLE_MILESTONES, null, values);

        db.close();
        return result != -1;
    }

    //this gets all milestones for a user
    public List<Double> getMilestones(int userId) {
        List<Double> milestones = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBHelper.TABLE_MILESTONES,
                new String[]{DBHelper.COL_MILESTONE_WEIGHT},
                DBHelper.COL_USER_ID_FK + "=?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                DBHelper.COL_MILESTONE_WEIGHT + " DESC"
        );

        while (cursor.moveToNext()) {
            milestones.add(cursor.getDouble(0));
        }

        cursor.close();
        db.close();

        return milestones;
    }
}