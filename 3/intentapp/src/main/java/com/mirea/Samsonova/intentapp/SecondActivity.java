package com.mirea.Samsonova.intentapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        textViewResult = findViewById(R.id.textViewResult);

        Intent intent = getIntent();
        String time = intent.getStringExtra("current_time");
        int square = intent.getIntExtra("number_square", 0);

        String result = "Квадрат значения моего номера по списку равен "
                + square + ", а текущее время " + time;

        textViewResult.setText(result);
    }
}