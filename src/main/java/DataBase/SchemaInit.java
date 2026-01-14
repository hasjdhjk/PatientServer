package DataBase;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInit {

    public static void ensureSchema(Connection conn) {
        try (Statement st = conn.createStatement()) {

            // Create tables if missing
            st.execute(
                    "CREATE TABLE IF NOT EXISTS patients (" +
                            "  id SERIAL PRIMARY KEY," +
                            "  doctor TEXT NOT NULL DEFAULT 'demo'," +
                            "  given_name TEXT NOT NULL," +
                            "  family_name TEXT NOT NULL," +
                            "  gender TEXT," +
                            "  age INT," +
                            "  blood_pressure TEXT" +
                            ")"
            );

            st.execute(
                    "CREATE TABLE IF NOT EXISTS doctors (" +
                            "  id SERIAL PRIMARY KEY," +
                            "  email TEXT UNIQUE NOT NULL," +
                            "  given_name TEXT NOT NULL," +
                            "  family_name TEXT NOT NULL," +
                            "  password_hash TEXT NOT NULL," +
                            "  verified BOOLEAN NOT NULL DEFAULT FALSE," +
                            "  verification_token TEXT," +
                            "  reset_token TEXT," +
                            "  reset_token_expires TIMESTAMP" +
                            ")"
            );

            // Index check to make it solid
            st.execute("CREATE INDEX IF NOT EXISTS idx_doctors_email ON doctors(email)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_patients_doctor ON patients(doctor)");

        } catch (Exception e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }
}