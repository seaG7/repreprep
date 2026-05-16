package com.mirea.Samsonova.mireaproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonSignIn;
    private Button buttonCreateAccount;
    private TextView textViewStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        textViewStatus = findViewById(R.id.textViewStatus);
        progressBar = findViewById(R.id.progressBar);

        buttonSignIn.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString();
            signIn(email, password);
        });

        buttonCreateAccount.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString();
            createAccount(email, password);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            openMainScreen();
        }
    }

    private void signIn(String email, String password) {
        if (!validateForm()) {
            return;
        }

        setLoading(true);
        textViewStatus.setText(R.string.login_status_signing_in);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                R.string.login_success,
                                Toast.LENGTH_SHORT
                        ).show();

                        openMainScreen();
                    } else {
                        textViewStatus.setText(R.string.login_status_error);

                        Toast.makeText(
                                LoginActivity.this,
                                getErrorMessage(task.getException()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void createAccount(String email, String password) {
        if (!validateForm()) {
            return;
        }

        setLoading(true);
        textViewStatus.setText(R.string.login_status_creating_account);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                R.string.account_created,
                                Toast.LENGTH_SHORT
                        ).show();

                        openMainScreen();
                    } else {
                        textViewStatus.setText(R.string.login_status_error);

                        Toast.makeText(
                                LoginActivity.this,
                                getErrorMessage(task.getException()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(getString(R.string.error_empty_email));
            valid = false;
        } else {
            editTextEmail.setError(null);
        }

        String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.error_empty_password));
            valid = false;
        } else if (password.length() < 6) {
            editTextPassword.setError(getString(R.string.error_short_password));
            valid = false;
        } else {
            editTextPassword.setError(null);
        }

        return valid;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);

        buttonSignIn.setEnabled(!loading);
        buttonCreateAccount.setEnabled(!loading);
        editTextEmail.setEnabled(!loading);
        editTextPassword.setEnabled(!loading);
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null) {
            return getString(R.string.auth_failed);
        }

        String message = exception.getLocalizedMessage();

        if (message == null || message.trim().isEmpty()) {
            return getString(R.string.auth_failed);
        }

        return message;
    }

    private void openMainScreen() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}