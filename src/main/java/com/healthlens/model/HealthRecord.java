package com.healthlens.model;

import java.time.LocalDate;

public class HealthRecord extends HealthRecordBase implements HealthMetric {
    private double sleepHours;
    private double waterGlasses;
    private int exerciseMinutes;
    private String mood;
    private String stress;

    public HealthRecord(int id, int userId, LocalDate date, double sleepHours,
                        double waterGlasses, int exerciseMinutes, String mood, String stress) {
        super(id, userId, date);
        this.sleepHours = sleepHours;
        this.waterGlasses = waterGlasses;
        this.exerciseMinutes = exerciseMinutes;
        this.mood = mood;
        this.stress = stress;
    }

    @Override
    public double calculateScore() {
        double sleepScore = Math.min(sleepHours / 8.0, 1.0) * 30;
        double waterScore = Math.min(waterGlasses / 2.0, 1.0) * 20;
        double exerciseScore = Math.min(exerciseMinutes / 30.0, 1.0) * 20;
        double moodScore = switch (mood.toLowerCase()) {
            case "excellent" -> 20;
            case "good" -> 16;
            case "okay" -> 12;
            case "bad" -> 6;
            default -> 10;
        };
        double stressPenalty = switch (stress.toLowerCase()) {
            case "low" -> 0;
            case "medium" -> 5;
            case "high" -> 12;
            default -> 5;
        };
        return Math.max(0, Math.min(100, sleepScore + waterScore + exerciseScore + moodScore - stressPenalty));
    }

    @Override
    public String getDescription() {
        return "Sleep " + sleepHours + "h, Water " + waterGlasses +
                " L, Exercise " + exerciseMinutes + " min";
    }

    public double getSleepHours() { return sleepHours; }
    public double getWaterGlasses() { return waterGlasses; }
    public int getExerciseMinutes() { return exerciseMinutes; }
    public String getMood() { return mood; }
    public String getStress() { return stress; }
}
