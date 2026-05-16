package com.mirea.Samsonova.httpurlconnection;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String IP_INFO_URL = "https://ipinfo.io/json";

    private TextView statusTextView;
    private TextView ipTextView;
    private TextView cityTextView;
    private TextView regionTextView;
    private TextView countryTextView;
    private TextView locationTextView;
    private TextView orgTextView;
    private TextView timezoneTextView;

    private TextView weatherStatusTextView;
    private TextView temperatureTextView;
    private TextView windSpeedTextView;
    private TextView windDirectionTextView;
    private TextView weatherCodeTextView;
    private TextView weatherTimeTextView;

    private Button loadButton;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        ipTextView = findViewById(R.id.ipTextView);
        cityTextView = findViewById(R.id.cityTextView);
        regionTextView = findViewById(R.id.regionTextView);
        countryTextView = findViewById(R.id.countryTextView);
        locationTextView = findViewById(R.id.locationTextView);
        orgTextView = findViewById(R.id.orgTextView);
        timezoneTextView = findViewById(R.id.timezoneTextView);

        weatherStatusTextView = findViewById(R.id.weatherStatusTextView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        windSpeedTextView = findViewById(R.id.windSpeedTextView);
        windDirectionTextView = findViewById(R.id.windDirectionTextView);
        weatherCodeTextView = findViewById(R.id.weatherCodeTextView);
        weatherTimeTextView = findViewById(R.id.weatherTimeTextView);

        loadButton = findViewById(R.id.loadButton);

        loadButton.setOnClickListener(view -> loadData());
    }

    private void loadData() {
        if (!hasInternetConnection()) {
            Toast.makeText(this, "Нет интернета", Toast.LENGTH_SHORT).show();
            return;
        }

        loadButton.setEnabled(false);
        statusTextView.setText("Загружаем...");
        clearFields();

        executorService.execute(() -> {
            try {
                String ipJson = downloadText(IP_INFO_URL);
                JSONObject ipInfo = new JSONObject(ipJson);

                JSONObject weatherInfo = null;
                String loc = ipInfo.optString("loc", "");

                if (!loc.isEmpty() && loc.contains(",")) {
                    String[] coordinates = loc.split(",");

                    if (coordinates.length == 2) {
                        String latitude = coordinates[0].trim();
                        String longitude = coordinates[1].trim();

                        String weatherUrl = buildWeatherUrl(latitude, longitude);
                        String weatherJson = downloadText(weatherUrl);
                        weatherInfo = new JSONObject(weatherJson);
                    }
                }

                JSONObject finalWeatherInfo = weatherInfo;

                mainHandler.post(() -> {
                    loadButton.setEnabled(true);
                    showIpInfo(ipInfo);
                    showWeather(finalWeatherInfo);
                    statusTextView.setText("Данные загружены");
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    loadButton.setEnabled(true);
                    statusTextView.setText("Ошибка загрузки");
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean hasInternetConnection() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();

            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private String buildWeatherUrl(String latitude, String longitude) {
        return "https://api.open-meteo.com/v1/forecast?latitude="
                + latitude
                + "&longitude="
                + longitude
                + "&current_weather=true";
    }

    private String downloadText(String address) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();

            connection.setReadTimeout(100000);
            connection.setConnectTimeout(100000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                return readStream(inputStream);
            } else {
                inputStream = connection.getErrorStream();
                String errorText = readStream(inputStream);
                throw new IOException(connection.getResponseMessage()
                        + ". Error Code: " + responseCode
                        + "\n" + errorText);
            }

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, read);
        }

        return byteArrayOutputStream.toString("UTF-8");
    }

    private void showIpInfo(JSONObject ipInfo) {
        ipTextView.setText("IP: " + value(ipInfo, "ip"));
        cityTextView.setText("Город: " + value(ipInfo, "city"));
        regionTextView.setText("Регион: " + value(ipInfo, "region"));
        countryTextView.setText("Страна: " + value(ipInfo, "country"));
        locationTextView.setText("Координаты: " + value(ipInfo, "loc"));
        orgTextView.setText("Организация: " + value(ipInfo, "org"));
        timezoneTextView.setText("Часовой пояс: " + value(ipInfo, "timezone"));
    }

    private void showWeather(JSONObject weatherInfo) {
        if (weatherInfo == null) {
            weatherStatusTextView.setText("Погода: координаты не получены");
            temperatureTextView.setText("Температура: -");
            windSpeedTextView.setText("Скорость ветра: -");
            windDirectionTextView.setText("Направление ветра: -");
            weatherCodeTextView.setText("Код погоды: -");
            weatherTimeTextView.setText("Время прогноза: -");
            return;
        }

        JSONObject currentWeather = weatherInfo.optJSONObject("current_weather");
        JSONObject units = weatherInfo.optJSONObject("current_weather_units");

        if (currentWeather == null) {
            weatherStatusTextView.setText("Погода: нет блока current_weather");
            return;
        }

        weatherStatusTextView.setText("Погода: получена");

        temperatureTextView.setText(
                "Температура: "
                        + weatherValue(currentWeather, "temperature")
                        + " "
                        + unit(units, "temperature", "°C")
        );

        windSpeedTextView.setText(
                "Скорость ветра: "
                        + weatherValue(currentWeather, "windspeed")
                        + " "
                        + unit(units, "windspeed", "km/h")
        );

        windDirectionTextView.setText(
                "Направление ветра: "
                        + weatherValue(currentWeather, "winddirection")
                        + " "
                        + unit(units, "winddirection", "°")
        );

        weatherCodeTextView.setText(
                "Код погоды: " + weatherValue(currentWeather, "weathercode")
        );

        weatherTimeTextView.setText(
                "Время прогноза: " + weatherValue(currentWeather, "time")
        );
    }

    private String value(JSONObject object, String key) {
        String result = object.optString(key, "-");

        if (result == null || result.trim().isEmpty()) {
            return "-";
        }

        return result;
    }

    private String weatherValue(JSONObject object, String key) {
        Object result = object.opt(key);

        if (result == null || result == JSONObject.NULL) {
            return "-";
        }

        return String.valueOf(result);
    }

    private String unit(JSONObject units, String key, String defaultValue) {
        if (units == null) {
            return defaultValue;
        }

        String result = units.optString(key, defaultValue);

        if (result == null || result.trim().isEmpty()) {
            return defaultValue;
        }

        return result;
    }

    private void clearFields() {
        ipTextView.setText("IP: -");
        cityTextView.setText("Город: -");
        regionTextView.setText("Регион: -");
        countryTextView.setText("Страна: -");
        locationTextView.setText("Координаты: -");
        orgTextView.setText("Организация: -");
        timezoneTextView.setText("Часовой пояс: -");

        weatherStatusTextView.setText("Погода: -");
        temperatureTextView.setText("Температура: -");
        windSpeedTextView.setText("Скорость ветра: -");
        windDirectionTextView.setText("Направление ветра: -");
        weatherCodeTextView.setText("Код погоды: -");
        weatherTimeTextView.setText("Время прогноза: -");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}