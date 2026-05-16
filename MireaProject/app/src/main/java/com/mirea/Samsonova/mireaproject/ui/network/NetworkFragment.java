package com.mirea.Samsonova.mireaproject.ui.network;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mirea.Samsonova.mireaproject.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkFragment extends Fragment {

    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=55.75&longitude=37.62&current_weather=true&timezone=Europe%2FMoscow";

    private TextView textViewNetworkStatus;
    private TextView textViewCity;
    private TextView textViewCoordinates;
    private TextView textViewTemperature;
    private TextView textViewWindSpeed;
    private TextView textViewWindDirection;
    private TextView textViewWeatherCode;
    private TextView textViewWeatherTime;
    private TextView textViewRawNetwork;
    private Button buttonLoadNetwork;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View root = inflater.inflate(R.layout.fragment_network, container, false);

        textViewNetworkStatus = root.findViewById(R.id.textViewNetworkStatus);
        textViewCity = root.findViewById(R.id.textViewCity);
        textViewCoordinates = root.findViewById(R.id.textViewCoordinates);
        textViewTemperature = root.findViewById(R.id.textViewTemperature);
        textViewWindSpeed = root.findViewById(R.id.textViewWindSpeed);
        textViewWindDirection = root.findViewById(R.id.textViewWindDirection);
        textViewWeatherCode = root.findViewById(R.id.textViewWeatherCode);
        textViewWeatherTime = root.findViewById(R.id.textViewWeatherTime);
        textViewRawNetwork = root.findViewById(R.id.textViewRawNetwork);
        buttonLoadNetwork = root.findViewById(R.id.buttonLoadNetwork);

        buttonLoadNetwork.setOnClickListener(view -> loadWeather());

        loadWeather();

        return root;
    }

    private void loadWeather() {
        buttonLoadNetwork.setEnabled(false);
        textViewNetworkStatus.setText(R.string.network_status_loading);
        textViewRawNetwork.setText("");

        executorService.execute(() -> {
            try {
                String json = downloadText(WEATHER_URL);
                WeatherInfo weatherInfo = parseWeather(json);

                mainHandler.post(() -> {
                    if (getView() == null) {
                        return;
                    }

                    buttonLoadNetwork.setEnabled(true);
                    showWeather(weatherInfo);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (getView() == null) {
                        return;
                    }

                    buttonLoadNetwork.setEnabled(true);
                    textViewNetworkStatus.setText(R.string.network_status_error);

                    Toast.makeText(
                            requireContext(),
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private String downloadText(String address) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();

            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
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

                throw new IOException(
                        "Ошибка сервера. Код: " + responseCode + "\n" + errorText
                );
            }

        } catch (SecurityException e) {
            throw new IOException(
                    "Нет разрешения INTERNET. Проверь AndroidManifest.xml.",
                    e
            );
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

    private WeatherInfo parseWeather(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject currentWeather = root.getJSONObject("current_weather");
        JSONObject units = root.optJSONObject("current_weather_units");

        WeatherInfo weatherInfo = new WeatherInfo();

        weatherInfo.city = "Москва";
        weatherInfo.coordinates = "55.75, 37.62";

        weatherInfo.temperature =
                currentWeather.optString("temperature", "-")
                        + " "
                        + getUnit(units, "temperature", "°C");

        weatherInfo.windSpeed =
                currentWeather.optString("windspeed", "-")
                        + " "
                        + getUnit(units, "windspeed", "км/ч");

        weatherInfo.windDirection =
                currentWeather.optString("winddirection", "-")
                        + " "
                        + getUnit(units, "winddirection", "°");

        weatherInfo.weatherCode = currentWeather.optString("weathercode", "-");
        weatherInfo.time = currentWeather.optString("time", "-");
        weatherInfo.rawJson = root.toString(2);

        return weatherInfo;
    }

    private String getUnit(JSONObject units, String key, String defaultValue) {
        if (units == null) {
            return defaultValue;
        }

        String unit = units.optString(key, defaultValue);

        if (unit == null || unit.trim().isEmpty()) {
            return defaultValue;
        }

        return unit;
    }

    private void showWeather(WeatherInfo weatherInfo) {
        textViewNetworkStatus.setText(R.string.network_status_success);

        textViewCity.setText("Город: " + weatherInfo.city);
        textViewCoordinates.setText("Координаты: " + weatherInfo.coordinates);
        textViewTemperature.setText("Температура: " + weatherInfo.temperature);
        textViewWindSpeed.setText("Скорость ветра: " + weatherInfo.windSpeed);
        textViewWindDirection.setText("Направление ветра: " + weatherInfo.windDirection);
        textViewWeatherCode.setText("Код погоды: " + weatherInfo.weatherCode);
        textViewWeatherTime.setText("Время прогноза: " + weatherInfo.time);

        textViewRawNetwork.setText("JSON-ответ:\n" + weatherInfo.rawJson);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    private static class WeatherInfo {
        String city;
        String coordinates;
        String temperature;
        String windSpeed;
        String windDirection;
        String weatherCode;
        String time;
        String rawJson;
    }
}