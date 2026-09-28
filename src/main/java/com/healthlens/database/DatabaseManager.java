package com.healthlens.database;

import com.healthlens.model.HealthRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:healthlens.db";
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public void initialize() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
            """;

        String records = """
            CREATE TABLE IF NOT EXISTS health_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                record_date TEXT NOT NULL,
                sleep_hours REAL NOT NULL,
                water_glasses INTEGER NOT NULL,
                exercise_minutes INTEGER NOT NULL,
                mood TEXT NOT NULL,
                stress TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

        try (Connection c = connect();
             Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute(users);
            st.execute(records);

            String check = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement ps = c.prepareStatement(check)) {
                ps.setString(1, "demo@healthlens.com");
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO users(name,email,password) VALUES(?,?,?)")) {
                        insert.setString(1, "Demo User");
                        insert.setString(2, "demo@healthlens.com");
                        insert.setString(3, "1234");
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    public int login(String email, String password) {
        String sql = "SELECT id FROM users WHERE email = ? AND password = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        } catch (SQLException e) {
            return -1;
        }
    }

    public void insertRecord(HealthRecord r) {
        String sql = """
            INSERT INTO health_records
            (user_id, record_date, sleep_hours, water_glasses, exercise_minutes, mood, stress)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getUserId());
            ps.setString(2, r.getDate().toString());
            ps.setDouble(3, r.getSleepHours());
            ps.setDouble(4, r.getWaterGlasses());
            ps.setInt(5, r.getExerciseMinutes());
            ps.setString(6, r.getMood());
            ps.setString(7, r.getStress());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<HealthRecord> getRecords(int userId) {
        List<HealthRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM health_records WHERE user_id = ? ORDER BY record_date DESC";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new HealthRecord(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        LocalDate.parse(rs.getString("record_date")),
                        rs.getDouble("sleep_hours"),
                        rs.getInt("water_glasses"),
                        rs.getInt("exercise_minutes"),
                        rs.getString("mood"),
                        rs.getString("stress")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public void updateRecord(HealthRecord r) {
        String sql = """
            UPDATE health_records SET record_date=?, sleep_hours=?, water_glasses=?,
            exercise_minutes=?, mood=?, stress=? WHERE id=? AND user_id=?
            """;
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getDate().toString());
            ps.setDouble(2, r.getSleepHours());
            ps.setDouble(3, r.getWaterGlasses());
            ps.setInt(4, r.getExerciseMinutes());
            ps.setString(5, r.getMood());
            ps.setString(6, r.getStress());
            ps.setInt(7, r.getId());
            ps.setInt(8, r.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRecord(int id, int userId) {
        String sql = "DELETE FROM health_records WHERE id=? AND user_id=?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
