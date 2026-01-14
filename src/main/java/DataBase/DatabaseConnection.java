package DataBase;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConnection {

    private static volatile boolean schemaInitialized = false;

    // Connect to Postgres using env vars.
    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            DbInfo info = resolveDbInfoFromEnvStrict();
            Connection conn = DriverManager.getConnection(info.jdbcUrl, info.user, info.pass);
            if (conn == null) throw new SQLException("DriverManager.getConnection returned null");

            if (!schemaInitialized) {
                synchronized (DatabaseConnection.class) {
                    if (!schemaInitialized) {
                        SchemaInit.ensureSchema(conn);
                        schemaInitialized = true;
                    }
                }
            }

            return conn;

        } catch (Exception e) {
            // IMPORTANT: print env-based details without leaking a password
            throw new RuntimeException("DB connection failed: " + safeDebugInfo() + " msg=" + e.getMessage(), e);
        }
    }

    private static DbInfo resolveDbInfoFromEnvStrict() {
        String databaseUrl = getenvAny("DATABASE_URL", "TSURU_DATABASE_URL"); // second key just in case

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            return fromDatabaseUrl(databaseUrl.trim());
        }

        // Strict fallback to PG* vars (common in many platforms)
        String host = System.getenv("PGHOST");
        String port = System.getenv("PGPORT");
        String db   = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        String pass = System.getenv("PGPASSWORD");

        if (!isNonBlank(host) || !isNonBlank(port) || !isNonBlank(db) || !isNonBlank(user)) {
            throw new IllegalStateException("Missing required Postgres env vars. Need DATABASE_URL or PGHOST/PGPORT/PGDATABASE/PGUSER.");
        }

        // add sslmode=require for cloud safety (harmless locally if ignored)
        String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=require";
        return new DbInfo(jdbc, user, pass == null ? "" : pass);
    }

    private static DbInfo fromDatabaseUrl(String raw) {
        // If already a JDBC url
        if (raw.startsWith("jdbc:postgresql://")) {
            Map<String, String> q = parseQuery(raw.contains("?") ? raw.substring(raw.indexOf('?') + 1) : "");
            String user = q.getOrDefault("user", q.getOrDefault("username", ""));
            String pass = q.getOrDefault("password", "");
            // If user missing, fallback to PGUSER/PGPASSWORD if present
            if (!isNonBlank(user)) user = getenvAny("PGUSER");
            if (!isNonBlank(pass)) pass = getenvAny("PGPASSWORD");
            return new DbInfo(raw, user == null ? "" : user, pass == null ? "" : pass);
        }

        String normalized = raw.replaceFirst("^postgres://", "postgresql://");

        try {
            URI uri = new URI(normalized);

            String userInfo = uri.getUserInfo(); // "user:pass"
            String user = "";
            String pass = "";

            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                user = urlDecode(parts[0]);
                if (parts.length > 1) pass = urlDecode(parts[1]);
            }

            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();

            String path = uri.getPath(); // "/dbname"
            String db = (path != null && path.startsWith("/")) ? path.substring(1) : path;

            Map<String, String> q = parseQuery(uri.getQuery());
            String sslmode = q.getOrDefault("sslmode", "require"); // cloud default

            // Build JDBC
            String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=" + sslmode;

            // Some platforms put user/pass in a query too
            if (!isNonBlank(user)) user = q.getOrDefault("user", q.getOrDefault("username", user));
            if (!isNonBlank(pass)) pass = q.getOrDefault("password", pass);

            return new DbInfo(jdbc, user, pass);

        } catch (URISyntaxException e) {
            // If parsing fails, the last resort: throw exception since no local fallback
            throw new IllegalStateException("Invalid DATABASE_URL format and no local fallback available.", e);
        }
    }

    // Helpers

    private static String getenvAny(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;

        String[] pairs = query.split("&");
        for (String p : pairs) {
            String[] kv = p.split("=", 2);
            String k = urlDecode(kv[0]);
            String v = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.put(k, v);
        }
        return map;
    }

    private static String safeDebugInfo() {
        // don’t print passwords
        String databaseUrl = getenvAny("DATABASE_URL", "TSURU_DATABASE_URL");
        if (databaseUrl != null) {
            return "using DATABASE_URL (redacted)";
        }
        String host = System.getenv("PGHOST");
        String db = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        if (host != null || db != null || user != null) {
            return "using PG* env host=" + host + " db=" + db + " user=" + user;
        }
        return "missing DATABASE_URL and PG* env";
    }

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