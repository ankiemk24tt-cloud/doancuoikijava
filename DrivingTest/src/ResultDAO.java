import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Lưu kết quả thi (kèm chi tiết từng câu) và các truy vấn thống kê. */
public class ResultDAO {

    /** Một lần thi của một người dùng. */
    public static class Result {
        public final int id;
        public final int userId;
        public final String examCode;
        public final String takenAt;
        public final int durationSec;
        public final int total;
        public final int score;
        public final boolean passed;

        public Result(int id, int userId, String examCode, String takenAt, int durationSec,
                      int total, int score, boolean passed) {
            this.id = id;
            this.userId = userId;
            this.examCode = examCode;
            this.takenAt = takenAt;
            this.durationSec = durationSec;
            this.total = total;
            this.score = score;
            this.passed = passed;
        }
    }

    /** Chi tiết một câu trong lần thi: nội dung câu hỏi (bản chụp), đáp án chọn và đáp án đúng. */
    public static class AnswerDetail {
        public final int qNo;
        public final int questionId;
        public final String content;
        public final String[] options;
        public final int correct; // 1..4
        public final int chosen;  // 0 = bỏ trống

        public AnswerDetail(int qNo, int questionId, String content, String[] options, int correct, int chosen) {
            this.qNo = qNo;
            this.questionId = questionId;
            this.content = content;
            this.options = options;
            this.correct = correct;
            this.chosen = chosen;
        }

        public boolean isCorrect() { return chosen == correct; }
    }

    /** Thống kê theo từng người dùng. */
    public static class UserStat {
        public final int userId;
        public final String username;
        public final String fullName;
        public final int attempts;
        public final int passes;
        public final int best;

        public UserStat(int userId, String username, String fullName, int attempts, int passes, int best) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.attempts = attempts;
            this.passes = passes;
            this.best = best;
        }

        /** "Chưa thi", "Đạt" (có ít nhất một lần đạt) hoặc "Chưa đạt". */
        public String status() {
            if (attempts == 0) return "Chưa thi";
            return passes > 0 ? "Đạt" : "Chưa đạt";
        }
    }

    /** Lưu kết quả và chi tiết từng câu trong một giao dịch; trả về id kết quả. */
    public int save(int userId, String examCode, String takenAt, int durationSec, int total, int score,
                    boolean passed, List<AnswerDetail> details) throws SQLException {
        String sqlR = "INSERT INTO results(user_id, exam_code, taken_at, duration_sec, total, score, passed) "
                + "VALUES (?,?,?,?,?,?,?)";
        String sqlA = "INSERT INTO result_answers(result_id, q_no, question_id, content, option_a, option_b, "
                + "option_c, option_d, correct, chosen) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(sqlR, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setString(2, examCode);
                    ps.setString(3, takenAt);
                    ps.setInt(4, durationSec);
                    ps.setInt(5, total);
                    ps.setInt(6, score);
                    ps.setInt(7, passed ? 1 : 0);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không lấy được id kết quả");
                        id = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(sqlA)) {
                    for (AnswerDetail d : details) {
                        ps.setInt(1, id);
                        ps.setInt(2, d.qNo);
                        ps.setInt(3, d.questionId);
                        ps.setString(4, d.content);
                        for (int i = 0; i < 4; i++) ps.setString(5 + i, d.options[i]);
                        ps.setInt(9, d.correct);
                        ps.setInt(10, d.chosen);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return id;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private Result map(ResultSet rs) throws SQLException {
        return new Result(rs.getInt("id"), rs.getInt("user_id"), rs.getString("exam_code"),
                rs.getString("taken_at"), rs.getInt("duration_sec"), rs.getInt("total"),
                rs.getInt("score"), rs.getInt("passed") == 1);
    }

    /** Lịch sử thi của một người dùng, mới nhất lên đầu. */
    public List<Result> findByUser(int userId) throws SQLException {
        List<Result> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM results WHERE user_id=? ORDER BY id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<AnswerDetail> findAnswers(int resultId) throws SQLException {
        List<AnswerDetail> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM result_answers WHERE result_id=? ORDER BY q_no")) {
            ps.setInt(1, resultId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AnswerDetail(rs.getInt("q_no"), rs.getInt("question_id"), rs.getString("content"),
                            new String[] { rs.getString("option_a"), rs.getString("option_b"),
                                    rs.getString("option_c"), rs.getString("option_d") },
                            rs.getInt("correct"), rs.getInt("chosen")));
                }
            }
        }
        return list;
    }

    /** Thống kê từng người dùng thường (không tính admin). */
    public List<UserStat> userStats() throws SQLException {
        List<UserStat> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.full_name, COUNT(r.id) AS attempts, "
                + "COALESCE(SUM(r.passed),0) AS passes, COALESCE(MAX(r.score),0) AS best "
                + "FROM users u LEFT JOIN results r ON r.user_id = u.id "
                + "WHERE u.role = 'USER' GROUP BY u.id ORDER BY u.id";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new UserStat(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"),
                        rs.getInt("attempts"), rs.getInt("passes"), rs.getInt("best")));
            }
        }
        return list;
    }

    /** Toàn bộ lượt thi kèm tên người dùng, dùng để xuất file. */
    public List<String[]> allResultsForExport() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT r.exam_code, u.username, u.full_name, r.taken_at, r.duration_sec, r.score, r.total, "
                + "r.passed FROM results r JOIN users u ON u.id = r.user_id ORDER BY r.id";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        String.valueOf(rs.getInt(5)), String.valueOf(rs.getInt(6)), String.valueOf(rs.getInt(7)),
                        rs.getInt(8) == 1 ? "Đạt" : "Không đạt" });
            }
        }
        return rows;
    }

    public int countAttempts() throws SQLException {
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM results")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void deleteAll() throws SQLException {
        try (Connection c = Database.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM result_answers");
            st.executeUpdate("DELETE FROM results");
        }
    }
}
