import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Lịch sử thi của chính người dùng đang đăng nhập; xem lại đáp án từng lần thi. */
public class HistoryPanel extends JPanel {
    private final User user;
    private final ResultDAO dao = new ResultDAO();
    private List<ResultDAO.Result> results = new java.util.ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Lần", "Mã bài thi", "Thời điểm nộp", "Thời gian làm", "Điểm", "Kết quả" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblSummary = new JLabel(" ");

    public HistoryPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnView = new JButton("Xem chi tiết đáp án");
        JButton btnRefresh = new JButton("Làm mới");
        top.add(btnView);
        top.add(btnRefresh);
        top.add(lblSummary);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JLabel("Chọn một lần thi rồi bấm \"Xem chi tiết đáp án\" (hoặc nhấp đúp) để xem lại từng câu."),
                BorderLayout.SOUTH);

        btnView.addActionListener(e -> openSelected());
        btnRefresh.addActionListener(e -> refresh());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });
        refresh();
    }

    public void refresh() {
        try {
            results = dao.findByUser(user.getId());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        model.setRowCount(0);
        int passed = 0, best = 0;
        for (int i = 0; i < results.size(); i++) {
            ResultDAO.Result r = results.get(i);
            model.addRow(new Object[] { results.size() - i, r.examCode, r.takenAt, UiUtil.formatDuration(r.durationSec),
                    r.score + " / " + r.total, r.passed ? "Đạt" : "Không đạt" });
            if (r.passed) passed++;
            best = Math.max(best, r.score);
        }
        lblSummary.setText(results.isEmpty() ? "  Bạn chưa có lần thi nào."
                : String.format("  Số lần thi: %d  |  Đạt: %d  |  Không đạt: %d  |  Điểm cao nhất: %d",
                        results.size(), passed, results.size() - passed, best));
    }

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một lần thi trong bảng.");
            return;
        }
        ResultDAO.Result r = results.get(table.convertRowIndexToModel(row));
        try {
            List<ResultDAO.AnswerDetail> details = dao.findAnswers(r.id);
            new ReviewDialog(SwingUtilities.getWindowAncestor(this), r, details).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
