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

    // ===== LOCAL DEV FALLBACK =====
    private static final String LOCAL_URL  = "jdbc:postgresql://localhost:5432/postgres";
    private static final String LOCAL_USER = "postgres";
    private static final String LOCAL_PASS = "h7182005H123";

    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");

            DbInfo info = resolveDbInfoFromEnv();

            return DriverManager.getConnection(
                    info.jdbcUrl,
                    info.user,
                    info.pass
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "DB connection failed (" + safeDebugInfo() + "): " + e.getMessage(),
                    e
            );
        }
    }

    // ======================= ENV RESOLUTION =======================

    private static DbInfo resolveDbInfoFromEnv() {

        // 1️⃣ Preferred on Tsuru / cloud
        String databaseUrl = getenvAny("DATABASE_URL", "TSURU_DATABASE_URL");
        if (isNonBlank(databaseUrl)) {
            return fromDatabaseUrl(databaseUrl.trim());
        }

        // 2️⃣ Fallback: PG* vars
        String host = System.getenv("PGHOST");
        String port = System.getenv("PGPORT");
        String db   = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        String pass = System.getenv("PGPASSWORD");

        if (isNonBlank(host) && isNonBlank(port) && isNonBlank(db) && isNonBlank(user)) {
            String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=require";
            return new DbInfo(jdbc, user, pass == null ? "" : pass);
        }

        // 3️⃣ Local dev
        return new DbInfo(LOCAL_URL, LOCAL_USER, LOCAL_PASS);
    }

    // ======================= DATABASE_URL PARSER =======================

    private static DbInfo fromDatabaseUrl(String raw) {

        // Already JDBC
        if (raw.startsWith("jdbc:postgresql://")) {
            Map<String, String> q = parseQuery(raw.contains("?")
                    ? raw.substring(raw.indexOf('?') + 1)
                    : "");

            String user = q.getOrDefault("user", getenvAny("PGUSER"));
            String pass = q.getOrDefault("password", getenvAny("PGPASSWORD"));

            return new DbInfo(raw, nz(user), nz(pass));
        }

        // Normalize postgres:// → postgresql://
        String normalized = raw.replaceFirst("^postgres://", "postgresql://");

        try {
            URI uri = new URI(normalized);

            String user = "";
            String pass = "";

            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                user = urlDecode(parts[0]);
                if (parts.length > 1) pass = urlDecode(parts[1]);
            }

            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String db = uri.getPath().substring(1);

            Map<String, String> q = parseQuery(uri.getQuery());
            String sslmode = q.getOrDefault("sslmode", "require");

            String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db
                    + "?sslmode=" + sslmode;

            if (!isNonBlank(user)) user = q.getOrDefault("user", getenvAny("PGUSER"));
            if (!isNonBlank(pass)) pass = q.getOrDefault("password", getenvAny("PGPASSWORD"));

            return new DbInfo(jdbc, nz(user), nz(pass));

        } catch (URISyntaxException e) {
            return new DbInfo(LOCAL_URL, LOCAL_USER, LOCAL_PASS);
        }
    }

    // ======================= HELPERS =======================

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

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (!isNonBlank(query)) return map;

        for (String p : query.split("&")) {
            String[] kv = p.split("=", 2);
            map.put(urlDecode(kv[0]), kv.length > 1 ? urlDecode(kv[1]) : "");
        }
        return map;
    }

    private static String safeDebugInfo() {
        if (getenvAny("DATABASE_URL", "TSURU_DATABASE_URL") != null) {
            return "using DATABASE_URL";
        }
        if (System.getenv("PGHOST") != null) {
            return "using PG* env";
        }
        return "using LOCAL postgres";
    }

    private static class DbInfo {
        final String jdbcUrl;
        final String user;
        final String pass;

        DbInfo(String jdbcUrl, String user, String pass) {
            this.jdbcUrl = jdbcUrl;
            this.user = user;
            this.pass = pass;
        }
    }
}
