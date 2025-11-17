package com.icu;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    // EDIT these to match your MySQL credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/icu?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root"; // put your password here

    private Connection conn;

    public DBManager() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }

    // Add a patient
    public void addPatient(Patient p) throws SQLException {
        String sql = "INSERT INTO patients (name, age, condition_priority, bed_allocated) VALUES (?, ?, ?, false)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setInt(3, p.getConditionPriority());
            ps.executeUpdate();
        }
    }

    // Get waiting patients (not allocated) ordered by priority asc (1 highest)
    public List<Patient> getWaitingPatients() throws SQLException {
        String sql = "SELECT id, name, age, condition_priority, bed_allocated, allocated_bed_id FROM patients WHERE bed_allocated = false ORDER BY condition_priority ASC, id ASC";
        List<Patient> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Patient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getInt("condition_priority"),
                        rs.getBoolean("bed_allocated"),
                        (Integer) rs.getObject("allocated_bed_id")
                ));
            }
        }
        return list;
    }

    public List<String> getAllocatedInfo() throws SQLException {
        String sql = "SELECT p.name, p.condition_priority, b.bed_number FROM patients p JOIN beds b ON p.allocated_bed_id = b.id WHERE p.bed_allocated = true";
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("name") + " (P=" + rs.getInt("condition_priority") + ") -> " + rs.getString("bed_number"));
            }
        }
        return list;
    }

    // Get available beds
    public List<Bed> getAvailableBeds() throws SQLException {
        String sql = "SELECT id, bed_number, is_occupied FROM beds WHERE is_occupied = false";
        List<Bed> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Bed(rs.getInt("id"), rs.getString("bed_number"), rs.getBoolean("is_occupied")));
            }
        }
        return list;
    }

    // Allocate the highest-priority waiting patient to the first free bed (transactional)
    public String allocateNextPatient() throws SQLException {
        // Transactional: lock both tables briefly
        conn.setAutoCommit(false);
        Savepoint sp = null;
        try {
            sp = conn.setSavepoint();

            // 1) find highest priority waiting patient (FOR UPDATE to lock row)
            String pickPatientSql = "SELECT id, name, condition_priority FROM patients WHERE bed_allocated = false ORDER BY condition_priority ASC, id ASC LIMIT 1 FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(pickPatientSql);
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback(sp);
                    conn.setAutoCommit(true);
                    return "No patients waiting.";
                }
                int pid = rs.getInt("id");
                String pname = rs.getString("name");
                int ppriority = rs.getInt("condition_priority");

                // 2) find first available bed (FOR UPDATE to lock row)
                String pickBedSql = "SELECT id, bed_number FROM beds WHERE is_occupied = false LIMIT 1 FOR UPDATE";
                try (PreparedStatement ps2 = conn.prepareStatement(pickBedSql);
                     ResultSet rs2 = ps2.executeQuery()) {
                    if (!rs2.next()) {
                        conn.rollback(sp);
                        conn.setAutoCommit(true);
                        return "No available beds right now for " + pname;
                    }

                    int bid = rs2.getInt("id");
                    String bedNum = rs2.getString("bed_number");

                    // 3) update bed -> occupied
                    String updateBed = "UPDATE beds SET is_occupied = true WHERE id = ?";
                    try (PreparedStatement ps3 = conn.prepareStatement(updateBed)) {
                        ps3.setInt(1, bid);
                        ps3.executeUpdate();
                    }

                    // 4) update patient -> bed_allocated true and set allocated_bed_id
                    String updatePatient = "UPDATE patients SET bed_allocated = true, allocated_bed_id = ? WHERE id = ?";
                    try (PreparedStatement ps4 = conn.prepareStatement(updatePatient)) {
                        ps4.setInt(1, bid);
                        ps4.setInt(2, pid);
                        ps4.executeUpdate();
                    }

                    conn.commit();
                    conn.setAutoCommit(true);
                    return "Allocated " + pname + " (P=" + ppriority + ") to " + bedNum;
                }
            }
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback(sp);
                } catch (SQLException ignored) {}
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            throw ex;
        }
    }
}
