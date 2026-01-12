package DataBase;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConnection {

    // Local dev defaults (only used when APP_ENV=local OR when DATABASE_URL not present and running locally)
    private static final String LOCAL_URL  = "jdbc:postgresql://localhost:5432/postgres";
    private static final String LOCAL_USER = "postgres";
    private static final String LOCAL_PASS = "h7182005H123";

    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");

            DbInfo info = resolveDbInfo();

            // Helpful log (no password)
            System.out.println("[DB] Using JDBC URL: " + info.jdbcUrl + " user=" + info.user);

            return DriverManager.getConnection(info.jdbcUrl, info.user, info.pass);

        } catch (Exception e) {
            throw new RuntimeException("DB connection failed: " + e.getMessage(), e);
        }
    }

    private static DbInfo resolveDbInfo() {
        // 1) Prefer DATABASE_URL (cloud)
        String databaseUrl = getenvAny("DATABASE_URL", "TSURU_DATABASE_URL");
        if (isNonBlank(databaseUrl)) {
            return fromDatabaseUrl(databaseUrl.trim());
        }

        // 2) Fallback to PG* vars (cloud)
        String host = System.getenv("PGHOST");
        String port = System.getenv("PGPORT");
        String db   = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        String pass = System.getenv("PGPASSWORD");

        if (isNonBlank(host) && isNonBlank(port) && isNonBlank(db) && isNonBlank(user)) {
            String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=require";
            return new DbInfo(jdbc, user, pass == null ? "" : pass);
        }

        // 3) Decide local vs cloud STRICTLY
        // If running in Tsuru, DO NOT silently fallback to localhost.
        // Tsuru usually provides TSURU_APPNAME or TSURU_HOST (var names can vary).
        boolean looksLikeCloud = isNonBlank(System.getenv("TSURU_APPNAME"))
                || isNonBlank(System.getenv("TSURU_HOST"))
                || isNonBlank(System.getenv("PORT"));

        if (looksLikeCloud) {
            // FAIL FAST so you see the real reason instead of "invalid credentials"
            throw new RuntimeException(
                    "No database environment variables found on Tsuru. " +
                            "Expected DATABASE_URL or PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD."
            );
        }

        // Local fallback (your laptop)
        return new DbInfo(LOCAL_URL, LOCAL_USER, LOCAL_PASS);
    }

    /**
     * Parse DATABASE_URL formats like:
     *   postgres://user:pass@host:5432/dbname
     *   postgresql://user:pass@host:5432/dbname?sslmode=require
     *   jdbc:postgresql://host:5432/dbname?user=...&password=...
     */
    private static DbInfo fromDatabaseUrl(String raw) {
        if (raw.startsWith("jdbc:postgresql://")) {
            Map<String, String> q = parseQuery(raw.contains("?") ? raw.substring(raw.indexOf('?') + 1) : "");
            String user = q.getOrDefault("user", q.getOrDefault("username", ""));
            String pass = q.getOrDefault("password", "");
            if (!isNonBlank(user)) user = getenvAny("PGUSER");
            if (!isNonBlank(pass)) pass = getenvAny("PGPASSWORD");
            return new DbInfo(raw, user == null ? "" : user, pass == null ? "" : pass);
        }

        String normalized = raw.replaceFirst("^postgres://", "postgresql://");

        try {
            URI uri = new URI(normalized);

            String userInfo = uri.getUserInfo();
            String user = "";
            String pass = "";

            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                user = urlDecode(parts[0]);
                if (parts.length > 1) pass = urlDecode(parts[1]);
            }

            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();

            String path = uri.getPath();
            String db = (path != null && path.startsWith("/")) ? path.substring(1) : path;

            Map<String, String> q = parseQuery(uri.getQuery());
            String sslmode = q.getOrDefault("sslmode", "require");

            String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=" + sslmode;

            if (!isNonBlank(user)) user = q.getOrDefault("user", q.getOrDefault("username", user));
            if (!isNonBlank(pass)) pass = q.getOrDefault("password", pass);

            return new DbInfo(jdbc, user, pass);

        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid DATABASE_URL format: " + raw, e);
        }
    }

    private static String getenvAny(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            if (isNonBlank(v)) return v;
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
