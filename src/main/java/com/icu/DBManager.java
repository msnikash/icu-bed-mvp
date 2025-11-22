package com.icu;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bed?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    private Connection conn;

    public DBManager() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet cols = md.getColumns(null, null, "patients", "allocated_days")) {
                if (!cols.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE patients ADD COLUMN allocated_days INT NULL");
                    }
                }
            }

            try (ResultSet cols = md.getColumns(null, null, "patients", "start_date")) {
                if (!cols.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE patients ADD COLUMN start_date DATE NULL");
                    }
                }
            }
            try (ResultSet cols = md.getColumns(null, null, "patients", "end_date")) {
                if (!cols.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE patients ADD COLUMN end_date DATE NULL");
                    }
                }
            }
            try (ResultSet cols = md.getColumns(null, null, "patients", "allocated_on")) {
                if (!cols.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE patients ADD COLUMN allocated_on DATE NULL");
                    }
                }
            }

            try (ResultSet cols2 = md.getColumns(null, null, "patients", "condition_priority")) {
                if (cols2.next()) {
                    String typeName = cols2.getString("TYPE_NAME");
                    if (typeName != null) {
                        String t = typeName.toUpperCase();
                        if (!(t.contains("CHAR") || t.contains("TEXT"))) {
                            try (Statement st = conn.createStatement()) {
                                st.executeUpdate("ALTER TABLE patients MODIFY condition_priority VARCHAR(10)");
                            } catch (SQLException ignore) {
                            }
                        }
                    }
                }
            }

            try (ResultSet cols3 = md.getColumns(null, null, "patients", "discharged")) {
                if (!cols3.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE patients ADD COLUMN discharged BOOLEAN DEFAULT FALSE");
                    } catch (SQLException ignore) {
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Schema check/upgrade failed: " + e.getMessage());
        }
    }

    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }

    public void addPatient(Patient p) throws SQLException {
        String sql = "INSERT INTO patients (name, age, condition_priority, bed_allocated, allocated_days, start_date, end_date) VALUES (?, ?, ?, false, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getConditionPriority());
            ps.setInt(4, p.getAllocatedDays() == null ? 0 : p.getAllocatedDays());
            if (p.getStartDate() != null) ps.setDate(5, java.sql.Date.valueOf(p.getStartDate())); else ps.setNull(5, Types.DATE);
            if (p.getEndDate() != null) ps.setDate(6, java.sql.Date.valueOf(p.getEndDate())); else ps.setNull(6, Types.DATE);
            ps.executeUpdate();
        }
    }

    public List<Patient> getWaitingPatients() throws SQLException {
        String sql = "SELECT id, name, age, condition_priority, bed_allocated, allocated_bed_id, allocated_days, start_date, end_date FROM patients WHERE bed_allocated = false AND (discharged IS NULL OR discharged = false) ORDER BY FIELD(condition_priority,'High','Medium','Low'), id ASC";
        List<Patient> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date sd = rs.getDate("start_date");
                java.sql.Date ed = rs.getDate("end_date");
                java.time.LocalDate sdLocal = sd == null ? null : sd.toLocalDate();
                java.time.LocalDate edLocal = ed == null ? null : ed.toLocalDate();
                Patient p = new Patient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("condition_priority"),
                        rs.getBoolean("bed_allocated"),
                        (Integer) rs.getObject("allocated_bed_id"),
                        (Integer) rs.getObject("allocated_days"),
                        sdLocal,
                        edLocal
                );
                list.add(p);
            }
        }
        return list;
    }

    public List<AllocatedEntry> getAllocatedInfo() throws SQLException {
        String sql = "SELECT p.id AS pid, p.name, p.age AS age, p.condition_priority, p.allocated_days, p.allocated_on, p.end_date, b.id AS bid, b.bed_number FROM patients p JOIN beds b ON p.allocated_bed_id = b.id WHERE p.bed_allocated = true";
        List<AllocatedEntry> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
            Integer days = (Integer) rs.getObject("allocated_days");
            java.sql.Date allocatedOn = rs.getDate("allocated_on");
            java.sql.Date endDate = rs.getDate("end_date");
            Integer age = (Integer) rs.getObject("age");
            list.add(new AllocatedEntry(
                rs.getInt("pid"),
                rs.getString("name"),
                age,
                rs.getString("condition_priority"),
                days,
                rs.getInt("bid"),
                rs.getString("bed_number"),
                allocatedOn == null ? null : allocatedOn.toLocalDate(),
                endDate == null ? null : endDate.toLocalDate()
            ));
            }
        }
        return list;
    }

    public String allocateSpecificPatient(int patientId) throws SQLException {
        conn.setAutoCommit(false);
        Savepoint sp = null;
        try {
            sp = conn.setSavepoint();

            String pickPatientSql = "SELECT id, name, condition_priority FROM patients WHERE id = ? AND bed_allocated = false FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(pickPatientSql)) {
                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback(sp);
                        conn.setAutoCommit(true);
                        return "Patient not found or already allocated.";
                    }
                    int pid = rs.getInt("id");
                    String pname = rs.getString("name");
                    String ppriority = rs.getString("condition_priority");

                    int bid = -1;
                    String bedNum = null;
                    String preferredSql = (ppriority != null && ppriority.equalsIgnoreCase("High"))
                            ? "SELECT id, bed_number FROM beds WHERE is_occupied = false AND bed_number LIKE 'BED-%' LIMIT 1 FOR UPDATE"
                            : "SELECT id, bed_number FROM beds WHERE is_occupied = false AND bed_number NOT LIKE 'BED-%' LIMIT 1 FOR UPDATE";

                    try (PreparedStatement ps2 = conn.prepareStatement(preferredSql);
                         ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            bid = rs2.getInt("id");
                            bedNum = rs2.getString("bed_number");
                        }
                    }

                    if (bid == -1) {
                        String fallbackSql = "SELECT id, bed_number FROM beds WHERE is_occupied = false LIMIT 1 FOR UPDATE";
                        try (PreparedStatement ps3 = conn.prepareStatement(fallbackSql);
                             ResultSet rs3 = ps3.executeQuery()) {
                            if (rs3.next()) {
                                bid = rs3.getInt("id");
                                bedNum = rs3.getString("bed_number");
                            } else {
                                conn.rollback(sp);
                                conn.setAutoCommit(true);
                                return "No available beds right now for " + pname;
                            }
                        }
                    }

                    String updateBed = "UPDATE beds SET is_occupied = true WHERE id = ?";
                    try (PreparedStatement ps3 = conn.prepareStatement(updateBed)) {
                        ps3.setInt(1, bid);
                        ps3.executeUpdate();
                    }

                    String updatePatient = "UPDATE patients SET bed_allocated = true, allocated_bed_id = ?, allocated_on = CURRENT_DATE WHERE id = ?";
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
                try { conn.rollback(sp); } catch (SQLException ignored) {}
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            throw ex;
        }
    }

    public String freeBedById(int bedId) throws SQLException {
        conn.setAutoCommit(false);
        Savepoint sp = null;
        try {
            sp = conn.setSavepoint();

            String findPatient = "SELECT id, name FROM patients WHERE allocated_bed_id = ? FOR UPDATE";
            Integer pid = null;
            String pname = null;
            try (PreparedStatement ps = conn.prepareStatement(findPatient)) {
                ps.setInt(1, bedId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pid = rs.getInt("id");
                        pname = rs.getString("name");
                    }
                }
            }

            String updateBed = "UPDATE beds SET is_occupied = false WHERE id = ?";
            try (PreparedStatement ps2 = conn.prepareStatement(updateBed)) {
                ps2.setInt(1, bedId);
                ps2.executeUpdate();
            }

            if (pid != null) {
                String deletePatient = "DELETE FROM patients WHERE id = ?";
                try (PreparedStatement ps3 = conn.prepareStatement(deletePatient)) {
                    ps3.setInt(1, pid);
                    ps3.executeUpdate();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            if (pid != null) {
                return "Freed bed (id=" + bedId + ") previously allocated to " + pname;
            } else {
                return "Freed bed (id=" + bedId + ")";
            }
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(sp); } catch (SQLException ignored) {}
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            throw ex;
        }
    }

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

    public String allocateNextPatient() throws SQLException {
        conn.setAutoCommit(false);
        Savepoint sp = null;
        try {
            sp = conn.setSavepoint();

            String pickPatientSql = "SELECT id, name, condition_priority FROM patients WHERE bed_allocated = false ORDER BY FIELD(condition_priority,'High','Medium','Low'), id ASC LIMIT 1 FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(pickPatientSql);
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback(sp);
                    conn.setAutoCommit(true);
                    return "No patients waiting.";
                }
                int pid = rs.getInt("id");
                String pname = rs.getString("name");
                String ppriority = rs.getString("condition_priority");

                int bid = -1;
                String bedNum = null;
                String preferredSql = (ppriority != null && ppriority.equalsIgnoreCase("High"))
                    ? "SELECT id, bed_number FROM beds WHERE is_occupied = false AND bed_number LIKE 'BED-%' LIMIT 1 FOR UPDATE"
                    : "SELECT id, bed_number FROM beds WHERE is_occupied = false AND bed_number NOT LIKE 'BED-%' LIMIT 1 FOR UPDATE";

                try (PreparedStatement ps2 = conn.prepareStatement(preferredSql);
                     ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        bid = rs2.getInt("id");
                        bedNum = rs2.getString("bed_number");
                    }
                }

                if (bid == -1) {
                    String fallbackSql = "SELECT id, bed_number FROM beds WHERE is_occupied = false LIMIT 1 FOR UPDATE";
                    try (PreparedStatement ps3 = conn.prepareStatement(fallbackSql);
                         ResultSet rs3 = ps3.executeQuery()) {
                        if (rs3.next()) {
                            bid = rs3.getInt("id");
                            bedNum = rs3.getString("bed_number");
                        } else {
                            conn.rollback(sp);
                            conn.setAutoCommit(true);
                            return "No available beds right now for " + pname;
                        }
                    }
                }

                String updateBed = "UPDATE beds SET is_occupied = true WHERE id = ?";
                try (PreparedStatement ps3 = conn.prepareStatement(updateBed)) {
                    ps3.setInt(1, bid);
                    ps3.executeUpdate();
                }

                String updatePatient = "UPDATE patients SET bed_allocated = true, allocated_bed_id = ?, allocated_on = CURRENT_DATE WHERE id = ?";
                try (PreparedStatement ps4 = conn.prepareStatement(updatePatient)) {
                    ps4.setInt(1, bid);
                    ps4.setInt(2, pid);
                    ps4.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(true);
                return "Allocated " + pname + " (P=" + ppriority + ") to " + bedNum;
            }
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(sp); } catch (SQLException ignored) {}
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            throw ex;
        }
    }
}
