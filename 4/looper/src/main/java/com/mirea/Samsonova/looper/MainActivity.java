package com.mirea.Samsonova.looper;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.mirea.Samsonova.looper.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Handler mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(MainActivity.class.getSimpleName(), msg.getData().getString("result"));
            }
        };

        MyLooper myLooper = new MyLooper(mainHandler);
        myLooper.start();

        binding.buttonMirea.setOnClickListener(v -> {
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString("WORK", binding.editWork.getText().toString());
            bundle.putInt("AGE", Integer.parseInt(binding.editAge.getText().toString()));
            msg.setData(bundle);
            myLooper.mHandler.sendMessage(msg);
        });
    }
}