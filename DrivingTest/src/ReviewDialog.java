import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Xem kết quả một lần thi: điểm, đạt/không đạt và đáp án từng câu (bạn chọn / đáp án đúng). */
public class ReviewDialog extends JDialog {

    public ReviewDialog(Window owner, ResultDAO.Result r, List<ResultDAO.AnswerDetail> details) {
        super(owner, "Chi tiết bài thi " + r.examCode, ModalityType.APPLICATION_MODAL);
        setSize(1050, 650);
        setLocationRelativeTo(owner);

        JLabel head = new JLabel("<html><div style='font-size:14px'><b>Điểm: " + r.score + " / " + r.total
                + " &nbsp;-&nbsp; <span style='color:" + (r.passed ? "#1b7f2a'>ĐẠT" : "#c0392b'>KHÔNG ĐẠT") + "</span></b>"
                + "<br>Mã bài thi: " + UiUtil.escapeHtml(r.examCode) + " &nbsp;|&nbsp; Nộp lúc: " + r.takenAt
                + " &nbsp;|&nbsp; Thời gian làm: " + UiUtil.formatDuration(r.durationSec)
                + "</div></html>");
        head.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Câu", "Nội dung", "Bạn chọn", "Đáp án đúng", "Kết quả" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final int[] status = new int[details.size()]; // 1 đúng, 2 sai, 3 bỏ trống
        for (int i = 0; i < details.size(); i++) {
            ResultDAO.AnswerDetail d = details.get(i);
            String mine = d.chosen == 0 ? "(Bỏ trống)"
                    : Question.correctLabel(d.chosen) + ". " + d.options[d.chosen - 1];
            String right = Question.correctLabel(d.correct) + ". " + d.options[d.correct - 1];
            status[i] = d.chosen == 0 ? 3 : (d.isCorrect() ? 1 : 2);
            model.addRow(new Object[] { d.qNo, d.content, mine, right,
                    status[i] == 1 ? "Đúng" : status[i] == 2 ? "Sai" : "Bỏ trống" });
        }

        JTable table = new JTable(model);
        table.setRowHeight(68);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMaxWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(230);
        table.getColumnModel().getColumn(3).setPreferredWidth(230);
        table.getColumnModel().getColumn(4).setMaxWidth(80);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                String text = value == null ? "" : String.valueOf(value);
                if (col >= 1 && col <= 3) text = "<html>" + UiUtil.escapeHtml(text) + "</html>";
                JLabel c = (JLabel) super.getTableCellRendererComponent(t, text, sel, focus, row, col);
                c.setVerticalAlignment(JLabel.CENTER);
                if (!sel) {
                    int st = status[t.convertRowIndexToModel(row)];
                    c.setBackground(st == 1 ? new Color(222, 245, 222) : st == 2 ? new Color(252, 226, 226)
                            : new Color(255, 245, 210));
                }
                c.setToolTipText(text);
                if (col == 4) c.setFont(c.getFont().deriveFont(Font.BOLD));
                return c;
            }
        });

        JButton close = new JButton("Đóng");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.add(close);

        setLayout(new BorderLayout());
        add(head, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }
}
