package DataAccessObject;

import DataBase.DatabaseConnection;
import Models.Doctor;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO {

    public static Doctor findByEmail(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors WHERE email = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
 
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
 
    public static Doctor findByVerificationToken(String token) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors WHERE verification_token = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

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
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static void insertDoctor(String email, String givenName,
                                    String familyName, String passwordHash,
                                    String verificationToken) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO doctors " +
                    "(email, givenname, familyname, password_hash, verified, verification_token) " +
                    "VALUES (?,?,?,?,false,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, givenName);
            ps.setString(3, familyName);
            ps.setString(4, passwordHash);
            ps.setString(5, verificationToken);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void markVerified(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE doctors SET verified = true, verification_token = NULL WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

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

    public static void updatePassword(int id, String newHash) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE doctors SET password_hash = ?, reset_token=NULL, reset_token_expires=NULL WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("id"));
        d.setEmail(rs.getString("email"));
        d.setGivenName(rs.getString("givenname"));
        d.setFamilyName(rs.getString("familyname"));
        d.setPasswordHash(rs.getString("password_hash"));
        d.setVerified(rs.getBoolean("verified"));
        return d;
    }
    public static List<Doctor> getAllDoctors() {
        List<Doctor> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, email, givenname, familyname, verified FROM doctors ORDER BY id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Doctor d = new Doctor();
                d.setId(rs.getInt("id"));
                d.setEmail(rs.getString("email"));
                d.setGivenName(rs.getString("givenname"));
                d.setFamilyName(rs.getString("familyname"));
                d.setVerified(rs.getBoolean("verified"));
                list.add(d);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("getAllDoctors failed: " + e.getMessage());
        }
        return list;
    }
}
