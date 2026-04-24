package com.mirea.Samsonova.thread;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.mirea.Samsonova.thread.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonCalc.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    float pairs = Float.parseFloat(binding.editPairs.getText().toString());
                    float days = Float.parseFloat(binding.editDays.getText().toString());
                    float result = pairs / days;
                    runOnUiThread(() -> binding.textResult.setText("Среднее пар в день: " + result));
                } catch (Exception e) {
                    runOnUiThread(() -> binding.textResult.setText("Ошибка в данных"));
                }
            }).start();
        });
    }
}