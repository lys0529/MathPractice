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

    private int totalQuestions = 10;

    private TextView tvProgress, tvQuestion, tvTimer, tvAnswer;
    private Button btnSubmit;
    private LinearLayout layoutResult;

    private final List<Question> questions = new ArrayList<>();
    private final List<Long> userAnswers = new ArrayList<>();
    private int currentIndex = 0;
    private long startTime;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
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

        totalQuestions = getIntent().getIntExtra("count", 10);
        if (totalQuestions < 1) totalQuestions = 1;

        tvProgress = findViewById(R.id.tvProgress);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvTimer = findViewById(R.id.tvTimer);
        tvAnswer = findViewById(R.id.tvAnswer);
        btnSubmit = findViewById(R.id.btnSubmit);
        layoutResult = findViewById(R.id.layoutResult);

        // 隐藏原来的结果区和下一题按钮，本版本不用它们
        layoutResult.setVisibility(View.GONE);
        findViewById(R.id.btnNext).setVisibility(View.GONE);

        int[] digitIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int i = 0; i < digitIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(digitIds[i]).setOnClickListener(v -> {
                if (answerBuffer.length() < 6) {
                    answerBuffer.append(digit);
                    tvAnswer.setText(answerBuffer.toString());
                }
            });
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (answerBuffer.length() > 0) {
                answerBuffer.deleteCharAt(answerBuffer.length() - 1);
                tvAnswer.setText(answerBuffer.toString());
            }
        });
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            answerBuffer.setLength(0);
            tvAnswer.setText("");
        });

        generateQuestions();
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
        showQuestion();

        btnSubmit.setOnClickListener(v -> submitAnswer());
    }

    private void generateQuestions() {
        Random random = new Random();
        for (int i = 0; i < totalQuestions; i++) {
            int num1 = 100 + random.nextInt(900);
            int num2 = 10 + random.nextInt(90);
            questions.add(new Question(num1, num2));
        }
    }

    private void showQuestion() {
        if (currentIndex >= totalQuestions) {
            finishPractice();
            return;
        }
        Question q = questions.get(currentIndex);
        tvProgress.setText("第 " + (currentIndex + 1) + " / " + totalQuestions + " 题");
        tvQuestion.setText(q.getQuestionText());
        answerBuffer.setLength(0);
        tvAnswer.setText("");
        if (currentIndex == totalQuestions - 1) {
            btnSubmit.setText("提交并查看结果");
        } else {
            btnSubmit.setText("提交");
        }
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

        userAnswers.add(userAnswer);
        currentIndex++;
        showQuestion();
    }

    private void finishPractice() {
        timerHandler.removeCallbacks(timerRunnable);
        int elapsedSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);

        // 计算正确数、保存错题
        int correctCount = 0;
        StringBuilder detail = new StringBuilder();
        DatabaseHelper db = new DatabaseHelper(this);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            long ua = (i < userAnswers.size()) ? userAnswers.get(i) : -1;
            boolean correct = (ua == q.getAnswer());
            q.setCorrect(correct);
            if (correct) {
                correctCount++;
            } else {
                db.saveWrongQuestion(q.getNum1(), q.getNum2());
            }
            // 拼结果明细
            detail.append(i + 1).append(". ")
                    .append(q.getQuestionText().replace(" = ?", ""))
                    .append(" = ")
                    .append(q.getAnswer())
                    .append("  ");
            if (correct) {
                detail.append("✓");
            } else {
                detail.append("✗ 你答: ").append(ua >= 0 ? ua : "未答");
            }
            detail.append("\n");
        }

        db.saveRecord(totalQuestions, correctCount, elapsedSeconds);

        final int finalCorrect = correctCount;
        new AlertDialog.Builder(this)
                .setTitle("练习完成")
                .setMessage("总题数: " + totalQuestions + "\n" +
                        "正确数: " + finalCorrect + "\n" +
                        "正确率: " + (finalCorrect * 100 / totalQuestions) + "%\n" +
                        "用时: " + formatTime(elapsedSeconds))
                .setCancelable(false)
                .setPositiveButton("查看详情", (d, w) -> showDetailDialog(detail.toString()))
                .setNeutralButton("再练一次", (d, w) -> restart())
                .setNegativeButton("返回首页", (d, w) -> finish())
                .show();
    }

    private void showDetailDialog(String detail) {
        new AlertDialog.Builder(this)
                .setTitle("答题详情")
                .setMessage(detail)
                .setPositiveButton("确定", null)
                .show();
    }

    private void restart() {
        questions.clear();
        userAnswers.clear();
        currentIndex = 0;
        generateQuestions();
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
        showQuestion();
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
