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
import androidx.appcompat.app.AlertDialog;

// Dialog imports which are used for popups
import android.text.InputType;
import android.widget.EditText;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LoggedWeightsActivity extends AppCompatActivity {

    // These match with the UI widgets that are located in activity_logged_weights.xml
    // The IDs MUST exist in that XML file or they will be NULL

    private LinearLayout layoutRows;
    private Button buttonDailyWeight;
    private Button buttonGoalWeight;
    private Button buttonStartingWeight;
    private TextView textStartingWeight;
    private TextView textGoalWeight;
    private ProgressBar progressWeightGoal;
    private TextView textProgressPercent;
    private TextView textPoundsRemaining;
    private TextView textWeeklyChange;
    private TextView textEstimatedGoal;

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
        textWeeklyChange = findViewById(R.id.textWeeklyChange);
        textEstimatedGoal = findViewById(R.id.textEstimatedGoal);
        buttonStartingWeight = findViewById(R.id.buttonStartingWeight);
        textStartingWeight = findViewById(R.id.textStartingWeight);

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

        //this gets the saved starting weight from the database
        Double savedStartingWeight = userDao.getStartingWeight(userId);

        if (savedStartingWeight != null) {
            textStartingWeight.setText(
                    String.format(
                            Locale.US,
                            "Starting Weight: %.1f lbs",
                            savedStartingWeight
                    )
            );
        } else {
            textStartingWeight.setText("Starting Weight: Not entered");
        }



        refreshGrid();

        //Daily Weight: asks for Date + Weight
        buttonDailyWeight.setOnClickListener(v -> showDailyWeightDialog());

        //Starting Weight: asks for starting weight
        buttonStartingWeight.setOnClickListener(v -> showStartingWeightDialog());

        //Goal Weight: asks for Goal only
        buttonGoalWeight.setOnClickListener(v -> showGoalWeightDialog());
    }

    private void showDailyWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Daily Weight");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText editWeight = new EditText(this);
        editWeight.setHint("Weight in lbs");
        editWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(editWeight);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            //this automatically uses today's date
            String date = new SimpleDateFormat(
                    "MM/dd",
                    Locale.US
            ).format(new Date());

            String weightStr = editWeight.getText().toString().trim();

            if (weightStr.isEmpty()) {
                Toast.makeText(this, "Please enter a weight.", Toast.LENGTH_SHORT).show();
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

            //this helps prevent more than one daily weight from being entered on the same date
            if (hasWeightForDate(entries, date)) {
                Toast.makeText(
                        this,
                        "A weight has already been entered for today!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Double currentGoal = getLatestGoalWeight(entries);

            boolean added = weightDao.addWeight(userId, date, weightValue, currentGoal);

            if (added) {
                Toast.makeText(this, "Daily weight saved!", Toast.LENGTH_SHORT).show();

                checkForMilestone(weightValue, date);

                refreshGrid();
            } else {
                Toast.makeText(this, "Save failed.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    //this shows a popup for the user to set their starting weight
    private void showStartingWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Starting Weight");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText editStartingWeight = new EditText(this);
        editStartingWeight.setHint("Starting Weight in lbs");
        editStartingWeight.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        container.addView(editStartingWeight);
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {

            String startingWeightStr =
                    editStartingWeight.getText().toString().trim();

            if (startingWeightStr.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please enter your starting weight.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            double startingWeight;

            try {
                startingWeight = Double.parseDouble(startingWeightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(
                        this,
                        "Starting weight must be a number.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            boolean saved =
                    userDao.setStartingWeight(userId, startingWeight);

            if (saved) {
                Toast.makeText(
                        this,
                        "Current weight saved!",
                        Toast.LENGTH_SHORT
                ).show();

                textStartingWeight.setText(
                        String.format(
                                Locale.US,
                                "Starting Weight: %.1f lbs",
                                startingWeight
                        )
                );

            } else {
                Toast.makeText(
                        this,
                        "Starting weight could not be saved.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        builder.setNegativeButton(
                "Cancel",
                (dialog, which) -> dialog.dismiss()
        );

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

        // Goal input
        EditText editGoal = new EditText(this);
        editGoal.setHint("Goal Weight in lbs");
        editGoal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(editGoal);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            //this automatically uses today's date
            String date = new SimpleDateFormat(
                    "MM/dd",
                    Locale.US
            ).format(new Date());

            String goalStr = editGoal.getText().toString().trim();
            if (goalStr.isEmpty()) {
                Toast.makeText(this, "Please enter a goal weight.", Toast.LENGTH_SHORT).show();
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

        //this calculates and displays the average weekly weight change
        double weeklyChange = calculateAverageWeeklyChange(entries);

        if (entries.size() < 2 || weeklyChange == 0.0) {
            textWeeklyChange.setText("Average Weekly Change: Needs More Weights Entered");
        } else {
            textWeeklyChange.setText(
                    String.format(
                            Locale.US,
                            "Average Weekly Change: %.1f lbs/week",
                            weeklyChange
                    )
            );
        }

        //this calculates and displays the estimated time to reach the goal
        Double latestGoal = getLatestGoalWeight(entries);

        if (latestGoal == null) {
            textEstimatedGoal.setText("Estimated Time to Goal: Please set a goal!");
        } else {
            double estimatedWeeks =
                    calculateEstimatedWeeksToGoal(entries, latestGoal);

            if (estimatedWeeks == 0) {
                textEstimatedGoal.setText("Estimated Time to Goal: Goal reached!");
            } else if (estimatedWeeks < 0) {
                textEstimatedGoal.setText(
                        "Estimated Time to Goal: Need More Weights Entered"
                );
            } else {
                textEstimatedGoal.setText(
                        String.format(
                                Locale.US,
                                "Estimated Time to Goal: %.1f weeks",
                                estimatedWeeks
                        )
                );
            }
        }

        // Displays the goal weight at the top of the screen
        String goalDisplay = "Goal Weight: Not set";
        latestGoal = getLatestGoalWeight(entries);
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

        //this gets the user's starting weight from the database
        Double startingWeight = userDao.getStartingWeight(userId);

        if (startingWeight == null) {
            progressWeightGoal.setProgress(0);
            textProgressPercent.setText("0%");
            textPoundsRemaining.setText("Please set a starting weight");
            return;
        }

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

    //this sorts weight entries by date for trend calculations
    private List<WeightEntry> sortWeightsByDate(List<WeightEntry> entries) {

        //this makes a copy so the original database list is not changed
        List<WeightEntry> sortedEntries = new ArrayList<>();

        //this only adds actual weight entries to the trend calculation
        for (WeightEntry entry : entries) {
            if (entry.weight > 0) {
                sortedEntries.add(entry);
            }
        }

        //this will sort entries by their MM/DD date
        Collections.sort(sortedEntries, new Comparator<WeightEntry>() {
            @Override
            public int compare(WeightEntry first, WeightEntry second) {
                return first.date.compareTo(second.date);
            }
        });

        return sortedEntries;
    }

    //this calculates the average weekly weight change
    private double calculateAverageWeeklyChange(List<WeightEntry> entries) {

        //this just means that there must be at least two weights to calculate a trend
        if (entries == null || entries.size() < 2) {
            return 0.0;
        }

        //this sorts the entries from oldest to newest
        List<WeightEntry> sortedEntries = sortWeightsByDate(entries);

        //this means there has to be at least two actual weight entries
        if (sortedEntries.size() < 2) {
            return 0.0;
        }

        WeightEntry oldestEntry = sortedEntries.get(0);
        WeightEntry newestEntry = sortedEntries.get(sortedEntries.size() - 1);

        try {
            //this converts the MM/DD strings into dates
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("MM/dd", Locale.US);

            Date oldestDate = dateFormat.parse(oldestEntry.date);
            Date newestDate = dateFormat.parse(newestEntry.date);

            if (oldestDate == null || newestDate == null) {
                return 0.0;
            }

            //this finds the number of days between the two entries
            long difference =
                    newestDate.getTime() - oldestDate.getTime();

            long days =
                    TimeUnit.MILLISECONDS.toDays(difference);

            //this prevents division by zero
            if (days <= 0) {
                return 0.0;
            }

            //this calculates the total weight change
            double weightChange =
                    newestEntry.weight - oldestEntry.weight;

            //this converts days into weeks
            double weeks = days / 7.0;

            //this returns the average change per week
            return weightChange / weeks;

        } catch (ParseException e) {
            return 0.0;
        }
    }

    //this estimates how many weeks it may take to reach the goal weight
    private double calculateEstimatedWeeksToGoal(
            List<WeightEntry> entries,
            double goalWeight) {

        //this means that there must be weight data to calculate an estimate
        if (entries == null || entries.size() < 2) {
            return -1;
        }

        //this sorts entries from oldest to newest
        List<WeightEntry> sortedEntries = sortWeightsByDate(entries);

        //this just means that there must be at least two actual weights to estimate the trend
        if (sortedEntries.size() < 2) {
            return -1;
        }

        //this gets the most recent weight
        WeightEntry newestEntry =
                sortedEntries.get(sortedEntries.size() - 1);

        double currentWeight = newestEntry.weight;

        //this gets the average weekly change from the trend algorithm
        double weeklyChange =
                calculateAverageWeeklyChange(entries);

        //this means that the user has already reached or passed the goal
        if (currentWeight <= goalWeight) {
            return 0;
        }

        // means a zero or positive means the current trend is not moving towards a lower goal weight
        if (weeklyChange >= 0) {
            return -1;
        }

        //this calculates how many pounds remain
        double poundsRemaining =
                currentWeight - goalWeight;

        return poundsRemaining / Math.abs(weeklyChange);
    }

    //this checks if a daily weight has already been entered for today's date
    private boolean hasWeightForDate(List<WeightEntry> entries, String date) {

        for (WeightEntry entry : entries) {

            //this means that weight must be greater than 0 because goal-only entries
            //should not count as daily weight entries
            if (entry.date.equals(date) && entry.weight > 0) {
                return true;
            }
        }

        return false;
    }

    //this checks whether the user has reached a new 5-pound weight-loss milestone
    private void checkForMilestone(double currentWeight, String date) {

        //this gets the user's starting weight from the database
        Double startingWeight = userDao.getStartingWeight(userId);

        //this means that a starting weight must be set before milestones can be calculated
        if (startingWeight == null) {
            return;
        }

        //this calculates total pounds lost
        double poundsLost = startingWeight - currentWeight;

        //this means the user has not reached the first 5-pound milestone yet
        if (poundsLost < 5) {
            return;
        }

        //this finds the highest 5-pound milestone reached
        double milestoneWeight =
                Math.floor(poundsLost / 5.0) * 5.0;

        //this gets milestones already stored for this user
        List<Double> savedMilestones =
                weightDao.getMilestones(userId);

        //this prevents the same milestone from being stored twice
        if (!savedMilestones.contains(milestoneWeight)) {

            boolean milestoneSaved =
                    weightDao.addMilestone(
                            userId,
                            milestoneWeight,
                            date
                    );

            if (milestoneSaved) {

                // Loads the custom purple milestone popup
                View milestoneView = getLayoutInflater().inflate(
                        R.layout.dialog_milestone,
                        null
                );

                TextView milestoneMessage =
                        milestoneView.findViewById(R.id.textMilestoneMessage);

                Button closeButton =
                        milestoneView.findViewById(R.id.buttonCloseMilestone);

                milestoneMessage.setText(
                        "You've lost " + (int) milestoneWeight + " lbs!"
                );

                AlertDialog milestoneDialog =
                        new AlertDialog.Builder(this)
                                .setView(milestoneView)
                                .setCancelable(false)
                                .create();

                closeButton.setOnClickListener(v ->
                        milestoneDialog.dismiss()
                );

                milestoneDialog.show();
            }
        }
    }

    // Most recent goal from entries list
    private Double getLatestGoalWeight(List<WeightEntry> entries) {
        for (WeightEntry e : entries) {
            if (e.goalWeight != null) return e.goalWeight;
        }
        return null;
    }
}