package com.example.mathpractice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PracticeActivity extends AppCompatActivity {

    private static final int TOTAL_QUESTIONS = 10;

    private TextView tvProgress, tvQuestion, tvTimer, tvResult, tvAnswer;
    private Button btnSubmit, btnNext;
    private LinearLayout layoutResult;

    private final List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private long startTime;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean isAnswered = false;
    private final StringBuilder answerBuffer = new StringBuilder();

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            tvTimer.setText("用时: " + formatTime((int) elapsed));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        tvProgress = findViewById(R.id.tvProgress);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvTimer = findViewById(R.id.tvTimer);
        tvResult = findViewById(R.id.tvResult);
        tvAnswer = findViewById(R.id.tvAnswer);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnNext = findViewById(R.id.btnNext);
        layoutResult = findViewById(R.id.layoutResult);

        int[] digitIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int i = 0; i < digitIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(digitIds[i]).setOnClickListener(v -> {
                if (isAnswered) return;
                if (answerBuffer.length() < 6) {
                    answerBuffer.append(digit);
                    tvAnswer.setText(answerBuffer.toString());
                }
            });
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (isAnswered) return;
            if (answerBuffer.length() > 0) {
                answerBuffer.deleteCharAt(answerBuffer.length() - 1);
                tvAnswer.setText(answerBuffer.toString());
            }
        });
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            if (isAnswered) return;
            answerBuffer.setLength(0);
            tvAnswer.setText("");
        });

        generateQuestions();
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
        showQuestion();

        btnSubmit.setOnClickListener(v -> submitAnswer());
        btnNext.setOnClickListener(v -> nextQuestion());
    }

    private void generateQuestions() {
        Random random = new Random();
        for (int i = 0; i < TOTAL_QUESTIONS; i++) {
            int num1 = 100 + random.nextInt(900);
            int num2 = 10 + random.nextInt(90);
            questions.add(new Question(num1, num2));
        }
    }

    private void showQuestion() {
        if (currentIndex >= TOTAL_QUESTIONS) {
            finishPractice();
            return;
        }
        Question q = questions.get(currentIndex);
        tvProgress.setText("第 " + (currentIndex + 1) + " / " + TOTAL_QUESTIONS + " 题");
        tvQuestion.setText(q.getQuestionText());
        answerBuffer.setLength(0);
        tvAnswer.setText("");
        layoutResult.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
        isAnswered = false;
    }

    private void submitAnswer() {
        String input = answerBuffer.toString().trim();
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show();
            return;
        }
        long userAnswer;
        try {
            userAnswer = Long.parseLong(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
            return;
        }

        Question q = questions.get(currentIndex);
        boolean correct = (userAnswer == q.getAnswer());
        q.setCorrect(correct);
        if (correct) {
            correctCount++;
        } else {
            DatabaseHelper db = new DatabaseHelper(this);
            db.saveWrongQuestion(q.getNum1(), q.getNum2());
        }

        isAnswered = true;
        layoutResult.setVisibility(View.VISIBLE);
        if (correct) {
            tvResult.setText("✓ 回答正确！");
            tvResult.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvResult.setText("✗ 回答错误，正确答案是: " + q.getAnswer());
            tvResult.setTextColor(getColor(android.R.color.holo_red_dark));
        }
        btnSubmit.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
        if (currentIndex == TOTAL_QUESTIONS - 1) {
            btnNext.setText("查看结果");
        }
    }

    private void nextQuestion() {
        currentIndex++;
        showQuestion();
    }

    private void finishPractice() {
        timerHandler.removeCallbacks(timerRunnable);
        int elapsedSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);

        DatabaseHelper db = new DatabaseHelper(this);
        db.saveRecord(TOTAL_QUESTIONS, correctCount, elapsedSeconds);

        new AlertDialog.Builder(this)
                .setTitle("练习完成")
                .setMessage("总题数: " + TOTAL_QUESTIONS + "\n" +
                        "正确数: " + correctCount + "\n" +
                        "正确率: " + (correctCount * 100 / TOTAL_QUESTIONS) + "%\n" +
                        "用时: " + formatTime(elapsedSeconds))
                .setCancelable(false)
                .setPositiveButton("返回首页", (d, w) -> finish())
                .setNegativeButton("再练一次", (d, w) -> {
                    questions.clear();
                    currentIndex = 0;
                    correctCount = 0;
                    generateQuestions();
                    startTime = System.currentTimeMillis();
                    timerHandler.post(timerRunnable);
                    showQuestion();
                })
                .show();
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
