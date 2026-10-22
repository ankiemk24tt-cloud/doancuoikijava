import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Điểm khởi chạy của ứng dụng Thi thử bằng lái xe.
 * Tài khoản admin mặc định (tạo ở lần chạy đầu): admin / admin123.
 */
public class App {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        try {
            Class.forName("org.sqlite.JDBC");
            Database.init();
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                    "Thiếu thư viện sqlite-jdbc. Hãy thêm file .jar vào Build Path (xem README.md).",
                    "Thiếu thư viện", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Không khởi tạo được CSDL: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
