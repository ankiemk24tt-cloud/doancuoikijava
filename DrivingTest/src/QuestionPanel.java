import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Quản lý ngân hàng câu hỏi: hiển thị, thêm, sửa, xóa, tìm kiếm. */
public class QuestionPanel extends JPanel {
    private final QuestionDAO dao = new QuestionDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Nội dung", "A", "B", "C", "D", "Đáp án", "Chủ đề" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JTextField txtSearch = new JTextField(20);
    private final JTextArea txtContent = new JTextArea(3, 30);
    private final JTextField txtA = new JTextField();
    private final JTextField txtB = new JTextField();
    private final JTextField txtC = new JTextField();
    private final JTextField txtD = new JTextField();
    private final JComboBox<String> cbCorrect = new JComboBox<>(new String[] { "A", "B", "C", "D" });
    // Có thể gõ thêm chủ đề mới ngoài danh sách gợi ý.
    private final JComboBox<String> cbCategory = new JComboBox<>(new String[] {
            "Biển báo", "Quy tắc giao thông", "Tốc độ", "Kỹ thuật lái xe", "Văn hóa & đạo đức",
            "Giấy phép & phương tiện", "Cấu tạo & bảo dưỡng" });

    private int selectedId = -1;

    public QuestionPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Thanh tìm kiếm ---
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSearch = new JButton("Tìm kiếm");
        JButton btnAll = new JButton("Hiện tất cả");
        top.add(new JLabel("Từ khóa:"));
        top.add(txtSearch);
        top.add(btnSearch);
        top.add(btnAll);
        JButton btnExportQ = new JButton("Xuất câu hỏi (CSV)");
        JButton btnExportExam = new JButton("Xuất đề thi mẫu (TXT)");
        top.add(javax.swing.Box.createHorizontalStrut(30));
        top.add(btnExportQ);
        top.add(btnExportExam);
        add(top, BorderLayout.NORTH);

        // --- Bảng dữ liệu ---
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Form nhập liệu ---
        txtContent.setLineWrap(true);
        txtContent.setWrapStyleWord(true);
        cbCategory.setEditable(true);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Thông tin câu hỏi"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, g, 0, "Nội dung:", new JScrollPane(txtContent));
        addRow(form, g, 1, "Đáp án A:", txtA);
        addRow(form, g, 2, "Đáp án B:", txtB);
        addRow(form, g, 3, "Đáp án C:", txtC);
        addRow(form, g, 4, "Đáp án D:", txtD);
        addRow(form, g, 5, "Đáp án đúng:", cbCorrect);
        addRow(form, g, 6, "Chủ đề:", cbCategory);

        JButton btnAdd = new JButton("Thêm");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDelete = new JButton("Xóa");
        JButton btnClear = new JButton("Làm mới form");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btns.add(btnAdd);
        btns.add(btnEdit);
        btns.add(btnDelete);
        btns.add(btnClear);
        g.gridx = 0; g.gridy = 7; g.gridwidth = 2;
        form.add(btns, g);

        JPanel right = new JPanel(new BorderLayout());
        right.add(form, BorderLayout.NORTH);
        right.setPreferredSize(new java.awt.Dimension(420, 0));
        add(right, BorderLayout.EAST);

        // --- Sự kiện ---
        btnSearch.addActionListener(e -> load(txtSearch.getText()));
        txtSearch.addActionListener(e -> load(txtSearch.getText()));
        btnAll.addActionListener(e -> { txtSearch.setText(""); load(""); });
        btnExportQ.addActionListener(e -> onExportQuestions());
        btnExportExam.addActionListener(e -> onExportExam());
        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
        btnClear.addActionListener(e -> clearForm());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillFormFromTable();
        });

        load("");
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, java.awt.Component comp) {
        g.gridy = row;
        g.gridx = 0; g.gridwidth = 1; g.weightx = 0;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1;
        p.add(comp, g);
    }

    /** Nạp dữ liệu từ CSDL lên bảng. */
    public void load(String keyword) {
        try {
            List<Question> list = dao.search(keyword);
            model.setRowCount(0);
            for (Question q : list) {
                model.addRow(new Object[] { q.getId(), q.getContent(), q.getOptionA(), q.getOptionB(),
                        q.getOptionC(), q.getOptionD(), Question.correctLabel(q.getCorrect()), q.getCategory() });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void fillFormFromTable() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        selectedId = (Integer) model.getValueAt(r, 0);
        txtContent.setText(String.valueOf(model.getValueAt(r, 1)));
        txtA.setText(String.valueOf(model.getValueAt(r, 2)));
        txtB.setText(String.valueOf(model.getValueAt(r, 3)));
        txtC.setText(String.valueOf(model.getValueAt(r, 4)));
        txtD.setText(String.valueOf(model.getValueAt(r, 5)));
        cbCorrect.setSelectedItem(String.valueOf(model.getValueAt(r, 6)));
        cbCategory.setSelectedItem(String.valueOf(model.getValueAt(r, 7)));
    }

    private Question readForm() {
        String content = txtContent.getText().trim();
        String a = txtA.getText().trim(), b = txtB.getText().trim();
        String c = txtC.getText().trim(), d = txtD.getText().trim();
        if (content.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ nội dung và 4 đáp án.",
                    "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String cat = String.valueOf(cbCategory.getSelectedItem()).trim();
        if (cat.isEmpty() || cat.equals("null")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập chủ đề.",
                    "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new Question(selectedId, content, a, b, c, d, cbCorrect.getSelectedIndex() + 1, cat);
    }

    private void onAdd() {
        Question q = readForm();
        if (q == null) return;
        try {
            dao.insert(q);
            load(txtSearch.getText());
            clearForm();
            JOptionPane.showMessageDialog(this, "Đã thêm câu hỏi.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void onEdit() {
        if (selectedId < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một câu hỏi trong bảng để sửa.");
            return;
        }
        Question q = readForm();
        if (q == null) return;
        try {
            dao.update(q);
            load(txtSearch.getText());
            JOptionPane.showMessageDialog(this, "Đã cập nhật câu hỏi.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void onDelete() {
        if (selectedId < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một câu hỏi trong bảng để xóa.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Xóa câu hỏi đã chọn?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.delete(selectedId);
            load(txtSearch.getText());
            clearForm();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void onExportQuestions() {
        java.io.File f = UiUtil.chooseSaveFile(this, "ngan_hang_cau_hoi.csv");
        if (f == null) return;
        try {
            ExportService.exportQuestions(f, dao.findAll());
            JOptionPane.showMessageDialog(this, "Đã xuất: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Xuất một đề thi mẫu ngẫu nhiên kèm mã đề và phần đáp án. */
    private void onExportExam() {
        try {
            List<Question> list = dao.findRandom(ExamPanel.NUM_QUESTIONS);
            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ngân hàng câu hỏi đang trống.");
                return;
            }
            String code = ExportService.newExamCode();
            java.io.File f = UiUtil.chooseSaveFile(this, "de_thi_" + code + ".txt");
            if (f == null) return;
            ExportService.exportExam(f, code, list);
            JOptionPane.showMessageDialog(this, "Đã xuất đề thi mã " + code + ":\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedId = -1;
        txtContent.setText("");
        txtA.setText(""); txtB.setText(""); txtC.setText(""); txtD.setText("");
        cbCorrect.setSelectedIndex(0);
        cbCategory.setSelectedIndex(0);
        table.clearSelection();
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Lỗi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
