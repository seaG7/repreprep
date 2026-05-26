# Практическая работа №7. Сетевое взаимодействие в Android

**Студент:** Самсонова Ольга Павловна  
**Группа:** БСБО-09-23  
**Дисциплина:** Разработка мобильных приложений

---

## 1. Цель работы

Изучить способы организации сетевого взаимодействия в Android-приложениях: работу с сокетами на низком уровне через `java.net.Socket`, выполнение HTTP-запросов с помощью `HttpURLConnection`, парсинг JSON-ответов с использованием `org.json`, а также интеграцию Firebase Authentication для реализации входа в систему по электронной почте и паролю. Освоить современный подход к выполнению фоновых операций через `ExecutorService` и `Handler(Looper.getMainLooper())` вместо устаревшего `AsyncTask`.

---

## 2. Архитектура проекта

Практическая работа состоит из трёх самостоятельных модулей и контрольного задания в рамках проекта **MireaProject**:

| Модуль | Описание |
|---|---|
| `timeservice` | TCP-сокет, подключение к NIST-серверу времени (`time.nist.gov:13`) |
| `httpurlconnection` | HTTP GET через `HttpURLConnection`, IP-геолокация и погода |
| `firebaseauth` | Аутентификация Firebase: вход, регистрация, верификация email |
| `MireaProject` | Контрольное задание: экран входа (Firebase) + фрагмент погоды |

Все сетевые операции выполняются в фоновом потоке через `ExecutorService.execute()`, результаты передаются на главный поток через `Handler(Looper.getMainLooper()).post()`.

---

## 3. Ход работы

### 3.1 Модуль TimeService — получение времени через TCP-сокет

#### Описание

Модуль демонстрирует низкоуровневое сетевое взаимодействие через TCP-сокет (`java.net.Socket`). Приложение подключается к публичному серверу точного времени NIST (`time.nist.gov`, порт `13`) и получает строку в формате NIST Daytime Protocol. Сервер возвращает две строки: первую (заголовок) пропускаем, вторую парсим как дату и время UTC.

Формат ответа сервера:
```
\n
JJJJJ YY-MM-DD HH:MM:SS TT L H msADV UTC(NIST) OTM\n
```

Приложение выводит дату (в формате `DD.MM.YYYY`), время UTC, источник (`time.nist.gov:13`) и исходную строку ответа.

![Экран приложения TimeService](report-images/timeservice_main.png)

#### Ключевые фрагменты кода

Подключение к сокету и чтение ответа выполняется в фоновом потоке:

```java
private static final String HOST = "time.nist.gov";
private static final int PORT = 13;

private final ExecutorService executorService = Executors.newSingleThreadExecutor();
private final Handler mainHandler = new Handler(Looper.getMainLooper());
```

```java
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
```

Метод `loadTime()` запускает задачу в фоне и публикует результат на UI-поток:

```java
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
```

Парсинг даты NIST: сервер возвращает год в формате `YY`, поэтому применяется правило: год ≥ 70 → 1900+, иначе → 2000+:

```java
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
```

Отображение результата: если строка начинается с «Ошибка», показываем Toast и прочерки; иначе разбиваем по пробелам, берём `parts[1]` как дату и `parts[2]` как время:

```java
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
```

В `onDestroy()` пул потоков корректно завершается:

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    executorService.shutdownNow();
}
```

Разрешение в `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

![Результат загрузки времени](report-images/timeservice_result.png)

---

### 3.2 Модуль HttpURLConnection — HTTP-запросы и JSON

#### Описание

Модуль демонстрирует работу с HTTP GET-запросами через стандартный `HttpURLConnection` и парсинг JSON-ответов с помощью `org.json`. Приложение выполняет два последовательных запроса:

1. **ipinfo.io/json** — получение информации об IP-адресе пользователя (IP, город, регион, страна, координаты, организация, часовой пояс).
2. **api.open-meteo.com** — получение текущей погоды по координатам из первого запроса (температура, скорость и направление ветра, код погоды, время прогноза).

Перед отправкой запросов проверяется наличие интернет-соединения через `ConnectivityManager` с поддержкой как современного (API 23+), так и устаревшего API.

![Экран приложения HttpURLConnection](report-images/httpurlconnection_main.png)

#### Ключевые фрагменты кода

Проверка наличия интернет-соединения с поддержкой разных версий API:

```java
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
```

Построение URL для запроса погоды на основе координат из IP-ответа:

```java
private String buildWeatherUrl(String latitude, String longitude) {
    return "https://api.open-meteo.com/v1/forecast?latitude="
            + latitude
            + "&longitude="
            + longitude
            + "&current_weather=true";
}
```

Универсальный метод загрузки текста по HTTP с обработкой ошибочных кодов ответа:

```java
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
```

Чтение потока байт с буфером 1024 байта в `ByteArrayOutputStream`:

```java
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
```

Двухэтапная загрузка данных в фоновом потоке:

```java
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
```

Отображение IP-информации из JSON:

