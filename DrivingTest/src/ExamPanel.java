import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Màn hình thi thử: random câu hỏi, đếm ngược thời gian, chấm điểm. */
public class ExamPanel extends JPanel {
    public static final int NUM_QUESTIONS = 25;   // số câu random từ ngân hàng 100 câu
    public static final int PASS_SCORE = 21;
    public static final int MINUTES = 19;

    private final User user;
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final ResultDAO resultDAO = new ResultDAO();
    private final Runnable onFinished;

    private List<Question> questions = new ArrayList<>();
    private int[] answers;       // đáp án đã chọn cho từng câu (0 = chưa chọn)
    private int current = 0;
    private int secondsLeft;
    private boolean running = false;
    private String examCode;
    private Timer timer;

    private final JLabel lblInfo = new JLabel(" ");
    private final JLabel lblTimer = new JLabel("", JLabel.RIGHT);
    private final JTextArea txtQuestion = new JTextArea();
    private final JRadioButton[] rbs = new JRadioButton[4];
    private final ButtonGroup group = new ButtonGroup();
    private final JButton btnStart = new JButton("Bắt đầu thi");
    private final JButton btnPrev = new JButton("< Câu trước");
    private final JButton btnNext = new JButton("Câu sau >");
    private final JButton btnSubmit = new JButton("Nộp bài");

    public ExamPanel(User user, Runnable onFinished) {
        this.user = user;
        this.onFinished = onFinished;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.BOLD, 15f));
        lblTimer.setFont(lblTimer.getFont().deriveFont(Font.BOLD, 15f));
        JPanel top = new JPanel(new BorderLayout());
        top.add(lblInfo, BorderLayout.WEST);
        top.add(lblTimer, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        txtQuestion.setEditable(false);
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setFont(txtQuestion.getFont().deriveFont(Font.BOLD, 16f));
        txtQuestion.setBackground(getBackground());

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(txtQuestion);
        center.add(javax.swing.Box.createVerticalStrut(12));
        for (int i = 0; i < 4; i++) {
            rbs[i] = new JRadioButton();
            rbs[i].setFont(rbs[i].getFont().deriveFont(14f));
            final int idx = i + 1;
            rbs[i].addActionListener(e -> answers[current] = idx);
            group.add(rbs[i]);
            center.add(rbs[i]);
            center.add(javax.swing.Box.createVerticalStrut(6));
        }
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bottom.add(btnStart);
        bottom.add(btnPrev);
        bottom.add(btnNext);
        bottom.add(btnSubmit);
        add(bottom, BorderLayout.SOUTH);

        btnStart.addActionListener(e -> start());
        btnPrev.addActionListener(e -> go(current - 1));
        btnNext.addActionListener(e -> go(current + 1));
        btnSubmit.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Bạn chắc chắn muốn nộp bài?", "Nộp bài",
                    JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) finish();
        });

        showIdle();
    }

    public boolean isRunning() { return running; }

    /** Hủy bài đang làm (không chấm, không lưu), ví dụ khi đăng xuất. */
    public void cancel() {
        if (timer != null) timer.stop();
        running = false;
    }

    private void showIdle() {
        running = false;
        lblInfo.setText("Thi thử bằng lái xe: " + NUM_QUESTIONS + " câu, " + MINUTES
                + " phút, đạt từ " + PASS_SCORE + " câu đúng.");
        lblTimer.setText("");
        txtQuestion.setText("Nhấn \"Bắt đầu thi\" để bắt đầu.");
        for (JRadioButton rb : rbs) { rb.setVisible(false); }
        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);
        btnSubmit.setEnabled(false);
        btnStart.setEnabled(true);
    }

    private void start() {
        try {
            questions = questionDAO.findRandom(NUM_QUESTIONS);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ngân hàng câu hỏi đang trống. Hãy thêm câu hỏi trước.");
            return;
        }
        answers = new int[questions.size()];
        examCode = ExportService.newExamCode() + "-U" + user.getId();
        running = true;
        secondsLeft = MINUTES * 60;
        btnStart.setEnabled(false);
        btnSubmit.setEnabled(true);
        for (JRadioButton rb : rbs) { rb.setVisible(true); }

        if (timer != null) timer.stop();
        timer = new Timer(1000, e -> {
            secondsLeft--;
            updateTimer();
            if (secondsLeft <= 0) {
                timer.stop();
                JOptionPane.showMessageDialog(this, "Hết giờ! Hệ thống tự động nộp bài.");
                finish();
            }
        });
        timer.start();
        updateTimer();
        go(0);
    }

    private void updateTimer() {
        lblTimer.setText(String.format("Còn lại: %02d:%02d", secondsLeft / 60, secondsLeft % 60));
        lblTimer.setForeground(secondsLeft <= 60 ? Color.RED : Color.BLACK);
    }

    private void go(int index) {
        if (index < 0 || index >= questions.size()) return;
        current = index;
        Question q = questions.get(index);
        lblInfo.setText("Câu " + (index + 1) + " / " + questions.size());
        txtQuestion.setText(q.getContent());
        String[] opts = q.getOptions();
        group.clearSelection();
        for (int i = 0; i < 4; i++) {
            rbs[i].setText((char) ('A' + i) + ". " + opts[i]);
            if (answers[index] == i + 1) rbs[i].setSelected(true);
        }
        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < questions.size() - 1);
    }

    private void finish() {
        if (timer != null) timer.stop();
        running = false;
        int score = 0;
        List<ResultDAO.AnswerDetail> details = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (answers[i] == q.getCorrect()) score++;
            details.add(new ResultDAO.AnswerDetail(i + 1, q.getId(), q.getContent(), q.getOptions(),
                    q.getCorrect(), answers[i]));
        }
        int total = questions.size();
        // Đạt khi đúng từ PASS_SCORE/NUM_QUESTIONS câu trở lên (21/25 như đề thi hạng A1).
        boolean passed = total > 0 && score * NUM_QUESTIONS >= total * PASS_SCORE;
        int duration = MINUTES * 60 - Math.max(0, secondsLeft);
        String takenAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        ResultDAO.Result saved = null;
        try {
            int id = resultDAO.save(user.getId(), examCode, takenAt, duration, total, score, passed, details);
            saved = new ResultDAO.Result(id, user.getId(), examCode, takenAt, duration, total, score, passed);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được kết quả: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        if (saved == null) {
            saved = new ResultDAO.Result(0, user.getId(), examCode, takenAt, duration, total, score, passed);
        }

        showIdle();
        if (onFinished != null) onFinished.run();
        new ReviewDialog(SwingUtilities.getWindowAncestor(this), saved, details).setVisible(true);
    }
}
