//package DataAccessObject;
//
//public class PatientDAO {
//
//    public static List<Patient> getAllPatients() {
//        List<Patient> patients = new ArrayList<>();
//
//        try (Connection conn = DatabaseConnection.getConnection()) {
//            Statement stmt = conn.createStatement();
//            ResultSet rs = stmt.executeQuery("SELECT * FROM patients");
//
//            while (rs.next()) {
//                Patient p = new Patient(
//                        rs.getInt("id"),
//                        rs.getString("givenname"),
//                        rs.getString("familyname"),
//                        rs.getInt("heartrate"),
//                        rs.getDouble("temperature"),
//                        rs.getString("bp")
//                );
//                patients.add(p);
//            }
//
//        } catch (Exception e) { e.printStackTrace(); }
//
//        return patients;
//    }
//}
