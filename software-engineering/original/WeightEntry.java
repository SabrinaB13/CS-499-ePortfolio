package com.example.watchtheweightfalloff;

public class WeightEntry {
    public int id;
    public String date;
    public double weight;
    public Double goalWeight; // can be null

    public WeightEntry(int id, String date, double weight, Double goalWeight) {
        this.id = id;
        this.date = date;
        this.weight = weight;
        this.goalWeight = goalWeight;
    }
}
