package com.example.mathpractice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etCount = findViewById(R.id.etCount);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStats = findViewById(R.id.btnStats);
        Button btnWrong = findViewById(R.id.btnWrong);

        btnStart.setOnClickListener(v -> {
            String input = etCount.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(this, "请输入出题数量", Toast.LENGTH_SHORT).show();
                return;
            }
            int count;
            try {
                count = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "数量无效", Toast.LENGTH_SHORT).show();
                return;
            }
            if (count < 1 || count > 999) {
                Toast.makeText(this, "数量需在 1-999 之间", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, PracticeActivity.class);
            intent.putExtra("count", count);
            startActivity(intent);
        });

        btnStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));
        btnWrong.setOnClickListener(v ->
                startActivity(new Intent(this, WrongBookActivity.class)));
    }
}
