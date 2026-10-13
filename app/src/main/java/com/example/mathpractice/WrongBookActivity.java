package com.example.mathpractice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WrongBookActivity extends AppCompatActivity {

    private StatsAdapter adapter;
    private DatabaseHelper db;
    private TextView tvEmpty;
    private List<DatabaseHelper.WrongQuestion> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrong_book);

        db = new DatabaseHelper(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnClearAll).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清空错题本")
                    .setMessage("确定要清空所有错题吗？")
                    .setPositiveButton("清空", (d, w) -> {
                        db.clearWrongQuestions();
                        loadData();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        data = db.getAllWrongQuestions();
        adapter = new StatsAdapter();
        recyclerView.setAdapter(adapter);
        updateEmpty();
    }

    private void loadData() {
        data = db.getAllWrongQuestions();
        adapter.notifyDataSetChanged();
        updateEmpty();
    }

    private void updateEmpty() {
        tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wrong, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DatabaseHelper.WrongQuestion w = data.get(position);
            h.tvQuestion.setText(w.getQuestionText());
            h.tvAnswer.setText("正确答案: " + w.getAnswer());
            h.tvDate.setText(w.date);

            h.btnDelete.setOnClickListener(v -> {
                db.deleteWrongQuestion(w.id);
                loadData();
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvQuestion, tvAnswer, tvDate;
            Button btnDelete;
            VH(@NonNull View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tvQuestion);
                tvAnswer = itemView.findViewById(R.id.tvAnswer);
                tvDate = itemView.findViewById(R.id.tvDate);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
