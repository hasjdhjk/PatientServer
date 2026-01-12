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

    // 你现在的 DB 是本机 postgres 数据库
    private static final String LOCAL_URL  = "jdbc:postgresql://localhost:5432/postgres";
//    private static final String LOCAL_URL  = System.getenv("APP_BASE_URL");

    // 你 psql 里 current_user() = apple，所以这里用 apple
    private static final String LOCAL_USER = "postgres";
//    private static final String LOCAL_USER = "apple";

    // 如果你没有给 apple 设置密码（brew 默认本机可能免密），就留空字符串。
    // 如果你有密码，就填进去。
    private static final String LOCAL_PASS = "h7182005H123";
//    private static final String LOCAL_PASS = "";

    /**
     * Tsuru / cloud: use env vars.
     *
     * Prefer:
     *   1) DATABASE_URL  (e.g. postgres://user:pass@host:port/db?sslmode=require)
     *
     * Fallback:
     *   2) PGHOST / PGPORT / PGDATABASE / PGUSER / PGPASSWORD
     */
    public static Connection getConnection() {

        // 👇 只会在 Tsuru 打印一次日志
        debugPrintEnvOnce();
        System.out.println("========== TSURU ENV DEBUG ==========");
        System.out.println(System.getenv("TSURU_SERVICES"));
        DbInfo info = resolveDbInfoFromEnv();

        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(info.jdbcUrl, info.user, info.pass);
        } catch (Exception e) {
            throw new RuntimeException(
                    "DB connection failed: url=" + info.jdbcUrl +
                            " user=" + info.user +
                            " msg=" + e.getMessage(),
                    e
            );
        }
    }

    // ======================= ENV RESOLUTION =======================

    private static DbInfo resolveDbInfoFromEnv() {
        String databaseUrl = getenvAny("DATABASE_URL", "TSURU_DATABASE_URL"); // second key just in case

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            return fromDatabaseUrl(databaseUrl.trim());
        }

        // Fallback to PG* vars (common in many platforms)
        String host = System.getenv("PGHOST");
        String port = System.getenv("PGPORT");
        String db   = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        String pass = System.getenv("PGPASSWORD");

        if (isNonBlank(host) && isNonBlank(port) && isNonBlank(db) && isNonBlank(user)) {
            // add sslmode=require for cloud safety (harmless locally if ignored)
            String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=require";
            return new DbInfo(jdbc, user, pass == null ? "" : pass);
        }

        // Local fallback
        throw new RuntimeException(
                "No database environment variables found. " +
                        "DATABASE_URL or PG* must be set on Tsuru."
        );

    }

    /**
     * Parse DATABASE_URL formats like:
     *   postgres://user:pass@host:5432/dbname
     *   postgresql://user:pass@host:5432/dbname?sslmode=require
     *   jdbc:postgresql://host:5432/dbname?user=...&password=...
     */
    private static DbInfo fromDatabaseUrl(String raw) {
        // If already a JDBC url
        if (raw.startsWith("jdbc:postgresql://")) {
            // Some platforms include user/pass as query params, others don't.
            // We'll keep it as jdbcUrl and still try to extract user/pass if possible.
            Map<String, String> q = parseQuery(raw.contains("?") ? raw.substring(raw.indexOf('?') + 1) : "");
            String user = q.getOrDefault("user", q.getOrDefault("username", ""));
            String pass = q.getOrDefault("password", "");
            // If user missing, fallback to PGUSER/PGPASSWORD if present
            if (!isNonBlank(user)) user = getenvAny("PGUSER");
            if (!isNonBlank(pass)) pass = getenvAny("PGPASSWORD");
            return new DbInfo(raw, user == null ? "" : user, pass == null ? "" : pass);
        }

        // Normalize scheme postgres:// or postgresql://
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

            // Some platforms put user/pass in query too
            if (!isNonBlank(user)) user = q.getOrDefault("user", q.getOrDefault("username", user));
            if (!isNonBlank(pass)) pass = q.getOrDefault("password", pass);

            return new DbInfo(jdbc, user, pass);

        } catch (URISyntaxException e) {
            // If parsing fails, last resort: local fallback
            return new DbInfo(LOCAL_URL, LOCAL_USER, LOCAL_PASS);
        }
    }

    // ======================= HELPERS =======================

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
        return "using LOCAL url=" + LOCAL_URL + " user=" + LOCAL_USER;
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
    private static void debugPrintEnvOnce() {
        // 只在 Tsuru / 云端打印，避免本地污染日志
        if (System.getenv("TSURU_APPNAME") == null) return;

        System.out.println("========== TSURU ENV DEBUG ==========");

        // 打印 TSURU_SERVICES 原始字符串（最重要）
        String tsuruServices = System.getenv("TSURU_SERVICES");
        if (tsuruServices == null) {
            System.out.println("TSURU_SERVICES = <null>");
        } else {
            System.out.println("TSURU_SERVICES raw:");
            System.out.println(tsuruServices);
        }

        // 顺便看看有没有 DATABASE_URL / PG*
        System.out.println("DATABASE_URL = " + System.getenv("DATABASE_URL"));
        System.out.println("PGHOST = " + System.getenv("PGHOST"));
        System.out.println("PGPORT = " + System.getenv("PGPORT"));
        System.out.println("PGDATABASE = " + System.getenv("PGDATABASE"));
        System.out.println("PGUSER = " + System.getenv("PGUSER"));
        System.out.println("PGPASSWORD = " +
                (System.getenv("PGPASSWORD") == null ? "<null>" : "<hidden>"));

        System.out.println("=====================================");
    }
}