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

            // 2) Migrate old table (if it already existed without doctor column)
            st.execute("ALTER TABLE patients ADD COLUMN IF NOT EXISTS doctor TEXT NOT NULL DEFAULT 'demo'");

            // 3) Optional index
            st.execute("CREATE INDEX IF NOT EXISTS idx_patients_doctor ON patients(doctor)");

        } catch (Exception e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }
}