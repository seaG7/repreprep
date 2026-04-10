package com.mirea.Samsonova.sharer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ShareActivity extends AppCompatActivity {

    private TextView textViewSharedData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        textViewSharedData = findViewById(R.id.textViewSharedData);

        Intent intent = getIntent();
        String resultText = "Данные не получены";

        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            String type = intent.getType();

            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null && !sharedText.isEmpty()) {
                    resultText = sharedText;
                }
            } else if (type.startsWith("image/")) {
                resultText = "Приложение готово принимать изображения";
            }
        }

        textViewSharedData.setText(resultText);
    }
}