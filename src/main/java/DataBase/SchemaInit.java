package DataBase;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInit {

    public static void ensureSchema(Connection conn) {
        try (Statement st = conn.createStatement()) {

            // ========= patients =========
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

            st.execute("ALTER TABLE patients ADD COLUMN IF NOT EXISTS doctor TEXT NOT NULL DEFAULT 'demo'");
            st.execute("CREATE INDEX IF NOT EXISTS idx_patients_doctor ON patients(doctor)");

            // ========= doctors =========
            st.execute(
                    "CREATE TABLE IF NOT EXISTS doctors (" +
                            "  id SERIAL PRIMARY KEY," +
                            "  email TEXT UNIQUE NOT NULL," +
                            "  given_name TEXT NOT NULL," +
                            "  family_name TEXT NOT NULL," +
                            "  password_hash TEXT NOT NULL," +
                            "  verified BOOLEAN NOT NULL DEFAULT TRUE," +
                            "  verification_token TEXT," +
                            "  reset_token TEXT," +
                            "  reset_token_expires TIMESTAMP" +
                            ")"
            );

            // Migrate old doctors table if it used givenname/familyname (camel) instead of given_name/family_name
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS given_name TEXT");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS family_name TEXT");

            // Copy data from old columns if they exist and new columns are null
            st.execute("UPDATE doctors SET given_name = givenname WHERE given_name IS NULL AND givenname IS NOT NULL");
            st.execute("UPDATE doctors SET family_name = familyname WHERE family_name IS NULL AND familyname IS NOT NULL");

            // Enforce NOT NULL if possible (only safe if table not empty and all rows now have values)
            // If you might already have rows without names, comment these two lines out.
            st.execute("ALTER TABLE doctors ALTER COLUMN given_name SET NOT NULL");
            st.execute("ALTER TABLE doctors ALTER COLUMN family_name SET NOT NULL");

        } catch (Exception e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }
}