```java
private void showIpInfo(JSONObject ipInfo) {
    ipTextView.setText("IP: " + value(ipInfo, "ip"));
    cityTextView.setText("Город: " + value(ipInfo, "city"));
    regionTextView.setText("Регион: " + value(ipInfo, "region"));
    countryTextView.setText("Страна: " + value(ipInfo, "country"));
    locationTextView.setText("Координаты: " + value(ipInfo, "loc"));
    orgTextView.setText("Организация: " + value(ipInfo, "org"));
    timezoneTextView.setText("Часовой пояс: " + value(ipInfo, "timezone"));
}
```

Отображение погодных данных с единицами измерения из поля `current_weather_units`:

```java
private void showWeather(JSONObject weatherInfo) {
    if (weatherInfo == null) {
        weatherStatusTextView.setText("Погода: координаты не получены");
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
    // ...
}
```

![Результат загрузки данных IP и погоды](report-images/httpurlconnection_result.png)

---

### 3.3 Модуль FirebaseAuth — аутентификация Firebase

#### Описание

Модуль демонстрирует интеграцию **Firebase Authentication** с методом входа по электронной почте и паролю. Реализованы четыре операции:

- **Создание аккаунта** (`createUserWithEmailAndPassword`)
- **Вход в систему** (`signInWithEmailAndPassword`)
- **Выход** (`signOut`)
- **Отправка письма для верификации email** (`sendEmailVerification`)

Интерфейс адаптируется к состоянию авторизации: при входе скрываются поля ввода и кнопки регистрации/входа, отображаются кнопки выхода и верификации. Метод `updateUI(FirebaseUser user)` централизованно управляет видимостью элементов.

Перед отправкой запроса выполняется валидация формы: проверяется непустота email и пароля, а также минимальная длина пароля (6 символов).

![Экран FirebaseAuth — вход](report-images/firebaseauth_signin.png)

#### Ключевые фрагменты кода

Инициализация Firebase и восстановление состояния при старте:

```java
mAuth = FirebaseAuth.getInstance();

@Override
protected void onStart() {
    super.onStart();
    FirebaseUser currentUser = mAuth.getCurrentUser();
    updateUI(currentUser);
}
```

Создание нового аккаунта с асинхронным колбэком:

```java
private void createAccount(String email, String password) {
    if (!validateForm()) {
        return;
    }

    mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                        Toast.makeText(MainActivity.this, "Аккаунт создан", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, getErrorMessage(task), Toast.LENGTH_LONG).show();
                        updateUI(null);
                    }
                }
            });
}
```

Вход в систему:

```java
private void signIn(String email, String password) {
    if (!validateForm()) {
        return;
    }

    mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                        Toast.makeText(MainActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                    } else {
                        statusTextView.setText(R.string.auth_failed);
                        Toast.makeText(MainActivity.this, getErrorMessage(task), Toast.LENGTH_LONG).show();
                        updateUI(null);
                    }
                }
            });
}
```

Отправка письма верификации:

```java
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
                        Toast.makeText(
                                MainActivity.this,
                                "Не удалось отправить письмо",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            });
}
```

Управление UI в зависимости от состояния авторизации:

```java
private void updateUI(FirebaseUser user) {
    if (user != null) {
        statusTextView.setText(
                getString(R.string.emailpassword_status_fmt, user.getEmail(), user.isEmailVerified())
        );
        detailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));

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
```

Валидация формы:

```java
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
```

Зависимости в `build.gradle`:

```groovy
implementation 'com.google.firebase:firebase-auth:21.0.1'
implementation 'com.google.android.gms:play-services-auth:20.0.0'
```

![FirebaseAuth — состояние после входа](report-images/firebaseauth_signedin.png)

---

### 3.4 Контрольное задание — MireaProject

#### Описание

В рамках контрольного задания проект **MireaProject** был дополнен двумя компонентами:

1. **`LoginActivity`** — экран аутентификации с Firebase, который устанавливается точкой входа в приложение (LAUNCHER-активити в манифесте).
2. **`NetworkFragment`** — фрагмент в навигационном меню приложения, отображающий текущую погоду для Москвы через API open-meteo.com.

#### 3.4.1 LoginActivity

`LoginActivity` реализует вход в приложение через Firebase Authentication. При запуске проверяется, авторизован ли пользователь уже (`mAuth.getCurrentUser() != null`): если да — происходит немедленный переход на главный экран. В противном случае пользователю предлагается ввести email и пароль и нажать «Войти» или «Создать аккаунт».

При выполнении сетевого запроса к Firebase отображается `ProgressBar`, кнопки и поля блокируются методом `setLoading(true)`. После успешного входа или создания аккаунта приложение переходит на `MainActivity` с флагами `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, чтобы исключить возврат на экран входа кнопкой «Назад».

В `AndroidManifest.xml` `LoginActivity` объявлена как `LAUNCHER`:

```xml
<activity android:name=".LoginActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Ключевые методы `LoginActivity`:

