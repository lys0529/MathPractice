package com.example.mathpractice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper db = new DatabaseHelper(this);
        List<DatabaseHelper.DailyStats> stats = db.getAllDailyStats();

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        if (stats.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }

        recyclerView.setAdapter(new StatsAdapter(stats));
    }

    static class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.VH> {
        private final List<DatabaseHelper.DailyStats> data;
        private final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        StatsAdapter(List<DatabaseHelper.DailyStats> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stats, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DatabaseHelper.DailyStats s = data.get(position);
            String dateLabel = s.date;
            if (s.date.equals(today)) dateLabel += " (今天)";
            h.tvDate.setText(dateLabel);
            h.tvSessions.setText("练习次数: " + s.sessions + " 次");
            h.tvQuestions.setText("题目: " + s.correctAnswers + " / " + s.totalQuestions +
                    " (正确率 " + s.getAccuracy() + "%)");
            h.tvTime.setText("累计用时: " + s.getTimeFormatted());
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvSessions, tvQuestions, tvTime;
            VH(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvSessions = itemView.findViewById(R.id.tvSessions);
                tvQuestions = itemView.findViewById(R.id.tvQuestions);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}
