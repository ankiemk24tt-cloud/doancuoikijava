import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/** Giao diện quản trị: quản lý câu hỏi, thống kê người dùng, xuất dữ liệu. */
public class AdminFrame extends JFrame {

    public AdminFrame(User admin) {
        super("Hệ thống thi thử bằng lái xe - Quản trị");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 700);
        setLocationRelativeTo(null);

        QuestionPanel questions = new QuestionPanel();
        AdminStatsPanel stats = new AdminStatsPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Quản lý câu hỏi", questions);
        tabs.addTab("Thống kê người dùng", stats);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == stats) stats.refresh();
        });

        setLayout(new BorderLayout());
        add(UiUtil.header(admin, () -> { dispose(); new LoginFrame().setVisible(true); }), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
