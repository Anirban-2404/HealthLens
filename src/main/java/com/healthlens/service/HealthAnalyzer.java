package com.healthlens.service;

import com.healthlens.model.HealthRecord;

import java.util.List;

public class HealthAnalyzer {

    public double averageScore(List<HealthRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream().mapToDouble(HealthRecord::calculateScore).average().orElse(0);
    }

    public double averageSleep(List<HealthRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream().mapToDouble(HealthRecord::getSleepHours).average().orElse(0);
    }

    public int totalExercise(List<HealthRecord> records) {
        return records.stream().mapToInt(HealthRecord::getExerciseMinutes).sum();
    }

    public String summary(List<HealthRecord> records) {
        if (records.isEmpty()) return "Add your first health record to see an analysis.";
        double score = averageScore(records);
        if (score >= 80) return "Great overall wellness pattern.";
        if (score >= 60) return "Good progress. A few habits can be improved.";
        return "Your recent records suggest focusing on sleep, hydration and activity.";
    }
}
