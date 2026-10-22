public class Question {
    private int id;
    private String content;
    private String optionA, optionB, optionC, optionD;
    private int correct; // 1 = A, 2 = B, 3 = C, 4 = D
    private String category;

    public Question() {}

    public Question(int id, String content, String a, String b, String c, String d, int correct, String category) {
        this.id = id;
        this.content = content;
        this.optionA = a;
        this.optionB = b;
        this.optionC = c;
        this.optionD = d;
        this.correct = correct;
        this.category = category;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public int getCorrect() { return correct; }
    public void setCorrect(int correct) { this.correct = correct; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String[] getOptions() {
        return new String[] { optionA, optionB, optionC, optionD };
    }

    public static String correctLabel(int correct) {
        return correct >= 1 && correct <= 4 ? String.valueOf((char) ('A' + correct - 1)) : "?";
    }
}
