package com.mirea.Samsonova.securesharedpreferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.mirea.Samsonova.securesharedpreferences.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences secureSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            String mainKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            secureSharedPreferences = EncryptedSharedPreferences.create(
                    "secret_shared_prefs",
                    mainKeyAlias,
                    getBaseContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String savedPoet = secureSharedPreferences.getString("POET_NAME", "");
            binding.editPoetName.setText(savedPoet);

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        binding.btnSaveSecure.setOnClickListener(v -> {
            String poetName = binding.editPoetName.getText().toString();
            secureSharedPreferences.edit().putString("POET_NAME", poetName).apply();
            Toast.makeText(this, "Имя поэта надежно сохранено", Toast.LENGTH_SHORT).show();
        });
    }
}