package com.example.watchtheweightfalloff;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

// Dialog imports which are used for popups
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

import java.util.List;

public class LoggedWeightsActivity extends AppCompatActivity {

    // These match with the UI widgets that are located in activity_logged_weights.xml
    // The IDs MUST exist in that XML file or they will be NULL

    private LinearLayout layoutRows;
    private Button buttonDailyWeight;
    private Button buttonGoalWeight;
    private TextView textGoalWeight;
    private ProgressBar progressWeightGoal;
    private TextView textProgressPercent;
    private TextView textPoundsRemaining;

    // These will connect to with the DBHelper and DAOs
    // DBHelper.java needs to create the tables/column used in WeightDao and UserDao.

    private WeightDao weightDao;
    private UserDao userDao;

    // Log-in user info
    private int userId = -1;
    private String username = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_weights);

        // Connect UI elements
        // All need to exist in activity_logged_weights.xml
        layoutRows = findViewById(R.id.layoutRows);
        buttonDailyWeight = findViewById(R.id.buttonDailyWeight);
        buttonGoalWeight = findViewById(R.id.buttonGoalWeight);
        textGoalWeight = findViewById(R.id.textGoalWeight);
        progressWeightGoal = findViewById(R.id.progressWeightGoal);
        textProgressPercent = findViewById(R.id.textProgressPercent);
        textPoundsRemaining = findViewById(R.id.textPoundsRemaining);

        // SMS Settings button
        Button buttonSmsSettings = findViewById(R.id.buttonSmsSettings);
        if (buttonSmsSettings != null) {
            buttonSmsSettings.setOnClickListener(v -> {
                Intent intent = new Intent(LoggedWeightsActivity.this, NotificationSettingsActivity.class);
                startActivity(intent);
            });
        }

        // Safety check
        // Prevents null crashes if the IDs are showing wrong
        if (layoutRows == null || buttonDailyWeight == null || buttonGoalWeight == null) {
            Toast.makeText(this, "ERROR: Missing required UI IDs in activity_logged_weights.xml", Toast.LENGTH_LONG).show();
            return;
        }

        // Set up DAOs
        // DBHelper must exist and create the needed tables.
        weightDao = new WeightDao(this);
        userDao = new UserDao(this);

        // Gets the username passed from MainActivity
        // MainActivity must do intent.putExtra("username", username) for this to work
        username = getIntent().getStringExtra("username");
        if (username == null) username = "";

        // Gets the userId from username
        // UserDao must have getUserIdByUsername() and DB must have users table for this to work
        userId = userDao.getUserIdByUsername(username);
        if (userId == -1) {
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }



        refreshGrid();

        // Daily Weight: asks for Date + Weight
        buttonDailyWeight.setOnClickListener(v -> showDailyWeightDialog());

        // Goal Weight: asks for Goal only
        buttonGoalWeight.setOnClickListener(v -> showGoalWeightDialog());
    }

    private void showDailyWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Daily Weight");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText editDate = new EditText(this);
        editDate.setHint("Date (MM/DD)");
        editDate.setInputType(InputType.TYPE_CLASS_TEXT);
        container.addView(editDate);

        EditText editWeight = new EditText(this);
        editWeight.setHint("Weight in lbs");
        editWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(editWeight);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String date = editDate.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();

            if (date.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please enter a date and weight.", Toast.LENGTH_SHORT).show();
                return;
            }

            double weightValue;
            try {
                weightValue = Double.parseDouble(weightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Weight must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Uses the latest goal if one exists
            List<WeightEntry> entries = weightDao.getAllWeights(userId);
            Double currentGoal = getLatestGoalWeight(entries);

            boolean added = weightDao.addWeight(userId, date, weightValue, currentGoal);

            if (added) {
                Toast.makeText(this, "Daily weight saved!", Toast.LENGTH_SHORT).show();
                refreshGrid();
            } else {
                Toast.makeText(this, "Save failed.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showGoalWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Goal Weight");

        // Uses a small vertical container
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        // User must type in the date
        EditText editDate = new EditText(this);
        editDate.setHint("Date (MM/DD)");
        editDate.setInputType(InputType.TYPE_CLASS_TEXT);
        container.addView(editDate);

        // Goal input
        EditText editGoal = new EditText(this);
        editGoal.setHint("Goal Weight in lbs");
        editGoal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(editGoal);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String date = editDate.getText().toString().trim();
            String goalStr = editGoal.getText().toString().trim();

            if (date.isEmpty() || goalStr.isEmpty()) {
                Toast.makeText(this, "Please enter a date and goal weight.", Toast.LENGTH_SHORT).show();
                return;
            }

            double goalValue;
            try {
                goalValue = Double.parseDouble(goalStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Goal weight must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reuses the most recent weight if one exists
            List<WeightEntry> entries = weightDao.getAllWeights(userId);
            double lastWeight = entries.isEmpty() ? 0.0 : entries.get(0).weight;

            boolean added = weightDao.addWeight(userId, date, lastWeight, goalValue);

            if (added) {
                Toast.makeText(this, "Goal weight saved!", Toast.LENGTH_SHORT).show();
                refreshGrid();
            } else {
                Toast.makeText(this, "Save failed.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void refreshGrid() {
        layoutRows.removeAllViews();

        List<WeightEntry> entries = weightDao.getAllWeights(userId);

        //this updates the progress ring
        updateGoalProgress(entries);

        // Displays the goal weight at the top of the screen
        String goalDisplay = "Goal Weight: Not set";
        Double latestGoal = getLatestGoalWeight(entries);
        if (latestGoal != null) {
            goalDisplay = "Goal Weight: " + latestGoal;
        }
        if (textGoalWeight != null) textGoalWeight.setText(goalDisplay);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (WeightEntry entry : entries) {
            View row = inflater.inflate(R.layout.row_weight, layoutRows, false);

            EditText editRowDate = row.findViewById(R.id.editRowDate);
            EditText editRowWeight = row.findViewById(R.id.editRowWeight);
            TextView textToGoal = row.findViewById(R.id.textRowToGoal);
            Button buttonUpdate = row.findViewById(R.id.buttonRowUpdate);
            Button buttonDelete = row.findViewById(R.id.buttonRowDelete);

            // Fills the values
            editRowDate.setText(entry.date);
            editRowWeight.setText(String.valueOf(entry.weight));

            // Lbs to goal
            if (entry.goalWeight != null) {
                double toGoal = entry.weight - entry.goalWeight;
                textToGoal.setText(String.valueOf((int) Math.abs(toGoal)));
            } else {
                textToGoal.setText("-");
            }

            // Updates and saves the new text
            buttonUpdate.setOnClickListener(v -> {
                String newDate = editRowDate.getText().toString().trim();
                String newWeightStr = editRowWeight.getText().toString().trim();

                if (newDate.isEmpty() || newWeightStr.isEmpty()) {
                    Toast.makeText(this, "Date and weight cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                double newWeight;
                try {
                    newWeight = Double.parseDouble(newWeightStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Weight must be a number.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean updated = weightDao.updateWeight(entry.id, newDate, newWeight, entry.goalWeight);

                if (updated) {
                    Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
                    refreshGrid();
                } else {
                    Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                }
            });

            // Delete
            buttonDelete.setOnClickListener(v -> {
                boolean deleted = weightDao.deleteWeight(entry.id);
                if (deleted) {
                    Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                    refreshGrid();
                } else {
                    Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show();
                }
            });

            layoutRows.addView(row);
        }
    }

    private void updateGoalProgress(List<WeightEntry> entries) {

        //this is if no weights have been entered yet
        if (entries == null || entries.isEmpty()) {
            progressWeightGoal.setProgress(0);
            textProgressPercent.setText("0%");
            textPoundsRemaining.setText("-- lbs remaining! Keep Going!");
            return;
        }

        //this is the newest weight
        double currentWeight = entries.get(0).weight;

        //this is the oldest weight
        double startingWeight = entries.get(entries.size() - 1).weight;

        //this is the most recent goal
        Double goalWeight = getLatestGoalWeight(entries);

        //this means that a goal must be set before progress can be calculated
        if (goalWeight == null) {
            progressWeightGoal.setProgress(0);
            textProgressPercent.setText("0%");
            textPoundsRemaining.setText("Please set a goal weight");
            return;
        }

        //this calculates progress using the separate calculator class created
        int progress = WeightProgressCalculator.calculateProgress(
                startingWeight,
                currentWeight,
                goalWeight
        );

        double poundsRemaining =
                WeightProgressCalculator.calculatePoundsRemaining(
                        currentWeight,
                        goalWeight
                );

        //this updates the progress ring and text
        progressWeightGoal.setProgress(progress);
        textProgressPercent.setText(progress + "%");

        textPoundsRemaining.setText(
                String.format("%.1f lbs remaining! Keep Going!", poundsRemaining)
        );
    }

    // Most recent goal from entries list
    private Double getLatestGoalWeight(List<WeightEntry> entries) {
        for (WeightEntry e : entries) {
            if (e.goalWeight != null) return e.goalWeight;
        }
        return null;
    }
}