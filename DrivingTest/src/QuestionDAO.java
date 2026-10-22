import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thêm / sửa / xóa / tìm kiếm câu hỏi trong CSDL. */
public class QuestionDAO {

    private Question map(ResultSet rs) throws SQLException {
        return new Question(
                rs.getInt("id"),
                rs.getString("content"),
                rs.getString("option_a"),
                rs.getString("option_b"),
                rs.getString("option_c"),
                rs.getString("option_d"),
                rs.getInt("correct"),
                rs.getString("category"));
    }

    public List<Question> findAll() throws SQLException {
        return search("");
    }

    /** Tìm theo từ khóa trong nội dung câu hỏi hoặc chủ đề. */
    public List<Question> search(String keyword) throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE content LIKE ? OR category LIKE ? ORDER BY id";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String k = "%" + keyword.trim() + "%";
            ps.setString(1, k);
            ps.setString(2, k);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Question> findRandom(int n) throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM questions ORDER BY RANDOM() LIMIT ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public int count() throws SQLException {
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM questions")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void insert(Question q) throws SQLException {
        String sql = "INSERT INTO questions(content, option_a, option_b, option_c, option_d, correct, category) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, q);
            ps.executeUpdate();
        }
    }

    public void update(Question q) throws SQLException {
        String sql = "UPDATE questions SET content=?, option_a=?, option_b=?, option_c=?, option_d=?, "
                + "correct=?, category=? WHERE id=?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, q);
            ps.setInt(8, q.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM questions WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Thống kê số câu hỏi theo chủ đề. */
    public Map<String, Integer> countByCategory() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT category, COUNT(*) AS n FROM questions GROUP BY category ORDER BY category";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("category"), rs.getInt("n"));
        }
        return map;
    }

    private void fill(PreparedStatement ps, Question q) throws SQLException {
        ps.setString(1, q.getContent());
        ps.setString(2, q.getOptionA());
        ps.setString(3, q.getOptionB());
        ps.setString(4, q.getOptionC());
        ps.setString(5, q.getOptionD());
        ps.setInt(6, q.getCorrect());
        ps.setString(7, q.getCategory());
    }
}
