import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Kết nối cơ sở dữ liệu SQLite (file drivingtest.db nằm cạnh thư mục chạy).
 * Cần thêm thư viện sqlite-jdbc.jar vào Build Path (xem README.md).
 */
public class Database {
    private static final String URL = "jdbc:sqlite:drivingtest.db";

    /** Tài khoản admin mặc định được tạo ở lần chạy đầu tiên (nên đổi mật khẩu khi triển khai thật). */
    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASS = "admin123";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) return true;
        }
        return false;
    }

    /** Tạo bảng nếu chưa có và nạp dữ liệu mẫu lần đầu. */
    public static void init() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS questions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "content TEXT NOT NULL,"
                    + "option_a TEXT NOT NULL,"
                    + "option_b TEXT NOT NULL,"
                    + "option_c TEXT NOT NULL,"
                    + "option_d TEXT NOT NULL,"
                    + "correct INTEGER NOT NULL,"
                    + "category TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT NOT NULL UNIQUE,"
                    + "password_hash TEXT NOT NULL,"
                    + "salt TEXT NOT NULL,"
                    + "full_name TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "created_at TEXT NOT NULL)");

            // CSDL phiên bản cũ: bảng results chưa có user_id -> đổi tên giữ lại làm dữ liệu cũ.
            if (tableExists(c, "results") && !columnExists(c, "results", "user_id")) {
                st.execute("ALTER TABLE results RENAME TO results_legacy");
            }
            st.execute("CREATE TABLE IF NOT EXISTS results ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER NOT NULL,"
                    + "exam_code TEXT NOT NULL,"
                    + "taken_at TEXT NOT NULL,"
                    + "duration_sec INTEGER NOT NULL,"
                    + "total INTEGER NOT NULL,"
                    + "score INTEGER NOT NULL,"
                    + "passed INTEGER NOT NULL)");
            // Lưu "ảnh chụp" từng câu đã thi để xem lại dù admin có sửa/xóa câu hỏi sau đó.
            st.execute("CREATE TABLE IF NOT EXISTS result_answers ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "result_id INTEGER NOT NULL,"
                    + "q_no INTEGER NOT NULL,"
                    + "question_id INTEGER NOT NULL,"
                    + "content TEXT NOT NULL,"
                    + "option_a TEXT NOT NULL,"
                    + "option_b TEXT NOT NULL,"
                    + "option_c TEXT NOT NULL,"
                    + "option_d TEXT NOT NULL,"
                    + "correct INTEGER NOT NULL,"
                    + "chosen INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_results_user ON results(user_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_answers_result ON result_answers(result_id)");

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE role='ADMIN'")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    UserDAO.insert(c, DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASS, "Quản trị viên", User.ADMIN);
                }
            }

            seedQuestionBank(c);
        }
    }

    /**
     * Nạp ngân hàng 100 câu hỏi (QuestionBank) đúng một lần cho mỗi CSDL, đánh dấu trong bảng meta
     * để câu hỏi admin đã xóa không bị nạp lại ở lần chạy sau. Với CSDL cũ: bỏ 10 câu mẫu cũ chưa
     * chỉnh sửa và thêm các câu của ngân hàng chưa có.
     */
    private static void seedQuestionBank(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM meta WHERE key='question_bank_v2'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return;
        }
        boolean oldAuto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement del = c.prepareStatement("DELETE FROM questions WHERE content = ?")) {
                for (String legacy : QuestionBank.LEGACY_CONTENTS) {
                    del.setString(1, legacy);
                    del.addBatch();
                }
                del.executeBatch();
            }
            String exists = "SELECT 1 FROM questions WHERE content = ?";
            String insert = "INSERT INTO questions(content, option_a, option_b, option_c, option_d, correct, category) "
                    + "VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement chk = c.prepareStatement(exists);
                 PreparedStatement ins = c.prepareStatement(insert)) {
                for (Object[] r : QuestionBank.ALL) {
                    chk.setString(1, (String) r[0]);
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next()) continue;
                    }
                    for (int i = 0; i < 7; i++) {
                        if (r[i] instanceof Integer) ins.setInt(i + 1, (Integer) r[i]);
                        else ins.setString(i + 1, (String) r[i]);
                    }
                    ins.executeUpdate();
                }
            }
            try (Statement st = c.createStatement()) {
                st.executeUpdate("INSERT INTO meta(key, value) VALUES ('question_bank_v2', '1')");
            }
            c.commit();
        } catch (SQLException ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(oldAuto);
        }
    }
}
