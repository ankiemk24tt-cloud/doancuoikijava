import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

/** Giao diện của người dùng thường: thi thử và xem lịch sử thi của bản thân. */
public class UserFrame extends JFrame {
    private final ExamPanel exam;

    public UserFrame(User user) {
        super("Hệ thống thi thử bằng lái xe - Người dùng");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        HistoryPanel history = new HistoryPanel(user);
        exam = new ExamPanel(user, history::refresh);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Thi thử", exam);
        tabs.addTab("Lịch sử thi của tôi", history);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == history) history.refresh();
        });

        setLayout(new BorderLayout());
        add(UiUtil.header(user, this::logout), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private void logout() {
        if (exam.isRunning()) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Bài thi đang làm dở sẽ bị hủy (không được chấm). Vẫn đăng xuất?", "Đăng xuất",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }
        exam.cancel();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
