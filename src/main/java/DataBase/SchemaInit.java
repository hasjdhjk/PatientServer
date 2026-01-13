package DataBase;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInit {

    public static void ensureSchema(Connection conn) {
        try (Statement st = conn.createStatement()) {

            // 1) Create table if missing
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

            // --- Migrate old doctors table column names (givenname/familyname -> given_name/family_name) ---
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS given_name TEXT");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS family_name TEXT");

            // Copy data from legacy columns if present
            st.execute("UPDATE doctors SET given_name = givenname WHERE given_name IS NULL AND givenname IS NOT NULL");
            st.execute("UPDATE doctors SET family_name = familyname WHERE family_name IS NULL AND familyname IS NOT NULL");

            // Enforce NOT NULL if possible (will succeed once data is populated)
            st.execute("ALTER TABLE doctors ALTER COLUMN given_name SET NOT NULL");
            st.execute("ALTER TABLE doctors ALTER COLUMN family_name SET NOT NULL");

            // Optional index
            st.execute("CREATE INDEX IF NOT EXISTS idx_doctors_email ON doctors(email)");

            // 2) Migrate old table (if it already existed without doctor column)
            st.execute("ALTER TABLE patients ADD COLUMN IF NOT EXISTS doctor TEXT NOT NULL DEFAULT 'demo'");

            // 3) Optional index
            st.execute("CREATE INDEX IF NOT EXISTS idx_patients_doctor ON patients(doctor)");

        } catch (Exception e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }
}