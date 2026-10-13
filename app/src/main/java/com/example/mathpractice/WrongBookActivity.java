package com.example.mathpractice;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.Executor;

public class WrongBookActivity extends AppCompatActivity {

    private static final int MODE_ALL = 0;
    private static final int MODE_WEEK = 1;
    private static final int MODE_MONTH = 2;
    private static final String KEY_PASSWORD = "clear_password";

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private Button btnModeAll, btnModeWeek, btnModeMonth;
    private DatabaseHelper db;
    private int currentMode = MODE_ALL;
    private WrongAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrong_book);

        db = new DatabaseHelper(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.recyclerView);
        btnModeAll = findViewById(R.id.btnModeAll);
        btnModeWeek = findViewById(R.id.btnModeWeek);
        btnModeMonth = findViewById(R.id.btnModeMonth);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WrongAdapter();
        recyclerView.setAdapter(adapter);

        btnModeAll.setOnClickListener(v -> switchMode(MODE_ALL));
        btnModeWeek.setOnClickListener(v -> switchMode(MODE_WEEK));
        btnModeMonth.setOnClickListener(v -> switchMode(MODE_MONTH));

        findViewById(R.id.btnClearAll).setOnClickListener(v -> onClearAllClicked());

        updateModeButtons();
        loadData();
    }

    private void switchMode(int mode) {
        currentMode = mode;
        updateModeButtons();
        loadData();
    }

    private void updateModeButtons() {
        btnModeAll.setEnabled(currentMode != MODE_ALL);
        btnModeWeek.setEnabled(currentMode != MODE_WEEK);
        btnModeMonth.setEnabled(currentMode != MODE_MONTH);
    }

    private void loadData() {
        switch (currentMode) {
            case MODE_WEEK:
                adapter.setStatsData(db.getWrongStatsByWeek());
                break;
            case MODE_MONTH:
                adapter.setStatsData(db.getWrongStatsByMonth());
                break;
            default:
                adapter.setQuestionData(db.getAllWrongQuestions());
                break;
        }
        updateEmpty();
    }

    private void updateEmpty() {
        tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    // ============ 清空错题本的验证流程 ============
    private void onClearAllClicked() {
        String savedPassword = db.getSetting(KEY_PASSWORD);
        if (savedPassword == null) {
            // 还没设置过密码 → 先设置
            showSetPasswordDialog();
        } else {
            // 已设置过 → 先尝试指纹，指纹不行再输密码
            tryBiometricOrPassword(savedPassword);
        }
    }

    private void showSetPasswordDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("请输入 4-8 位数字密码");

        new AlertDialog.Builder(this)
                .setTitle("设置清空密码")
                .setMessage("首次清空错题本，请设置一个密码")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create()
                .show();
        // 用 show 后再 set 监听，避免输入为空时对话框自动关闭
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("设置清空密码")
                .setMessage("首次清空错题本，请设置一个密码（4-8 位数字）")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pwd = input.getText().toString().trim();
            if (!pwd.matches("\\d{4,8}")) {
                Toast.makeText(this, "请输入 4-8 位数字", Toast.LENGTH_SHORT).show();
                return;
            }
            db.setSetting(KEY_PASSWORD, pwd);
            Toast.makeText(this, "密码设置成功", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            doClear();
        });
    }

    private void tryBiometricOrPassword(String savedPassword) {
        BiometricManager bm = BiometricManager.from(this);
        int can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if (can == BiometricManager.BIOMETRIC_SUCCESS) {
            // 有指纹/生物识别 → 弹指纹
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt prompt = new BiometricPrompt(this, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            doClear();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            // 用户取消或出错，退回密码输入
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                    && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                    && errorCode != BiometricPrompt.ERROR_CANCELED) {
                                showPasswordDialog(savedPassword);
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            // 单次识别失败，系统会自动重试，这里不用处理
                        }
                    });
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("验证指纹")
                    .setSubtitle("验证通过后可清空错题本")
                    .setNegativeButtonText("使用密码")
                    .build();
            prompt.authenticate(info);
        } else {
            // 没指纹 → 直接输密码
            showPasswordDialog(savedPassword);
        }
    }

    private void showPasswordDialog(String savedPassword) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("请输入密码");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("验证密码")
                .setMessage("请输入清空密码")
                .setView(input)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pwd = input.getText().toString().trim();
            if (TextUtils.isEmpty(pwd)) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pwd.equals(savedPassword)) {
                dialog.dismiss();
                doClear();
            } else {
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doClear() {
        db.clearWrongQuestions();
        loadData();
        Toast.makeText(this, "错题本已清空", Toast.LENGTH_SHORT).show();
    }

    // ============ Adapter ============
    class WrongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_QUESTION = 0;
        private static final int TYPE_STATS = 1;

        private List<DatabaseHelper.WrongQuestion> questions;
        private List<DatabaseHelper.WrongStats> stats;

        void setQuestionData(List<DatabaseHelper.WrongQuestion> data) {
            this.questions = data;
            this.stats = null;
            notifyDataSetChanged();
        }

        void setStatsData(List<DatabaseHelper.WrongStats> data) {
            this.stats = data;
            this.questions = null;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return questions != null ? TYPE_QUESTION : TYPE_STATS;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_QUESTION) {
                return new QuestionVH(inflater.inflate(R.layout.item_wrong, parent, false));
            } else {
                return new StatsVH(inflater.inflate(R.layout.item_wrong_stats, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof QuestionVH) {
                DatabaseHelper.WrongQuestion w = questions.get(position);
                QuestionVH h = (QuestionVH) holder;
                h.tvQuestion.setText(w.getQuestionText());
                h.tvAnswer.setText("正确答案: " + w.getAnswer());
                h.tvDate.setText(w.date);
                h.btnDelete.setOnClickListener(v -> {
                    db.deleteWrongQuestion(w.id);
                    loadData();
                });
            } else {
                DatabaseHelper.WrongStats s = stats.get(position);
                StatsVH h = (StatsVH) holder;
                h.tvPeriod.setText(s.getDisplayText());
                h.tvCount.setText("错题数: " + s.count + " 道");
            }
        }

        @Override
        public int getItemCount() {
            if (questions != null) return questions.size();
            if (stats != null) return stats.size();
            return 0;
        }

        class QuestionVH extends RecyclerView.ViewHolder {
            TextView tvQuestion, tvAnswer, tvDate;
            Button btnDelete;
            QuestionVH(@NonNull View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tvQuestion);
                tvAnswer = itemView.findViewById(R.id.tvAnswer);
                tvDate = itemView.findViewById(R.id.tvDate);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }

        class StatsVH extends RecyclerView.ViewHolder {
            TextView tvPeriod, tvCount;
            StatsVH(@NonNull View itemView) {
                super(itemView);
                tvPeriod = itemView.findViewById(R.id.tvPeriod);
                tvCount = itemView.findViewById(R.id.tvCount);
            }
        }
    }
}
