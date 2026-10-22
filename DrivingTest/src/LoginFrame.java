import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

public class LoginFrame extends JFrame {
    private final UserDAO userDAO = new UserDAO();

    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField txtLoginUser = new JTextField(18);
    private final JPasswordField txtLoginPass = new JPasswordField(18);

    private final JTextField txtRegName = new JTextField(18);
    private final JTextField txtRegUser = new JTextField(18);
    private final JPasswordField txtRegPass = new JPasswordField(18);
    private final JPasswordField txtRegPass2 = new JPasswordField(18);

    public LoginFrame() {
        super("Hệ thống thi thử bằng lái xe - Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel title = new JLabel("THI THỬ BẰNG LÁI XE", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        title.setBorder(BorderFactory.createEmptyBorder(14, 10, 4, 10));

        tabs.addTab("Đăng nhập", buildLoginTab());
        tabs.addTab("Đăng ký", buildRegisterTab());

        setLayout(new BorderLayout());
        add(title, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        pack();
        setSize(Math.max(getWidth(), 420), Math.max(getHeight(), 360));
        setLocationRelativeTo(null);
    }

    private JPanel buildLoginTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        row(p, g, 0, "Tên đăng nhập:", txtLoginUser);
        row(p, g, 1, "Mật khẩu:", txtLoginPass);
        JButton btn = new JButton("Đăng nhập");
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        p.add(btn, g);
        btn.addActionListener(e -> doLogin());
        txtLoginPass.addActionListener(e -> doLogin());
        txtLoginUser.addActionListener(e -> txtLoginPass.requestFocus());
        return p;
    }

    private JPanel buildRegisterTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        row(p, g, 0, "Họ và tên:", txtRegName);
        row(p, g, 1, "Tên đăng nhập:", txtRegUser);
        row(p, g, 2, "Mật khẩu:", txtRegPass);
row(p, g, 3, "Nhập lại mật khẩu:", txtRegPass2);
        JButton btn = new JButton("Đăng ký");
        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        p.add(btn, g);
        JLabel hint = new JLabel("<html><small>Tên đăng nhập: 2-50 ký tự (cho phép chữ có dấu, in hoa/thường, số, khoảng trắng và . _ - @ # $).</small></html>");
        g.gridy = 5;
        p.add(hint, g);
        btn.addActionListener(e -> doRegister());
        return p;
    }

    private void row(JPanel p, GridBagConstraints g, int r, String label, java.awt.Component c) {
        g.gridy = r;
        g.gridx = 0; g.gridwidth = 1; g.weightx = 0;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1;
        p.add(c, g);
    }

    private void doLogin() {
        String u = txtLoginUser.getText().trim();
        String pw = new String(txtLoginPass.getPassword());
        if (u.isEmpty() || pw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        try {
            User user = userDAO.login(u, pw);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Sai tên đăng nhập hoặc mật khẩu.", "Đăng nhập thất bại",
                        JOptionPane.ERROR_MESSAGE);
                txtLoginPass.setText("");
                return;
            }
            dispose();
            JFrame next = user.isAdmin() ? new AdminFrame(user) : new UserFrame(user);
            next.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegister() {
        String name = txtRegName.getText().trim();
        String user = txtRegUser.getText().trim();
        String p1 = new String(txtRegPass.getPassword());
        String p2 = new String(txtRegPass2.getPassword());
        String err = null;

        String userPattern = "[\\p{L}\\p{N}._\\-@#$ ]{2,50}";

        if (name.isEmpty()) {
            err = "Vui lòng nhập họ và tên.";
        } else if (!user.matches(userPattern)) {
            err = "Tên đăng nhập phải từ 2-50 ký tự (chấp nhận tiếng Việt có dấu, chữ hoa/thường, số, khoảng trắng và . _ - @ # $).";
        } else if (p1.length() < 6) {
            err = "Mật khẩu phải có ít nhất 6 ký tự.";
        } else if (!p1.equals(p2)) {
            err = "Mật khẩu nhập lại không khớp.";
        }

        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Thông tin chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            userDAO.register(user, p1, name);
        } catch (UserDAO.DuplicateUserException ex) {
JOptionPane.showMessageDialog(this, ex.getMessage(), "Không đăng ký được", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy đăng nhập.");
        txtLoginUser.setText(user);
        txtLoginPass.setText("");
        txtRegName.setText(""); txtRegUser.setText(""); txtRegPass.setText(""); txtRegPass2.setText("");
        tabs.setSelectedIndex(0);
        txtLoginPass.requestFocus();
    }
}
