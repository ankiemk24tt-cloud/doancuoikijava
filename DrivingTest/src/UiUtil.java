import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Các hàm giao diện dùng chung. */
public class UiUtil {

    /** Thanh tiêu đề: lời chào + nút đăng xuất. */
    public static JPanel header(User user, Runnable onLogout) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        String role = user.isAdmin() ? "Quản trị viên" : "Người dùng";
        JLabel lbl = new JLabel("Xin chào, " + user.getFullName() + "  (" + user.getUsername() + " - " + role + ")");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        JButton btn = new JButton("Đăng xuất");
        btn.addActionListener(e -> onLogout.run());
        p.add(lbl, BorderLayout.WEST);
        p.add(btn, BorderLayout.EAST);
        return p;
    }

    /** Hộp thoại chọn nơi lưu file; trả về null nếu người dùng hủy. */
    public static File chooseSaveFile(Component parent, String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        return fc.getSelectedFile();
    }

    public static String formatDuration(int sec) {
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    public static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
