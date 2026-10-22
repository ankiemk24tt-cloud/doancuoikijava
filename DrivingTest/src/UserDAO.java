import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Đăng ký, đăng nhập và đếm tài khoản. Mật khẩu được băm SHA-256 kèm salt ngẫu nhiên. */
public class UserDAO {

    /** Ném khi đăng ký trùng tên đăng nhập. */
    public static class DuplicateUserException extends Exception {
        public DuplicateUserException(String msg) { super(msg); }
    }

    static String newSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return toHex(b);
    }

    static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return toHex(md.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /** Thêm tài khoản bằng kết nối có sẵn (dùng cả khi khởi tạo CSDL). */
    static void insert(Connection c, String username, String password, String fullName, String role)
            throws SQLException {
        String salt = newSalt();
        String sql = "INSERT INTO users(username, password_hash, salt, full_name, role, created_at) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hash(salt, password));
            ps.setString(3, salt);
            ps.setString(4, fullName.trim());
            ps.setString(5, role);
            ps.setString(6, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            ps.executeUpdate();
        }
    }

    public boolean exists(String username) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username=?")) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Đăng ký tài khoản người dùng thường (không thể tự đăng ký quyền admin). */
    public void register(String username, String password, String fullName)
            throws SQLException, DuplicateUserException {
        if (exists(username)) throw new DuplicateUserException("Tên đăng nhập đã tồn tại.");
        try (Connection c = Database.getConnection()) {
            insert(c, username, password, fullName, User.USER);
        }
    }

    /** Trả về User nếu đúng tài khoản/mật khẩu, ngược lại trả về null. */
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password_hash, salt, full_name, role FROM users WHERE username=?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String expected = rs.getString("password_hash");
                if (!expected.equals(hash(rs.getString("salt"), password))) return null;
                return new User(rs.getInt("id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("role"));
            }
        }
    }
}
