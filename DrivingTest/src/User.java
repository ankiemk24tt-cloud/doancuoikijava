/** Tài khoản đăng nhập (người dùng thường hoặc quản trị viên). */
public class User {
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    private final int id;
    private final String username;
    private final String fullName;
    private final String role;

    public User(int id, String username, String fullName, String role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public boolean isAdmin() { return ADMIN.equals(role); }
}
