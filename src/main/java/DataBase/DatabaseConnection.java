package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    // LOCAL database (for running on your laptop)
    private static final String LOCAL_URL =
            "jdbc:postgresql://localhost:5432/postgres";
    private static final String LOCAL_USER = "postgres";     // your local username
    private static final String LOCAL_PASSWORD = "h7182005H123"; // your local password

    // Detect if running on Tsuru (environment variables exist)
    private static boolean runningOnTsuru() {
        return System.getenv("PGHOST") != null &&
                System.getenv("PGPORT") != null;
    }

    public static Connection getConnection() {

        try {
            // Load JDBC driver
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (runningOnTsuru()) {
                // Tsuru database format (from Tutorial 7)
                String dbUrl = "jdbc:postgresql://" +
                        System.getenv("PGHOST") + ":" +
                        System.getenv("PGPORT") + "/" +
                        System.getenv("PGDATABASE");

                return DriverManager.getConnection(
                        dbUrl,
                        System.getenv("PGUSER"),
                        System.getenv("PGPASSWORD")
                );

            } else {
                // Local Postgres database
                return DriverManager.getConnection(
                        LOCAL_URL, LOCAL_USER, LOCAL_PASSWORD);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
