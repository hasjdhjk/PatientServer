package DataAccessObject;

import DataBase.DatabaseConnection;
import Models.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public static List<Patient> getPatientsForDoctor(String doctor) {
        List<Patient> patients = new ArrayList<>();

        String sql = "SELECT id, givenname, familyname, heartrate, temperature, bp " +
                "FROM patients WHERE doctor = ? ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Patient p = new Patient(
                            rs.getInt("id"),
                            rs.getString("givenname"),
                            rs.getString("familyname"),
                            rs.getInt("heartrate"),
                            rs.getDouble("temperature"),
                            rs.getString("bp")
                    );
                    patients.add(p);
                }
            }

            return patients;

        } catch (SQLException e) {
            throw new RuntimeException("getPatientsForDoctor SQL failed: " + e.getMessage(), e);
        }
    }

    public static Patient getPatientByIdForDoctor(String doctor, int id) {
        String sql = "SELECT id, givenname, familyname, heartrate, temperature, bp " +
                "FROM patients WHERE doctor = ? AND id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctor);
            ps.setInt(2, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new Patient(
                        rs.getInt("id"),
                        rs.getString("givenname"),
                        rs.getString("familyname"),
                        rs.getInt("heartrate"),
                        rs.getDouble("temperature"),
                        rs.getString("bp")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("getPatientByIdForDoctor SQL failed: " + e.getMessage(), e);
        }
    }

    /**
     * Insert patient for doctor, return new id (DB generated).
     */
    public static int insertPatientForDoctor(String doctor, Patient p) throws SQLException {
        String sql = "INSERT INTO patients (doctor, givenname, familyname, heartrate, temperature, bp) " +
                "VALUES (?,?,?,?,?,?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctor);
            ps.setString(2, p.getGivenName());
            ps.setString(3, p.getFamilyName());
            ps.setInt(4, p.getHeartRate());
            ps.setDouble(5, p.getTemperature());
            ps.setString(6, p.getBloodPressure());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        }
    }
    public static boolean deletePatientForDoctor(String doctor, int id) throws Exception {
        String sql = "DELETE FROM patients WHERE id = ? AND doctor = ?";

        try (java.sql.Connection conn = DataBase.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setString(2, doctor);

            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }
}