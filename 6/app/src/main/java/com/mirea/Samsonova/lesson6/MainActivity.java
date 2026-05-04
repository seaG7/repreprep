package com.mirea.Samsonova.lesson6;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mirea.Samsonova.lesson6.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPref = getSharedPreferences("mirea_settings", Context.MODE_PRIVATE);

        binding.editGroup.setText(sharedPref.getString("GROUP", ""));
        binding.editNumber.setText(String.valueOf(sharedPref.getInt("NUMBER", 0)));
        binding.editMovie.setText(sharedPref.getString("MOVIE", ""));

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("GROUP", binding.editGroup.getText().toString());

                String numberStr = binding.editNumber.getText().toString();
                if (!numberStr.isEmpty()) {
                    editor.putInt("NUMBER", Integer.parseInt(numberStr));
                }

                editor.putString("MOVIE", binding.editMovie.getText().toString());
                editor.apply();

                Toast.makeText(MainActivity.this, "Данные сохранены", Toast.LENGTH_SHORT).show();
            }
        });
    }
}