```java
@Override
protected void onStart() {
    super.onStart();
    FirebaseUser currentUser = mAuth.getCurrentUser();
    if (currentUser != null) {
        openMainScreen();
    }
}

private void signIn(String email, String password) {
    if (!validateForm()) return;

    setLoading(true);
    textViewStatus.setText(R.string.login_status_signing_in);

    mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                setLoading(false);

                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    openMainScreen();
                } else {
                    textViewStatus.setText(R.string.login_status_error);
                    Toast.makeText(LoginActivity.this, getErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                }
            });
}

private void openMainScreen() {
    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}

private void setLoading(boolean loading) {
    progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
    buttonSignIn.setEnabled(!loading);
    buttonCreateAccount.setEnabled(!loading);
    editTextEmail.setEnabled(!loading);
    editTextPassword.setEnabled(!loading);
}
```

![Экран входа MireaProject](report-images/mirea_login.png)

#### 3.4.2 NetworkFragment

`NetworkFragment` загружает данные о текущей погоде в Москве с API `open-meteo.com` и отображает их в списке `TextView`. Запрос выполняется автоматически при открытии фрагмента (`loadWeather()` вызывается в `onCreateView()`), а также по нажатию кнопки «Обновить».

Ответ API парсится во вспомогательный объект `WeatherInfo` с полями: `city`, `coordinates`, `temperature`, `windSpeed`, `windDirection`, `weatherCode`, `time`, `rawJson`. Единицы измерения (°C, км/ч, °) берутся из блока `current_weather_units` ответа, если он присутствует.

URL запроса зафиксирован для Москвы с часовым поясом:

```java
private static final String WEATHER_URL =
    "https://api.open-meteo.com/v1/forecast?latitude=55.75&longitude=37.62" +
    "&current_weather=true&timezone=Europe%2FMoscow";
```

Фоновое выполнение запроса:

```java
private void loadWeather() {
    buttonLoadNetwork.setEnabled(false);
    textViewNetworkStatus.setText(R.string.network_status_loading);
    textViewRawNetwork.setText("");

    executorService.execute(() -> {
        try {
            String json = downloadText(WEATHER_URL);
            WeatherInfo weatherInfo = parseWeather(json);

            mainHandler.post(() -> {
                if (getView() == null) return;
                buttonLoadNetwork.setEnabled(true);
                showWeather(weatherInfo);
            });

        } catch (Exception e) {
            mainHandler.post(() -> {
                if (getView() == null) return;
                buttonLoadNetwork.setEnabled(true);
                textViewNetworkStatus.setText(R.string.network_status_error);
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    });
}
```

Парсинг JSON-ответа:

```java
private WeatherInfo parseWeather(String json) throws JSONException {
    JSONObject root = new JSONObject(json);
    JSONObject currentWeather = root.getJSONObject("current_weather");
    JSONObject units = root.optJSONObject("current_weather_units");

    WeatherInfo weatherInfo = new WeatherInfo();
    weatherInfo.city = "Москва";
    weatherInfo.coordinates = "55.75, 37.62";
    weatherInfo.temperature = currentWeather.optString("temperature", "-")
            + " " + getUnit(units, "temperature", "°C");
    weatherInfo.windSpeed = currentWeather.optString("windspeed", "-")
            + " " + getUnit(units, "windspeed", "км/ч");
    weatherInfo.windDirection = currentWeather.optString("winddirection", "-")
            + " " + getUnit(units, "winddirection", "°");
    weatherInfo.weatherCode = currentWeather.optString("weathercode", "-");
    weatherInfo.time = currentWeather.optString("time", "-");
    weatherInfo.rawJson = root.toString(2);

    return weatherInfo;
}
```

![NetworkFragment — данные о погоде](report-images/mirea_network.png)

---

## 4. Результаты работы

В ходе практической работы реализованы и изучены следующие механизмы сетевого взаимодействия в Android:

| Модуль | Технология | Результат |
|---|---|---|
| TimeService | TCP-сокет (`java.net.Socket`) | Получение точного времени UTC с сервера NIST, парсинг и форматирование даты |
| HttpURLConnection | `HttpURLConnection` + `org.json` | Двухэтапный запрос: IP-геолокация → координаты → погода |
| FirebaseAuth | Firebase Authentication | Регистрация, вход, выход, отправка письма верификации |
| MireaProject LoginActivity | Firebase Authentication | Экран входа как точка запуска приложения, авто-переход при наличии сессии |
| MireaProject NetworkFragment | `HttpURLConnection` + `org.json` | Отображение текущей погоды Москвы с автообновлением |

Освоен современный паттерн асинхронной работы: `ExecutorService` для выполнения сетевого кода в фоне и `Handler(Looper.getMainLooper()).post()` для обновления UI — в качестве замены устаревшего `AsyncTask`. Реализована проверка интернет-соединения через `ConnectivityManager` с поддержкой как нового (`NetworkCapabilities`), так и устаревшего API.
