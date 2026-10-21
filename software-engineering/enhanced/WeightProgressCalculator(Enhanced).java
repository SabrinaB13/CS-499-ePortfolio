package com.example.watchtheweightfalloff;

public class WeightProgressCalculator {

    //this calculates how many pounds are left until the goal is reached
    public static double calculatePoundsRemaining(
            double currentWeight,
            double goalWeight) {

        double remaining = currentWeight - goalWeight;

        //this prevents negative pounds if the user passes their goal weight
        return Math.max(remaining, 0);
    }

    //this calculates the percentage of the goal weight that has been completed
    public static int calculateProgress(
            double startingWeight,
            double currentWeight,
            double goalWeight) {

        double totalToLose = startingWeight - goalWeight;

        //this prevents any invalid calculations
        if (totalToLose <= 0) {
            return 0;
        }

        double weightLost = startingWeight - currentWeight;

        double progress = (weightLost / totalToLose) * 100;

        //this keeps progress between 0% and 100%
        progress = Math.max(0, Math.min(progress, 100));

        return (int) Math.round(progress);
    }
}