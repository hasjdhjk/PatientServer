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

            // --- Doctors schema migration (Step 1-3): normalize to given_name/family_name ---
            // 1) Ensure new snake_case columns exist
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS given_name TEXT");
            st.execute("ALTER TABLE doctors ADD COLUMN IF NOT EXISTS family_name TEXT");

            // 2) Backfill from legacy columns if they exist (givenname/familyname)
            // These UPDATEs will fail if legacy columns don't exist, so we guard them.
            try {
                st.execute("UPDATE doctors SET given_name = givenname WHERE given_name IS NULL AND givenname IS NOT NULL");
            } catch (Exception ignored) {}
            try {
                st.execute("UPDATE doctors SET family_name = familyname WHERE family_name IS NULL AND familyname IS NOT NULL");
            } catch (Exception ignored) {}

            // 3) Enforce NOT NULL on new columns (after backfill)
            st.execute("ALTER TABLE doctors ALTER COLUMN given_name SET NOT NULL");
            st.execute("ALTER TABLE doctors ALTER COLUMN family_name SET NOT NULL");

            // 3b) Drop NOT NULL on legacy columns so new-code inserts (snake_case only) won't fail
            try { st.execute("ALTER TABLE doctors ALTER COLUMN givenname DROP NOT NULL"); } catch (Exception ignored) {}
            try { st.execute("ALTER TABLE doctors ALTER COLUMN familyname DROP NOT NULL"); } catch (Exception ignored) {}

            // --- Step 6) Final cleanup: drop legacy columns ---
            try {
                st.execute("ALTER TABLE doctors DROP COLUMN IF EXISTS givenname");
            } catch (Exception ignored) {}
            try {
                st.execute("ALTER TABLE doctors DROP COLUMN IF EXISTS familyname");
            } catch (Exception ignored) {}

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