package com.mirea.Samsonova.data_thread;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import java.util.concurrent.TimeUnit;
import com.mirea.Samsonova.data_thread.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Runnable runn1 = () -> binding.tvInfo.append("\nrunn1: runOnUiThread");
        final Runnable runn2 = () -> binding.tvInfo.append("\nrunn2: post");
        final Runnable runn3 = () -> binding.tvInfo.append("\nrunn3: postDelayed");

        Thread t = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
                runOnUiThread(runn1);
                TimeUnit.SECONDS.sleep(1);
                binding.tvInfo.post(runn2);
                binding.tvInfo.postDelayed(runn3, 2000);
            } catch (InterruptedException e) { e.printStackTrace(); }
        });
        t.start();
    }
}