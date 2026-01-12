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

    // Local dev defaults (do NOT hardcode real credentials in source control)
    // You can override these locally via env: LOCAL_DB_URL / LOCAL_DB_USER / LOCAL_DB_PASS
    private static final String LOCAL_URL  = getenvOrDefault("LOCAL_DB_URL", "jdbc:postgresql://localhost:5432/postgres");
    private static final String LOCAL_USER = getenvOrDefault("LOCAL_DB_USER", "postgres");
    private static final String LOCAL_PASS = getenvOrDefault("LOCAL_DB_PASS", "");

    // Cloud should fail fast unless explicitly allowed to fall back to local.
    // Local runs usually have TSURU_APPNAME unset.
    private static final boolean ALLOW_LOCAL_FALLBACK =
            System.getenv("TSURU_APPNAME") == null || "true".equalsIgnoreCase(System.getenv("ALLOW_LOCAL_FALLBACK"));

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
        debugPrintEnvOnce();
        DbInfo info = resolveDbInfoFromEnv();
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(info.jdbcUrl, info.user, info.pass);
        } catch (Exception e) {
            throw new RuntimeException(
                    "DB connection failed: " + safeDebugInfo() +
                            " | msg=" + e.getMessage(),
                    e
            );
        }
    }

    // ======================= ENV RESOLUTION =======================

    private static DbInfo resolveDbInfoFromEnv() {
        // 1) Preferred: DATABASE_URL (or TSURU_DATABASE_URL)
        String databaseUrl = getenvAny("DATABASE_URL", "TSURU_DATABASE_URL");
        if (isNonBlank(databaseUrl)) {
            return fromDatabaseUrl(databaseUrl.trim());
        }

        // 2) Tsuru: parse TSURU_SERVICES (JSON string) heuristically.
        // We avoid Gson/JsonParser to prevent dependency/version issues.
        String tsuruServices = System.getenv("TSURU_SERVICES");
        if (isNonBlank(tsuruServices)) {
            String extracted = extractPostgresUrlFromTsuruServices(tsuruServices);
            if (isNonBlank(extracted)) {
                return fromDatabaseUrl(extracted.trim());
            }
        }

        // 3) Fallback: PG* variables
        String host = System.getenv("PGHOST");
        String port = System.getenv("PGPORT");
        String db   = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        String pass = System.getenv("PGPASSWORD");

        if (isNonBlank(host) && isNonBlank(port) && isNonBlank(db) && isNonBlank(user)) {
            String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=require";
            return new DbInfo(jdbc, user, pass == null ? "" : pass);
        }

        // 4) Local fallback (only allowed for local dev)
        if (ALLOW_LOCAL_FALLBACK) {
            return new DbInfo(LOCAL_URL, LOCAL_USER, LOCAL_PASS);
        }

        // Cloud must fail fast
        throw new RuntimeException(
                "No database configuration found (need DATABASE_URL / TSURU_SERVICES / PG*). " +
                        "TSURU_SERVICES present=" + isNonBlank(System.getenv("TSURU_SERVICES"))
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
        String databaseUrl = getenvAny("DATABASE_URL", "TSURU_DATABASE_URL");
        if (isNonBlank(databaseUrl)) {
            return "using DATABASE_URL (redacted)";
        }

        String tsuruServices = System.getenv("TSURU_SERVICES");
        if (isNonBlank(tsuruServices)) {
            String extracted = extractPostgresUrlFromTsuruServices(tsuruServices);
            if (isNonBlank(extracted)) {
                return "using TSURU_SERVICES extractedUrl=" + redactSecrets(trunc(extracted, 200));
            }
            return "using TSURU_SERVICES (no url extracted)";
        }

        String host = System.getenv("PGHOST");
        String db = System.getenv("PGDATABASE");
        String user = System.getenv("PGUSER");
        if (host != null || db != null || user != null) {
            return "using PG* env host=" + host + " db=" + db + " user=" + user;
        }

        return "using LOCAL (local dev)";
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
    private static volatile boolean DID_DEBUG_PRINT = false;

    private static void debugPrintEnvOnce() {
        // Only print once per JVM
        if (DID_DEBUG_PRINT) return;
        DID_DEBUG_PRINT = true;

        boolean isTsuru = System.getenv("TSURU_APPNAME") != null;
        System.out.println("========== DB ENV DEBUG ==========");
        System.out.println("isTsuru=" + isTsuru);

        // Print presence only (avoid leaking secrets)
        System.out.println("has DATABASE_URL=" + isNonBlank(System.getenv("DATABASE_URL")));
        System.out.println("has TSURU_DATABASE_URL=" + isNonBlank(System.getenv("TSURU_DATABASE_URL")));
        System.out.println("has PGHOST=" + isNonBlank(System.getenv("PGHOST")));
        System.out.println("has PGPORT=" + isNonBlank(System.getenv("PGPORT")));
        System.out.println("has PGDATABASE=" + isNonBlank(System.getenv("PGDATABASE")));
        System.out.println("has PGUSER=" + isNonBlank(System.getenv("PGUSER")));
        System.out.println("has PGPASSWORD=" + isNonBlank(System.getenv("PGPASSWORD")));
        System.out.println("has TSURU_SERVICES=" + isNonBlank(System.getenv("TSURU_SERVICES")));

        String tsuruServices = System.getenv("TSURU_SERVICES");
        if (tsuruServices != null) {
            String head = trunc(tsuruServices, 1200);
            System.out.println("TSURU_SERVICES(head, redacted)=" + trunc(redactSecrets(head), 1200));
        }

        System.out.println("=================================");
    }

    // ======================= NEW HELPERS =======================

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    /**
     * Heuristically extract a postgres/jdbc URL from TSURU_SERVICES JSON.
     * Works across multiple common service formats by scanning for substrings.
     */
    private static String extractPostgresUrlFromTsuruServices(String json) {
        // Common keys in various platforms: uri, url, connection_uri, connection_string
        String[] keys = new String[] {"connection_uri", "connection_string", "uri", "url", "dsn"};
        for (String k : keys) {
            String v = extractJsonStringValue(json, k);
            if (isNonBlank(v) && looksLikePostgresUrl(v)) {
                return v;
            }
        }

        // If no key-based hit, scan raw for a postgres-like URL
        String rawHit = findFirstPostgresLikeUrl(json);
        if (isNonBlank(rawHit)) return rawHit;

        // Otherwise, attempt host/port/db/user/pass style (some TSURU_SERVICES include these fields)
        String host = extractJsonStringValue(json, "host");
        String port = extractJsonStringValue(json, "port");
        String db   = firstNonBlank(
                extractJsonStringValue(json, "database"),
                extractJsonStringValue(json, "dbname"),
                extractJsonStringValue(json, "name")
        );
        String user = firstNonBlank(
                extractJsonStringValue(json, "user"),
                extractJsonStringValue(json, "username")
        );
        String pass = extractJsonStringValue(json, "password");

        if (isNonBlank(host) && isNonBlank(db)) {
            int p = 5432;
            try { if (isNonBlank(port)) p = Integer.parseInt(port.trim()); } catch (Exception ignored) {}
            String u = isNonBlank(user) ? user : "";
            String pw = pass == null ? "" : pass;

            // Build a DATABASE_URL-style string first (fromDatabaseUrl can parse it)
            if (isNonBlank(u)) {
                return "postgresql://" + urlEncode(u) + ":" + urlEncode(pw) + "@" + host + ":" + p + "/" + db + "?sslmode=require";
            } else {
                return "jdbc:postgresql://" + host + ":" + p + "/" + db + "?sslmode=require";
            }
        }

        return null;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (isNonBlank(v)) return v;
        }
        return null;
    }

    private static boolean looksLikePostgresUrl(String s) {
        String x = s.trim().toLowerCase();
        return x.startsWith("postgres://") || x.startsWith("postgresql://") || x.startsWith("jdbc:postgresql://");
    }

    /**
     * Very small JSON string extractor: finds the first occurrence of "key":"value" and returns value.
     * Not a full JSON parser, but good enough for env introspection.
     */
    private static String extractJsonStringValue(String json, String key) {
        if (json == null || key == null) return null;
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;

        // find ':' after the key
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;

        // skip whitespace
        int p = colon + 1;
        while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;

        // expect opening quote
        if (p >= json.length() || json.charAt(p) != '"') return null;
        p++;

        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        while (p < json.length()) {
            char c = json.charAt(p++);
            if (esc) {
                // handle basic escapes
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (p + 3 < json.length()) {
                            String hex = json.substring(p, p + 4);
                            try { sb.append((char) Integer.parseInt(hex, 16)); } catch (Exception ignored) {}
                            p += 4;
                        }
                        break;
                    default: sb.append(c);
                }
                esc = false;
                continue;
            }
            if (c == '\\') { esc = true; continue; }
            if (c == '"') break;
            sb.append(c);
        }
        String out = sb.toString();
        return out.isBlank() ? null : out;
    }

    /** Scan a string and return the first postgres-like URL found, if any. */
    private static String findFirstPostgresLikeUrl(String text) {
        if (text == null) return null;
        String[] schemes = new String[] {"postgresql://", "postgres://", "jdbc:postgresql://"};
        int best = Integer.MAX_VALUE;
        String scheme = null;
        for (String sc : schemes) {
            int idx = text.toLowerCase().indexOf(sc);
            if (idx >= 0 && idx < best) {
                best = idx;
                scheme = sc;
            }
        }
        if (scheme == null) return null;

        int start = best;
        int end = start;
        while (end < text.length()) {
            char c = text.charAt(end);
            // stop at whitespace or quote
            if (Character.isWhitespace(c) || c == '"' || c == '\'' ) break;
            end++;
        }
        String candidate = text.substring(start, end);
        return candidate.isBlank() ? null : candidate;
    }

    private static String urlEncode(String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }

    private static String redactSecrets(String s) {
        if (s == null) return null;

        // redact JSON password fields
        String out = s.replaceAll("(?i)\\\"password\\\"\\s*:\\s*\\\".*?\\\"", "\"password\":\"***\"");

        // redact user:pass@ in URLs
        out = out.replaceAll("(?i)(postgres(?:ql)?://[^:/@\\s\"]+:)([^@\\s\"]+)(@)", "$1***$3");
        out = out.replaceAll("(?i)(jdbc:postgresql://[^?\\s\"]+\\?user=[^&\\s\"]+&password=)([^&\\s\"]+)", "$1***");

        return out;
    }
}