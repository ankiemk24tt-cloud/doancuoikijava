import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Xuất dữ liệu cho admin: ngân hàng câu hỏi, đề thi mẫu (có mã đề), thống kê, kết quả thi. */
public class ExportService {

    private static PrintWriter open(File f) throws IOException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8));
        w.print('﻿'); // BOM để Excel đọc đúng tiếng Việt
        return w;
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static void line(PrintWriter w, String... cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(csv(cols[i]));
        }
        w.print(sb);
        w.print("\r\n");
    }

    /** Mã đề dạng DE-yyyyMMdd-HHmmss. */
    public static String newExamCode() {
        return "DE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    public static void exportQuestions(File f, List<Question> list) throws IOException {
        try (PrintWriter w = open(f)) {
            line(w, "ID", "Nội dung", "A", "B", "C", "D", "Đáp án đúng", "Chủ đề");
            for (Question q : list) {
                line(w, String.valueOf(q.getId()), q.getContent(), q.getOptionA(), q.getOptionB(), q.getOptionC(),
                        q.getOptionD(), Question.correctLabel(q.getCorrect()), q.getCategory());
            }
        }
    }

    /** Xuất một đề thi (văn bản) gồm các câu hỏi và phần đáp án ở cuối. */
    public static void exportExam(File f, String examCode, List<Question> list) throws IOException {
        try (PrintWriter w = open(f)) {
            w.print("ĐỀ THI THỬ BẰNG LÁI XE\r\n");
            w.print("Mã đề: " + examCode + "\r\n");
            w.print("Số câu: " + list.size() + " - Thời gian: " + ExamPanel.MINUTES + " phút - Đạt từ "
                    + ExamPanel.PASS_SCORE + "/" + ExamPanel.NUM_QUESTIONS + " câu đúng\r\n");
            w.print("==============================================\r\n\r\n");
            int n = 1;
            for (Question q : list) {
                w.print("Câu " + n++ + ". " + q.getContent() + "\r\n");
                String[] o = q.getOptions();
                for (int i = 0; i < 4; i++) w.print("   " + (char) ('A' + i) + ". " + o[i] + "\r\n");
                w.print("\r\n");
            }
            w.print("---------------- ĐÁP ÁN (mã đề " + examCode + ") ----------------\r\n");
            n = 1;
            for (Question q : list) {
                w.print("Câu " + n++ + ": " + Question.correctLabel(q.getCorrect()) + "\r\n");
            }
        }
    }

    public static void exportUserStats(File f, List<ResultDAO.UserStat> stats) throws IOException {
        try (PrintWriter w = open(f)) {
            line(w, "Tên đăng nhập", "Họ tên", "Số lần thi", "Số lần đạt", "Điểm cao nhất", "Trạng thái");
            for (ResultDAO.UserStat s : stats) {
                line(w, s.username, s.fullName, String.valueOf(s.attempts), String.valueOf(s.passes),
                        String.valueOf(s.best), s.status());
            }
        }
    }

    public static void exportResults(File f, ResultDAO dao) throws IOException, SQLException {
        List<String[]> rows = dao.allResultsForExport();
        try (PrintWriter w = open(f)) {
            line(w, "Mã bài thi", "Tên đăng nhập", "Họ tên", "Thời điểm nộp", "Thời lượng (giây)", "Điểm",
                    "Tổng câu", "Kết quả");
            for (String[] r : rows) line(w, r);
        }
    }
}
