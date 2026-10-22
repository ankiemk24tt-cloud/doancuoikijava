import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Thống kê cho admin: số người dùng, người thi đạt / không đạt, biểu đồ, danh sách và xuất file. */
public class AdminStatsPanel extends JPanel {
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final ResultDAO resultDAO = new ResultDAO();

    private final JLabel lblUsers = card("Tổng người dùng");
    private final JLabel lblTaken = card("Đã thi");
    private final JLabel lblPassed = card("Thi qua (đạt)");
    private final JLabel lblFailed = card("Thi không qua");
    private final JLabel lblNotTaken = card("Chưa thi");
    private final JLabel lblAttempts = card("Tổng lượt thi");

    private final BarChart statusChart = new BarChart("Người dùng theo kết quả");
    private final BarChart categoryChart = new BarChart("Số câu hỏi theo chủ đề");

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Tên đăng nhập", "Họ tên", "Số lần thi", "Số lần đạt", "Điểm cao nhất", "Trạng thái" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private List<ResultDAO.UserStat> lastStats = new java.util.ArrayList<>();

    public AdminStatsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel north = new JPanel(new BorderLayout(6, 6));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = new JButton("Cập nhật");
        JButton btnExpStats = new JButton("Xuất thống kê người dùng (CSV)");
        JButton btnExpResults = new JButton("Xuất kết quả thi (CSV)");
        JButton btnClear = new JButton("Xóa toàn bộ lịch sử thi");
        buttons.add(btnRefresh);
        buttons.add(btnExpStats);
        buttons.add(btnExpResults);
        buttons.add(btnClear);
        north.add(buttons, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 6, 8, 0));
        for (JLabel l : new JLabel[] { lblUsers, lblTaken, lblPassed, lblFailed, lblNotTaken, lblAttempts }) {
            cards.add(l);
        }
        north.add(cards, BorderLayout.CENTER);
        north.add(new JLabel("<html><small>Đạt = có ít nhất 1 lần thi đạt. Không qua = đã thi nhưng chưa lần nào đạt. "
                + "Chưa thi = đã đăng ký nhưng chưa thi lần nào.</small></html>"), BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 0));
        charts.add(statusChart);
        charts.add(categoryChart);

        JTable table = new JTable(model);
        table.setRowHeight(22);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder("Danh sách người dùng"));

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.add(charts);
        center.add(sp);
        add(center, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> refresh());
        btnExpStats.addActionListener(e -> {
            File f = UiUtil.chooseSaveFile(this, "thongke_nguoidung.csv");
            if (f == null) return;
            try {
                ExportService.exportUserStats(f, resultDAO.userStats());
                JOptionPane.showMessageDialog(this, "Đã xuất: " + f.getAbsolutePath());
            } catch (Exception ex) { showError(ex); }
        });
        btnExpResults.addActionListener(e -> {
            File f = UiUtil.chooseSaveFile(this, "ketqua_thi.csv");
            if (f == null) return;
            try {
                ExportService.exportResults(f, resultDAO);
                JOptionPane.showMessageDialog(this, "Đã xuất: " + f.getAbsolutePath());
            } catch (Exception ex) { showError(ex); }
        });
        btnClear.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Xóa toàn bộ lịch sử thi của TẤT CẢ người dùng? Thao tác này không hoàn tác được.",
                    "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
            try {
                resultDAO.deleteAll();
                refresh();
            } catch (SQLException ex) { showError(ex); }
        });
        refresh();
    }

    private static JLabel card(String title) {
        JLabel l = new JLabel("", JLabel.CENTER);
        l.setBorder(BorderFactory.createTitledBorder(title));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 22f));
        return l;
    }

    public void refresh() {
        try {
            lastStats = resultDAO.userStats();
            int users = lastStats.size(), passed = 0, failed = 0, notTaken = 0;
            model.setRowCount(0);
            for (ResultDAO.UserStat s : lastStats) {
                String st = s.status();
                if (st.equals("Đạt")) passed++;
                else if (st.equals("Chưa đạt")) failed++;
                else notTaken++;
                model.addRow(new Object[] { s.username, s.fullName, s.attempts, s.passes, s.best,
                        st.equals("Chưa đạt") ? "Không qua" : st });
            }
            lblUsers.setText(String.valueOf(users));
            lblTaken.setText(String.valueOf(users - notTaken));
            lblPassed.setText(String.valueOf(passed));
            lblFailed.setText(String.valueOf(failed));
            lblNotTaken.setText(String.valueOf(notTaken));
            lblAttempts.setText(String.valueOf(resultDAO.countAttempts()));
            lblPassed.setForeground(new Color(27, 127, 42));
            lblFailed.setForeground(new Color(192, 57, 43));

            statusChart.setData(new String[] { "Thi qua", "Không qua", "Chưa thi" },
                    new int[] { passed, failed, notTaken }, 0);
            Map<String, Integer> byCat = questionDAO.countByCategory();
            categoryChart.setData(byCat.keySet().toArray(new String[0]),
                    byCat.values().stream().mapToInt(Integer::intValue).toArray(), 0);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Biểu đồ cột tự vẽ bằng Java2D (không cần thư viện ngoài). */
    private static class BarChart extends JPanel {
        private final String title;
        private String[] labels = new String[0];
        private int[] values = new int[0];
        private int fixedMax;

        BarChart(String title) {
            this.title = title;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            setPreferredSize(new Dimension(400, 220));
        }

        void setData(String[] labels, int[] values, int fixedMax) {
            this.labels = labels;
            this.values = values;
            this.fixedMax = fixedMax;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g.getFontMetrics();

            g.setColor(Color.BLACK);
            g.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 20);

            int sum = 0;
            for (int v : values) sum += v;
            if (values.length == 0 || sum == 0) {
                String s = "Chưa có dữ liệu";
                g.setColor(Color.GRAY);
                g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, getHeight() / 2);
                return;
            }

            int left = 40, right = 15, top = 35, bottom = 40;
            int w = getWidth() - left - right;
            int h = getHeight() - top - bottom;
            int max = fixedMax;
            for (int v : values) max = Math.max(max, v);
            if (max == 0) max = 1;

            g.setColor(Color.GRAY);
            g.drawLine(left, top, left, top + h);
            g.drawLine(left, top + h, left + w, top + h);

            int n = values.length;
            int slot = w / n;
            int barW = Math.max(10, slot - 14);
            for (int i = 0; i < n; i++) {
                int bh = (int) ((double) values[i] / max * h);
                int x = left + i * slot + (slot - barW) / 2;
                int y = top + h - bh;
                g.setColor(new Color(52, 120, 200));
                g.fillRect(x, y, barW, bh);
                g.setColor(Color.BLACK);
                String v = String.valueOf(values[i]);
                g.drawString(v, x + (barW - fm.stringWidth(v)) / 2, y - 4);

                String lb = labels[i];
                while (fm.stringWidth(lb) > slot && lb.length() > 3) lb = lb.substring(0, lb.length() - 2);
                g.drawString(lb, left + i * slot + (slot - fm.stringWidth(lb)) / 2, top + h + 16);
            }
        }
    }
}
