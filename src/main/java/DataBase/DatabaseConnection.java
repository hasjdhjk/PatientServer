package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // 你现在的 DB 是本机 postgres 数据库
//    private static final String URL  = "jdbc:postgresql://localhost:5432/postgres";
    private static final String URL  = System.getenv("APP_BASE_URL");

    // 你 psql 里 current_user() = apple，所以这里用 apple
    private static final String USER = "postgres";
//    private static final String USER = "apple";

    // 如果你没有给 apple 设置密码（brew 默认本机可能免密），就留空字符串。
    // 如果你有密码，就填进去。
    private static final String PASS = "h7182005H123";
//    private static final String PASS = "";

    public static Connection getConnection() {
        try {
            // 确保 driver 已加载（有些环境不写也行，但写了更稳）
            Class.forName("org.postgresql.Driver");

            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            if (conn == null) {
                throw new SQLException("DriverManager.getConnection returned null");
            }
            return conn;

        } catch (Exception e) {
            // ✅ 不要吞掉：直接抛，让 servlet 返回真实原因
            throw new RuntimeException(
                    "DB connection failed: url=" + URL + ", user=" + USER + ", msg=" + e.getMessage(),
                    e
            );
        }
    }
}