package com.mirea.Samsonova.lesson7;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String HOST = "time.nist.gov";
    private static final int PORT = 13;

    private TextView dateTextView;
    private TextView timeTextView;
    private TextView sourceTextView;
    private TextView rawTextView;
    private Button loadButton;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        sourceTextView = findViewById(R.id.sourceTextView);
        rawTextView = findViewById(R.id.rawTextView);
        loadButton = findViewById(R.id.loadButton);

        sourceTextView.setText("Сервер: " + HOST + ":" + PORT);

        loadButton.setOnClickListener(view -> loadTime());
    }

    private void loadTime() {
        loadButton.setEnabled(false);
        dateTextView.setText("Дата: загрузка...");
        timeTextView.setText("Время: загрузка...");
        rawTextView.setText("");

        executorService.execute(() -> {
            String result = requestTimeFromServer();

            mainHandler.post(() -> {
                loadButton.setEnabled(true);
                showTime(result);
            });
        });
    }

    private String requestTimeFromServer() {
        try (
                Socket socket = new Socket(HOST, PORT);
                BufferedReader reader = SocketUtils.getReader(socket)
        ) {
            reader.readLine();
            String secondLine = reader.readLine();

            if (secondLine == null || secondLine.trim().isEmpty()) {
                return "Ошибка: сервер не вернул строку времени";
            }

            return secondLine;
        } catch (IOException e) {
            return "Ошибка подключения: " + e.getMessage();
        }
    }

    private void showTime(String result) {
        rawTextView.setText("Исходная строка:\n" + result);

        if (result.startsWith("Ошибка")) {
            dateTextView.setText("Дата: -");
            timeTextView.setText("Время: -");
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            return;
        }

        String[] parts = result.trim().split("\\s+");

        if (parts.length >= 3) {
            String date = formatNistDate(parts[1]);
            String time = parts[2];

            dateTextView.setText("Дата: " + date);
            timeTextView.setText("Время UTC: " + time);
        } else {
            dateTextView.setText("Дата: не удалось разобрать");
            timeTextView.setText("Время: не удалось разобрать");
        }
    }

    private String formatNistDate(String nistDate) {
        try {
            String[] dateParts = nistDate.split("-");

            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);

            year = year >= 70 ? 1900 + year : 2000 + year;

            return String.format(Locale.getDefault(), "%02d.%02d.%04d", day, month, year);
        } catch (Exception e) {
            return nistDate;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}