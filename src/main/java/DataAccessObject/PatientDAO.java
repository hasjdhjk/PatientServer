package DataAccessObject;

import DataBase.DatabaseConnection;
import Models.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
// Data Access Object for Patient-related database operations
public class PatientDAO {

    // Retrieve all patients belonging to a specific doctor
    public static List<Patient> getPatientsForDoctor(String doctor) {
        List<Patient> patients = new ArrayList<>();

        // SQL query to fetch patient fields
        String sql = "SELECT id, " +
                "given_name AS givenname, " +
                "family_name AS familyname, " +
                "gender, age, " +
                "blood_pressure AS bp " +
                "FROM patients WHERE doctor = ? ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind doctor parameter
            ps.setString(1, doctor);

            // Execute query and map results
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Patient p = new Patient(
                            rs.getInt("id"),
                            rs.getString("givenname"),
                            rs.getString("familyname"),
                            rs.getString("gender"),
                            rs.getInt("age"),
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
    // Retrieve a single patient by ID for a specific doctor
    public static Patient getPatientByIdForDoctor(String doctor, int id) {
        // SQL query to fetch one patient
        String sql = "SELECT id, " +
                "given_name AS givenname, " +
                "family_name AS familyname, " +
                "gender, age, " +
                "blood_pressure AS bp " +
                "FROM patients WHERE doctor = ? AND id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind parameters
            ps.setString(1, doctor);
            ps.setInt(2, id);

            // Execute query
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new Patient(
                        rs.getInt("id"),
                        rs.getString("givenname"),
                        rs.getString("familyname"),
                        rs.getString("gender"),
                        rs.getInt("age"),
                        rs.getString("bp")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("getPatientByIdForDoctor SQL failed: " + e.getMessage(), e);
        }
    }

    // Insert a new patient for a doctor and return generated ID
    public static int insertPatientForDoctor(String doctor, Patient p) throws SQLException {

        // SQL insert with returning ID
        String sql = "INSERT INTO patients (doctor, given_name, family_name, gender, age, blood_pressure) " +
                "VALUES (?,?,?,?,?,?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind patient fields
            ps.setString(1, doctor);
            ps.setString(2, p.getGivenName());
            ps.setString(3, p.getFamilyName());
            ps.setString(4, p.getGender());
            ps.setInt(5, p.getAge());
            ps.setString(6, p.getBloodPressure());

            // Execute insert and read generated ID
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        }
    }

    // Delete all patients for a given doctor
    public static int deletePatientsByDoctor(String doctor) {
        if (doctor == null) doctor = "";

        // SQL delete query
        String sql = "DELETE FROM patients WHERE doctor = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind doctor parameter
            ps.setString(1, doctor);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("deletePatientsByDoctor SQL failed: " + e.getMessage(), e);
        }
    }

    // Clear all demo doctor patients
    public static int clearDemoPatients() {
        return deletePatientsByDoctor("demo");
    }

    // Delete a specific patient for a specific doctor
    public static boolean deletePatientForDoctor(String doctor, int id) throws Exception {

        // SQL delete query
        String sql = "DELETE FROM patients WHERE id = ? AND doctor = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind parameters
            ps.setInt(1, id);
            ps.setString(2, doctor);

            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }
}