package DataBase;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInit {
    public static void ensureSchema() {
        String sqlPatients =
                "CREATE TABLE IF NOT EXISTS patients (" +
                        " id SERIAL PRIMARY KEY," +
                        " given_name TEXT NOT NULL," +
                        " family_name TEXT NOT NULL," +
                        " gender TEXT," +
                        " age INT," +
                        " blood_pressure TEXT" +
                        ");";

//        String sqlDoctors =
//                "CREATE TABLE IF NOT EXISTS doctors (" +
//                        " id SERIAL PRIMARY KEY," +
//                        " email TEXT UNIQUE NOT NULL," +
//                        " given_name TEXT," +
//                        " family_name TEXT," +
//                        " password_hash TEXT NOT NULL," +
//                        " verified BOOLEAN NOT NULL DEFAULT FALSE," +
//                        " verification_token TEXT," +
//                        " reset_token TEXT" +
//                        ");";

        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {
            st.execute(sqlPatients);
            //st.execute(sqlDoctors);
        } catch (Exception e) {
            throw new RuntimeException("Schema init failed: " + e.getMessage(), e);
        }
    }
}