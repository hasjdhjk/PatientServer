package DataBase;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConnection {

    // ===== LOCAL DEV (ONLY USED IF ENABLED) =====
    private static final boolean ALLOW_LOCAL_FALLBACK = false;

    private static final String LOCAL_JDBC =
            "jdbc:postgresql://localhost:5432/postgres";
    private static final String LOCAL_USER = "postgres";
    private static final String LOCAL_PASS = "h7182005H123";

    // ===== PUBLIC API =====
    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");

            DbInfo info = resolveDbInfo();

            Connection conn = DriverManager.getConnection(
                    info.jdbcUrl,
                    info.user,
                    info.pass
            );

            if (conn == null) {
                throw new SQLException("DriverManager returned null connection");
            }

            return conn;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Database connection failed: " + debugInfo(),
                    e
            );
        }
    }

    // ===== DB RESOLUTION =====
    private static DbInfo resolveDbInfo() {

        // Preferred: DATABASE_URL (most cloud platforms)
        String databaseUrl = getenv("DATABASE_URL");
        if (databaseUrl != null) {
            return fromDatabaseUrl(databaseUrl);
        }

        // Fallback: PG* variables
        String host = getenv("PGHOST");
        String port = getenv("PGPORT");
        String db   = getenv("PGDATABASE");
        String user = getenv("PGUSER");
        String pass = getenv("PGPASSWORD");

        if (host != null && port != null && db != null && user != null) {
            String jdbc =
                    "jdbc:postgresql://" + host + ":" + port + "/" + db +
                            "?sslmode=require";

            return new DbInfo(jdbc, user, pass == null ? "" : pass);
        }

        // Local dev ONLY if explicitly allowed
        if (ALLOW_LOCAL_FALLBACK) {
            return new DbInfo(LOCAL_JDBC, LOCAL_USER, LOCAL_PASS);
        }

        // Cloud must fail fast
        throw new RuntimeException(
                "No database environment variables found"
        );
    }

    // ===== DATABASE_URL PARSER =====
    private static DbInfo fromDatabaseUrl(String raw) {
        try {
            if (raw.startsWith("jdbc:postgresql://")) {
                return new DbInfo(raw, "", "");
            }

            String normalized =
                    raw.replaceFirst("^postgres://", "postgresql://");

            URI uri = new URI(normalized);

            String user = "";
            String pass = "";

            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                user = decode(parts[0]);
                if (parts.length > 1) {
                    pass = decode(parts[1]);
                }
            }

            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String db = uri.getPath().substring(1);

            Map<String, String> q = parseQuery(uri.getQuery());
            String ssl = q.getOrDefault("sslmode", "require");

            String jdbc =
                    "jdbc:postgresql://" + host + ":" + port + "/" + db +
                            "?sslmode=" + ssl;

            if (user.isEmpty()) user = q.getOrDefault("user", "");
            if (pass.isEmpty()) pass = q.getOrDefault("password", "");

            return new DbInfo(jdbc, user, pass);

        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid DATABASE_URL format", e);
        }
    }

    // ===== HELPERS =====
    private static String getenv(String key) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? null : v;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String q) {
        Map<String, String> map = new HashMap<>();
        if (q == null || q.isBlank()) return map;

        for (String p : q.split("&")) {
            String[] kv = p.split("=", 2);
            map.put(decode(kv[0]), kv.length > 1 ? decode(kv[1]) : "");
        }
        return map;
    }

    private static String debugInfo() {
        if (getenv("DATABASE_URL") != null) return "DATABASE_URL detected";
        if (getenv("PGHOST") != null) return "PG* variables detected";
        if (ALLOW_LOCAL_FALLBACK) return "local fallback enabled";
        return "no database configuration found";
    }

    // ===== INTERNAL DTO =====
    private static class DbInfo {
        final String jdbcUrl;
        final String user;
        final String pass;

        DbInfo(String jdbcUrl, String user, String pass) {
            this.jdbcUrl = jdbcUrl;
            this.user = user == null ? "" : user;
            this.pass = pass == null ? "" : pass;
        }
    }
}
