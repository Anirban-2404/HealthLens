package com.healthlens.service;

import com.healthlens.model.HealthRecord;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HealthAnalyzer {

    public double averageScore(List<HealthRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream().mapToDouble(HealthRecord::calculateScore).average().orElse(0);
    }

    public double averageSleep(List<HealthRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream().mapToDouble(HealthRecord::getSleepHours).average().orElse(0);
    }

    public double averageWater(List<HealthRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream().mapToDouble(HealthRecord::getWaterGlasses).average().orElse(0);
    }

    public double averageExercise(List<HealthRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream().mapToInt(HealthRecord::getExerciseMinutes).average().orElse(0);
    }

    public int totalExercise(List<HealthRecord> records) {
        return records.stream().mapToInt(HealthRecord::getExerciseMinutes).sum();
    }

    public double sevenDayAverage(List<HealthRecord> records) {
        LocalDate cutoff = LocalDate.now().minusDays(6);
        List<HealthRecord> recent = records.stream()
                .filter(r -> !r.getDate().isBefore(cutoff))
                .toList();
        return averageScore(recent);
    }

    public int streak(List<HealthRecord> records) {
        Set<LocalDate> dates = records.stream().map(HealthRecord::getDate).collect(Collectors.toSet());
        if (dates.isEmpty()) return 0;
        LocalDate day = dates.stream().max(Comparator.naturalOrder()).orElse(LocalDate.now());
        int count = 0;
        while (dates.contains(day)) {
            count++;
            day = day.minusDays(1);
        }
        return count;
    }

    public String riskLevel(List<HealthRecord> records) {
        if (records.isEmpty()) return "Not enough data";
        double score = averageScore(records);
        boolean highStress = records.stream().limit(7).anyMatch(r -> "High".equalsIgnoreCase(r.getStress()));
        if (score < 50 || highStress && score < 65) return "Needs attention";
        if (score < 75) return "Watch habits";
        return "Healthy pattern";
    }

    public String coachAdvice(List<HealthRecord> records) {
        if (records.isEmpty()) return "Start with one record today. HealthLens will personalize your advice as data grows.";
        HealthRecord latest = records.stream().max(Comparator.comparing(HealthRecord::getDate)).orElse(records.get(0));
        if (latest.getSleepHours() < 7) return "Coach: Aim for 7–9 hours of sleep tonight.";
        if (latest.getWaterGlasses() < 2) return "Coach: Add another glass or two of water today.";
        if (latest.getExerciseMinutes() < 30) return "Coach: Try a 30-minute walk or another light activity.";
        if ("High".equalsIgnoreCase(latest.getStress())) return "Coach: Add a short relaxation break and reduce stress where possible.";
        return "Coach: Great balance today. Keep the routine consistent!";
    }

    public String challenge(List<HealthRecord> records) {
        if (records.isEmpty()) return "Challenge: Log your first health record today.";
        HealthRecord latest = records.stream().max(Comparator.comparing(HealthRecord::getDate)).orElse(records.get(0));
        if (latest.getExerciseMinutes() < 30) return "Challenge: Reach 30 minutes of activity today.";
        if (latest.getWaterGlasses() < 2) return "Challenge: Reach 2.0 L of water today.";
        if (latest.getSleepHours() < 7) return "Challenge: Protect a 7+ hour sleep window tonight.";
        return "Challenge complete: keep tomorrow's score above today's score.";
    }

    public String achievements(List<HealthRecord> records) {
        if (records.isEmpty()) return "🏁 First Record — waiting for your first entry";
        StringBuilder s = new StringBuilder();
        s.append("✓ First Record\n");
        if (streak(records) >= 3) s.append("✓ 3-Day Streak\n"); else s.append("○ 3-Day Streak\n");
        if (averageScore(records) >= 80) s.append("✓ 80+ Average Score\n"); else s.append("○ 80+ Average Score\n");
        if (totalExercise(records) >= 150) s.append("✓ 150 Exercise Minutes\n"); else s.append("○ 150 Exercise Minutes\n");
        return s.toString().trim();
    }

    public String summary(List<HealthRecord> records) {
        if (records.isEmpty()) return "Add your first health record to see an analysis.";
        double score = averageScore(records);
        if (score >= 80) return "Strong overall wellness pattern. Keep your routine consistent.";
        if (score >= 60) return "Good progress. Focus on one small habit improvement each day.";
        return "Your recent records suggest focusing on sleep, hydration and activity.";
    }
}
