package com.example.mathpractice;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnStats = findViewById(R.id.btnStats);
        Button btnWrong = findViewById(R.id.btnWrong);

        btnStart.setOnClickListener(v ->
                startActivity(new Intent(this, PracticeActivity.class)));
        btnStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));
        btnWrong.setOnClickListener(v ->
                startActivity(new Intent(this, WrongBookActivity.class)));
    }
}
