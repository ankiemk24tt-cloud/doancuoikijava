# Hệ thống thi thử bằng lái xe (Java Swing + SQLite)

Bài tập cuối kỳ – Lập trình hướng đối tượng (nhóm đề tài quản lý).

## Phân quyền

Khi chạy sẽ hiện màn hình **Đăng nhập / Đăng ký**. Tài khoản admin mặc định (tạo ở lần chạy đầu): `admin` / `admin123`.
Đăng ký mới luôn là **người dùng**, không thể tự tạo admin. Mật khẩu được băm SHA-256 + salt.

| Vai trò | Chức năng |
|---|---|
| Người dùng | Đăng ký, đăng nhập; **thi thử** (mỗi lần **25 câu random** từ ngân hàng 100 câu, đếm ngược **19 phút**, tự nộp khi hết giờ, đạt khi đúng **>= 21/25** như đề thi hạng A1); xem kết quả ngay sau khi thi cùng **đáp án từng câu** (bạn chọn / đáp án đúng); tab **Lịch sử thi của tôi** (chỉ thấy bài của mình, xem lại chi tiết từng lần) |
| Admin | **Quản lý câu hỏi** (thêm / sửa / xóa / tìm kiếm, có thể nhập chủ đề mới); **Thống kê**: tổng số người dùng, người thi qua, người thi không qua, người chưa thi, tổng lượt thi, biểu đồ, danh sách từng người; **Xuất dữ liệu** (xem dưới) |

Định nghĩa thống kê: *Thi qua* = có ít nhất 1 lần thi đạt; *Không qua* = đã thi nhưng chưa lần nào đạt; *Chưa thi* = đã đăng ký, chưa thi lần nào.

### Xuất dữ liệu (admin)
- Tab Quản lý câu hỏi: **Xuất câu hỏi (CSV)** và **Xuất đề thi mẫu (TXT)** (đề ngẫu nhiên có *mã đề* + phần đáp án).
- Tab Thống kê: **Xuất thống kê người dùng (CSV)** và **Xuất kết quả thi (CSV)** (mỗi lượt thi có *mã bài thi*).
- File CSV có BOM UTF-8 nên mở bằng Excel không lỗi tiếng Việt.

## Ngân hàng câu hỏi xe máy

`QuestionBank.java` chứa **100 câu hỏi thi bằng lái xe máy** (7 chủ đề: Biển báo 20, Quy tắc giao thông 25, Tốc độ 10, Kỹ thuật lái xe 14, Văn hóa & đạo đức 12, Giấy phép & phương tiện 10, Cấu tạo & bảo dưỡng 9). Đáp án đúng được chia đều 25 câu cho mỗi đáp án A/B/C/D.
- Nạp vào CSDL **một lần** ở lần chạy đầu (đánh dấu trong bảng `meta`), nên câu admin đã xóa/sửa sẽ không bị nạp lại.
- Với CSDL cũ: 10 câu mẫu cũ (chưa sửa) được thay bằng ngân hàng mới; câu do admin tự thêm vẫn được giữ.
- Mỗi lần thi: `ORDER BY RANDOM() LIMIT 25`. Đổi số câu / thời gian / điểm đạt ở 3 hằng số đầu `ExamPanel.java` (`NUM_QUESTIONS`, `MINUTES`, `PASS_SCORE`).
- Nội dung soạn theo quy định hiện hành (Luật Trật tự, an toàn giao thông đường bộ 2024, tốc độ theo Thông tư 38/2024/TT-BGTVT). Quy định có thể thay đổi, admin nên rà soát lại trước khi dùng chính thức.

## Cấu trúc

```
DrivingTest/
└── src/
    ├── App.java              // main: khởi tạo CSDL, mở màn hình đăng nhập
    ├── Database.java         // kết nối, tạo bảng, migrate CSDL cũ, dữ liệu mẫu, admin mặc định
    ├── User.java, UserDAO.java        // tài khoản, đăng ký / đăng nhập
    ├── Question.java, QuestionDAO.java // câu hỏi (CRUD, tìm kiếm, random)
    ├── QuestionBank.java     // ngân hàng 100 câu hỏi xe máy
    ├── ResultDAO.java        // lưu kết quả + chi tiết từng câu; thống kê theo người dùng
    ├── ExportService.java    // xuất CSV / TXT
    ├── LoginFrame.java       // đăng nhập + đăng ký
    ├── UserFrame.java        // giao diện người dùng: ExamPanel + HistoryPanel
    ├── AdminFrame.java       // giao diện admin: QuestionPanel + AdminStatsPanel
    ├── ExamPanel.java        // làm bài thi thử (đếm ngược)
    ├── ReviewDialog.java     // xem kết quả + đáp án từng câu
    ├── HistoryPanel.java     // lịch sử thi của người dùng
    ├── QuestionPanel.java    // quản lý câu hỏi (admin)
    ├── AdminStatsPanel.java  // thống kê (admin)
    └── UiUtil.java
```

Cơ sở dữ liệu: `users`, `questions`, `results` (có `user_id`, `exam_code`), `result_answers` (lưu bản chụp từng câu đã thi nên xem lại được dù admin sửa/xóa câu hỏi sau đó).
Nếu bạn đang có file `drivingtest.db` bản cũ, ứng dụng tự nâng cấp: giữ nguyên câu hỏi, các kết quả ẩn danh cũ được chuyển sang bảng `results_legacy`.

## Cách chạy trong Eclipse

1. Tải **sqlite-jdbc** (file `.jar`) từ https://github.com/xerial/sqlite-jdbc/releases
   (nếu báo thiếu SLF4J thì tải thêm `slf4j-api` và `slf4j-nop`, hoặc dùng bản jar cũ 3.36.0.3 không cần slf4j).
2. Trong Eclipse: **File > New > Java Project**, đặt tên `DrivingTest`.
3. Chép các file trong thư mục `src` vào thư mục `src` của project (kéo thả, hoặc chuột phải `src` > Paste).
4. Chuột phải project > **Build Path > Configure Build Path > Libraries > Classpath > Add External JARs...** rồi chọn file `sqlite-jdbc-*.jar`.
5. Chuột phải `App.java` > **Run As > Java Application**.

File CSDL `drivingtest.db` được tự tạo ở thư mục project lần chạy đầu tiên, kèm 10 câu hỏi mẫu và tài khoản `admin` / `admin123`.

## Đưa lên GitHub

```
git init
git add .
git commit -m "He thong thi thu bang lai xe"
git branch -M main
git remote add origin https://github.com/<ten-ban>/DrivingTest.git
git push -u origin main
```

Nên thêm `drivingtest.db` vào `.gitignore` nếu không muốn đẩy dữ liệu lên.
