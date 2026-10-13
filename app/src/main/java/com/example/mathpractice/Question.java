package com.example.mathpractice;

public class Question {
    private final int num1;
    private final int num2;
    private final long answer;
    private boolean isCorrect;

    public Question(int num1, int num2) {
        this.num1 = num1;
        this.num2 = num2;
        this.answer = (long) num1 * num2;
    }

    public int getNum1() { return num1; }
    public int getNum2() { return num2; }
    public long getAnswer() { return answer; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public String getQuestionText() {
        return num1 + " × " + num2 + " = ?";
    }
}
