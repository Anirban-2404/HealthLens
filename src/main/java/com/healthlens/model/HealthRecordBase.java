package com.healthlens.model;

import java.time.LocalDate;

public abstract class HealthRecordBase {
    protected int id;
    protected int userId;
    protected LocalDate date;

    public HealthRecordBase(int id, int userId, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.date = date;
    }

    public abstract double calculateScore();

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public LocalDate getDate() { return date; }
}
