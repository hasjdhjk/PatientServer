package DataAccessObject;

import DataBase.DatabaseConnection;
import Models.Doctor;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Data Access Object for Doctor-related database operations
public class DoctorDAO {

    // Find doctor by email
    public static Doctor findByEmail(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors WHERE email = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
 
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("findByEmail failed: " + e.getMessage(), e);
        }
    }

    // Find doctor by email verification token
    public static Doctor findByVerificationToken(String token) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors WHERE verification_token = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("findByVerificationToken failed: " + e.getMessage(), e);
        }
    }

    // Find doctor by password reset token
    public static Doctor findByResetToken(String token) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors WHERE reset_token = ? " +
                    "AND reset_token_expires > now()";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("findByResetToken failed: " + e.getMessage(), e);
        }
    }

    // Insert a new doctor and return generated ID
    public static int insertDoctor(String email, String givenName,
                                  String familyName, String passwordHash,
                                  String verificationToken) {
        String sql = "INSERT INTO doctors " +
                "(email, given_name, family_name, password_hash, verified, verification_token) " +
                "VALUES (?,?,?,?,false,?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, email);
            ps.setString(2, givenName);
            ps.setString(3, familyName);
            ps.setString(4, passwordHash);
            ps.setString(5, verificationToken);

            int affected = ps.executeUpdate();
            if (affected != 1) {
                throw new RuntimeException("insertDoctor failed: affected rows=" + affected);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            // If DB/driver didn't return keys, treat as failure so caller won't report success incorrectly.
            throw new RuntimeException("insertDoctor failed: no generated key returned");

        } catch (Exception e) {
            throw new RuntimeException("insertDoctor failed: " + e.getMessage(), e);
        }
    }

    // Mark doctor as verified
    public static void markVerified(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE doctors SET verified = true, verification_token = NULL WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Set password reset token and expiry time
    public static void setResetToken(int id, String token, LocalDateTime expires) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE doctors SET reset_token = ?, reset_token_expires = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expires));
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Update doctor's password and clear reset token
    public static void updatePassword(int id, String newHash) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE doctors SET password_hash = ?, reset_token=NULL, reset_token_expires=NULL WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Update doctor's name using email
    public static void updateDoctorNameByEmail(String email, String givenName, String familyName) {
        if (email == null || email.isBlank()) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE doctors SET given_name = ?, family_name = ? WHERE email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, givenName);
                ps.setString(2, familyName);
                ps.setString(3, email.trim());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("updateDoctorNameByEmail failed: " + e.getMessage(), e);
        }
    }

    // Map a database row to a Doctor object
    private static Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("id"));
        d.setEmail(rs.getString("email"));
        d.setGivenName(rs.getString("given_name"));
        d.setFamilyName(rs.getString("family_name"));
        d.setPasswordHash(rs.getString("password_hash"));
        d.setVerified(rs.getBoolean("verified"));
        return d;
    }

    // Retrieve all doctors
    public static List<Doctor> getAllDoctors() {
        List<Doctor> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, email, given_name, family_name, verified FROM doctors ORDER BY id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Doctor d = new Doctor();
                d.setId(rs.getInt("id"));
                d.setEmail(rs.getString("email"));
                d.setGivenName(rs.getString("given_name"));
                d.setFamilyName(rs.getString("family_name"));
                d.setVerified(rs.getBoolean("verified"));
                list.add(d);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("getAllDoctors failed: " + e.getMessage());
        }
        return list;
    }
    // Permanently delete doctor by email and reassign patients
    public static boolean hardDeleteByEmail(String email) {
        if (email == null || email.isBlank()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1) Reassign patients to demo (or you can choose NULL if schema allows)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE patients SET doctor = 'demo' WHERE doctor = ?")) {
                    ps.setString(1, email.trim());
                    ps.executeUpdate();
                }

                // 2) Delete doctor
                int affected;
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM doctors WHERE email = ?")) {
                    ps.setString(1, email.trim());
                    affected = ps.executeUpdate();
                }

                conn.commit();
                return affected == 1;

            } catch (Exception inner) {
                try { conn.rollback(); } catch (Exception ignored) {}
                throw inner;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            throw new RuntimeException("hardDeleteByEmail failed: " + e.getMessage(), e);
        }
    }

    // Permanently delete doctor by ID
    public static boolean hardDeleteById(int id) {
        if (id <= 0) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT email FROM doctors WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    String email = rs.getString("email");
                    return hardDeleteByEmail(email);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("hardDeleteById failed: " + e.getMessage(), e);
        }
    }
}
