package com.mirea.Samsonova.firebaseauth;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseAuth mAuth;

    private TextView statusTextView;
    private TextView detailTextView;

    private EditText emailEditText;
    private EditText passwordEditText;

    private LinearLayout emailPasswordFields;
    private LinearLayout emailPasswordButtons;
    private LinearLayout signedInButtons;

    private Button signInButton;
    private Button createAccountButton;
    private Button signOutButton;
    private Button verifyEmailButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        detailTextView = findViewById(R.id.detailTextView);

        emailEditText = findViewById(R.id.fieldEmail);
        passwordEditText = findViewById(R.id.fieldPassword);

        emailPasswordFields = findViewById(R.id.emailPasswordFields);
        emailPasswordButtons = findViewById(R.id.emailPasswordButtons);
        signedInButtons = findViewById(R.id.signedInButtons);

        signInButton = findViewById(R.id.emailSignInButton);
        createAccountButton = findViewById(R.id.emailCreateAccountButton);
        signOutButton = findViewById(R.id.signOutButton);
        verifyEmailButton = findViewById(R.id.verifyEmailButton);

        mAuth = FirebaseAuth.getInstance();

        signInButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();
            signIn(email, password);
        });

        createAccountButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();
            createAccount(email, password);
        });

        signOutButton.setOnClickListener(view -> signOut());

        verifyEmailButton.setOnClickListener(view -> sendEmailVerification());
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);

        if (!validateForm()) {
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");

                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Аккаунт создан",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());

                            Toast.makeText(
                                    MainActivity.this,
                                    getErrorMessage(task),
                                    Toast.LENGTH_LONG
                            ).show();

                            updateUI(null);
                        }
                    }
                });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);

        if (!validateForm()) {
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");

                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Вход выполнен",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());

                            statusTextView.setText(R.string.auth_failed);

                            Toast.makeText(
                                    MainActivity.this,
                                    getErrorMessage(task),
                                    Toast.LENGTH_LONG
                            ).show();

                            updateUI(null);
                        }
                    }
                });
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);

        Toast.makeText(this, "Выход выполнен", Toast.LENGTH_SHORT).show();
    }

    private void sendEmailVerification() {
        verifyEmailButton.setEnabled(false);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            updateUI(null);
            return;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        verifyEmailButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Письмо отправлено на " + user.getEmail(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Log.e(TAG, "sendEmailVerification", task.getException());

                            Toast.makeText(
                                    MainActivity.this,
                                    "Не удалось отправить письмо",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                });
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_empty_email));
            valid = false;
        } else {
            emailEditText.setError(null);
        }

        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_empty_password));
            valid = false;
        } else if (password.length() < 6) {
            passwordEditText.setError(getString(R.string.error_short_password));
            valid = false;
        } else {
            passwordEditText.setError(null);
        }

        return valid;
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            statusTextView.setText(
                    getString(
                            R.string.emailpassword_status_fmt,
                            user.getEmail(),
                            user.isEmailVerified()
                    )
            );

            detailTextView.setText(
                    getString(
                            R.string.firebase_status_fmt,
                            user.getUid()
                    )
            );

            emailPasswordButtons.setVisibility(View.GONE);
            emailPasswordFields.setVisibility(View.GONE);
            signedInButtons.setVisibility(View.VISIBLE);

            verifyEmailButton.setEnabled(!user.isEmailVerified());
        } else {
            statusTextView.setText(R.string.signed_out);
            detailTextView.setText("");

            emailPasswordButtons.setVisibility(View.VISIBLE);
            emailPasswordFields.setVisibility(View.VISIBLE);
            signedInButtons.setVisibility(View.GONE);
        }
    }

    private String getErrorMessage(Task<?> task) {
        if (task.getException() == null) {
            return getString(R.string.auth_failed);
        }

        String message = task.getException().getMessage();

        if (message == null || message.trim().isEmpty()) {
            return getString(R.string.auth_failed);
        }

        return message;
    }
}