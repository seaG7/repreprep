# Отчет по практической работе №4

**Выполнил:** Студент группы БСБО-09-23 **ФИО:** Самсонова Ольга Павловна **Дисциплина:** Разработка мобильных приложений

---

## 1. Цель работы

Целью данной работы является глубокое изучение механизмов асинхронного программирования в среде Android. В ходе работы необходимо освоить методы выноса трудоемких операций (вычисления, работа с сетью, файлами) из главного потока (UI-thread) для предотвращения зависания интерфейса (ошибки ANR). Основное внимание уделяется работе с классом `Thread`, механизмам обратного вызова в UI-поток, организации очередей сообщений через `Handler` и `Looper`, использованию загрузчиков `Loader`, реализации фоновых служб `Service` и современных планировщиков задач `WorkManager`. Также ставится задача по внедрению асинхронных технологий в итоговый проект `MireaProject` через систему фрагментов и навигации.

---

## 2. Архитектура проекта

Проект реализован по модульному принципу, где каждый компонент демонстрирует специфический аспект асинхронности:

1. **app** — базовый модуль. Изучение **ViewBinding** для замены `findViewById` и обеспечения безопасности типов при работе с ресурсами разметки.
2. **thread** — расчет среднего количества пар. Использование классических потоков **Thread** и метода `runOnUiThread()` для обновления интерфейса.
3. **data_thread** — сравнительный анализ методов взаимодействия с UI: `runOnUiThread`, `View.post` и `View.postDelayed`.
4. **looper** — реализация кастомного потока с очередью сообщений. Изучение работы **Handler**, **Message** и жизненного цикла **Looper**.
5. **cryptoloader** — криптографический модуль. Применение **AsyncTaskLoader** для фоновой расшифровки данных алгоритмом AES.
6. **serviceapp** — мультимедийный модуль. Реализация **Foreground Service** с использованием **MediaPlayer** и системных уведомлений.
7. **workmanager** — планирование фоновых работ. Настройка условий запуска (**Constraints**) и отслеживание статуса задачи.
8. **MireaProject** — интеграция **WorkManager** в общую архитектуру проекта на базе **Navigation Drawer**.

---

## 3. Ход работы

### 3.1. Модуль app — Внедрение технологии ViewBinding

На первом этапе был настроен механизм **ViewBinding**. В отличие от `findViewById`, этот подход генерирует типизированный класс привязки для каждого XML-файла, что исключает ошибки во время выполнения. В файле `build.gradle` (Module: app) был добавлен соответствующий блок настроек.

В классе `MainActivity` был инициализирован объект `binding` через статический метод `inflate(getLayoutInflater())`. Это позволило получить доступ к `textViewMirea` и `buttonMirea` как к свойствам объекта, обеспечивая проверку наличия элементов еще на этапе компиляции. Был реализован слушатель нажатий, который меняет текст в поле и выводит отладочное сообщение в `Logcat`.

**Рисунок 1: Главный экран модуля app.** (Место для скриншота)

**Листинг** `MainActivity.java`:

```java
package com.mirea.Samsonova.lesson4;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.mirea.Samsonova.lesson4.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonMirea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.textViewMirea.setText("ViewBinding работает!");
                Log.d(MainActivity.class.getSimpleName(), "Кнопка нажата");
            }
        });
    }
}
```

**Листинг** `activity_main.xml`:

```java
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/textViewMirea"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Результат будет здесь"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <Button
        android:id="@+id/buttonMirea"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Нажми меня"
        app:layout_constraintTop_toBottomOf="@id/textViewMirea"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### 3.2. Модуль thread — Фоновые вычисления и взаимодействие с UI

В модуле `thread` была решена задача вычисления среднего количества учебных пар. Основная логика была вынесена в новый экземпляр класса `Thread`. Это критически важно, так как выполнение математических операций в главном потоке может привести к блокировке обработки касаний.

Внутри метода `run()` созданного потока считывались данные из полей ввода. Для вывода результата обратно в `TextView` использовался метод `runOnUiThread()`. Это необходимо, так как Android запрещает изменять компоненты графического интерфейса из любых потоков, кроме главного.

**Рисунок 2: Выполнение расчета среднего количества пар в фоновом режиме.** (Место для скриншота)

**Листинг** `MainActivity.java` (модуль thread):

```java
package com.mirea.Samsonova.thread;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.mirea.Samsonova.thread.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonCalc.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    float pairs = Float.parseFloat(binding.editPairs.getText().toString());
                    float days = Float.parseFloat(binding.editDays.getText().toString());
                    float result = pairs / days;
                    runOnUiThread(() -> binding.textResult.setText("Среднее пар в день: " + result));
                } catch (Exception e) {
                    runOnUiThread(() -> binding.textResult.setText("Ошибка в данных"));
                }
            }).start();
        });
    }
}
```

**Листинг** `activity_main.xml`:

```java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText android:id="@+id/editPairs" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="Общее кол-во пар" />
    <EditText android:id="@+id/editDays" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="Кол-во учебных дней" />
    <Button android:id="@+id/buttonCalc" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Посчитать среднее" />
    <TextView android:id="@+id/textResult" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="18sp" />
</LinearLayout>
```

---

### 3.3. Модуль data_thread — Методы обновления пользовательского интерфейса

Этот модуль был посвящен изучению низкоуровневых способов взаимодействия потоков. Было реализовано приложение, которое имитирует последовательность действий с задержками через `TimeUnit.SECONDS.sleep()`.

Для вывода логов на экран были протестированы три метода:

1. `runOnUiThread(Runnable)` — немедленное исполнение в UI-потоке.
2. `tvInfo.post(Runnable)` — добавление задачи в очередь сообщений компонента.
3. `tvInfo.postDelayed(Runnable, long)` — отложенное выполнение задачи через заданный интервал (в данном случае 2 секунды).

**Рисунок 3: Результат последовательного выполнения задач в модуле data_thread.** (Место для скриншота)

**Листинг** `MainActivity.java` (модуль data_thread):

```java
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
```

**Листинг** `activity_main.xml`:

```java
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <TextView xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@+id/tvInfo"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:lines="10"
        android:maxLines="10"
        android:textSize="16sp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### 3.4. Модуль looper — Организация фоновой очереди через Handler

В модуле `looper` был реализован более сложный сценарий: создание фонового потока, который постоянно «слушает» входящие сообщения. Класс `MyLooper` расширяет `Thread` и инициализирует цикл обработки через `Looper.prepare()` и `Looper.loop()`.

В `MainActivity` при нажатии на кнопку формируется объект `Message`. С помощью `Bundle` в него упаковываются данные о работе и возрасте. `Handler` фонового потока извлекает эти данные, имитирует задержку и отправляет ответный `Message` в `Handler` главного потока для логирования результата.

**Рисунок 4: Интерфейс модуля looper и лог обработки данных.** (Место для скриншота)

**Листинг** `MyLooper.java`:

```java
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
```

**Листинг** `MyLooper.java`:

```java
package com.mirea.Samsonova.looper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class MyLooper extends Thread {
    public Handler mHandler;
    private Handler mainHandler;

    public MyLooper(Handler mainThreadHandler) {
        mainHandler = mainThreadHandler;
    }

    public void run() {
        Log.d("MyLooper", "run");
        Looper.prepare();
        mHandler = new Handler(Looper.myLooper()) {
            public void handleMessage(Message msg) {
                String work = msg.getData().getString("WORK");
                int age = msg.getData().getInt("AGE");
                try { Thread.sleep(age * 1000); } catch (InterruptedException e) {}

                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("result", "Лет: " + age + ", Работа: " + work);
                message.setData(bundle);
                mainHandler.sendMessage(message);
            }
        };
        Looper.loop();
    }
}
```

**Листинг** `activity_main.xml`:

```java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:gravity="center">

    <EditText android:id="@+id/editAge" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="Ваш возраст" android:inputType="number" />
    <EditText android:id="@+id/editWork" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="Ваша работа" />
    <Button android:id="@+id/buttonMirea" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Отправить в поток" />
</LinearLayout>
```

---

### 3.5. Модуль cryptoloader — Использование механизмов Loader и AES-шифрования

В этом модуле реализован асинхронный механизм загрузки данных. Пользователь вводит фразу, которая шифруется симметричным алгоритмом AES прямо в `MainActivity` с использованием `KeyGenerator` и класса `Cipher`.

Затем зашифрованные байты и ключ передаются в `MyLoader`, наследующий `AsyncTaskLoader<String>`. Метод `loadInBackground()` выполняет обратную операцию дешифрования. Использование лоадеров позволяет приложению корректно обрабатывать поворот экрана: лоадер не перезапускается, а просто переподключается к активности, сохраняя результат.

**Рисунок 5: Процесс дешифрования фразы через Loader.** (Место для скриншота)

**Листинг** `MyLoader.java`:

```java
package com.mirea.Samsonova.cryptoloader;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.loader.content.AsyncTaskLoader;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MyLoader extends AsyncTaskLoader<String> {
    private byte[] cryptText;
    private byte[] key;

    public MyLoader(@NonNull Context context, Bundle args) {
        super(context);
        if (args != null) {
            cryptText = args.getByteArray("text");
            key = args.getByteArray("key");
        }
    }

    @Override
    protected void onStartLoading() { forceLoad(); }

    @Override
    public String loadInBackground() {
        try {
            SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(cryptText));
        } catch (Exception e) { return null; }
    }
}
```

**Листинг** `MainActivity.java` (модуль cryptoloader):

```java
package com.mirea.Samsonova.cryptoloader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.os.Bundle;
import android.widget.Toast;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import com.mirea.Samsonova.cryptoloader.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {
    private ActivityMainBinding binding;
    private final int LoaderID = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonMirea.setOnClickListener(v -> {
            try {
                String text = binding.editTextMirea.getText().toString();
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                SecretKey key = kg.generateKey();

                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.ENCRYPT_MODE, key);
                byte[] encrypted = c.doFinal(text.getBytes());

                Bundle b = new Bundle();
                b.putByteArray("text", encrypted);
                b.putByteArray("key", key.getEncoded());
                LoaderManager.getInstance(this).initLoader(LoaderID, b, this);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        return new MyLoader(this, args);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        Toast.makeText(this, "Расшифровано: " + data, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {}
}
```

**Листинг** `activity_main.xml`:

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="16dp">

    <EditText android:id="@+id/editTextMirea" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="Введите фразу для шифрования" />
    <Button android:id="@+id/buttonMirea" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Зашифровать и отправить" />
</LinearLayout>
```

---

### 3.6. Модуль serviceapp — Реализация фонового музыкального сервиса

Данный модуль демонстрирует работу с компонентом `Service`. Был реализован `PlayerService`, предназначенный для проигрывания музыки в фоновом режиме. Согласно современным требованиям Android, сервис запускается как `Foreground Service`.

В методе `onCreate()` сервиса создается `NotificationChannel` и формируется уведомление через `NotificationCompat.Builder`, в котором указана любимая композиция. Сервис переходит в активное состояние через `startForeground()`. Управление (Play/Stop) осуществляется из `MainActivity` через вызовы `startForegroundService()` и `stopService()`.

**Рисунок 6: Уведомление музыкального сервиса в панели уведомлений.** (Место для скриншота)

**Листинг** `PlayerService.java`:

```java
package com.mirea.Samsonova.serviceapp;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class PlayerService extends Service {
    private MediaPlayer mediaPlayer;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @SuppressLint({"DiscouragedApi", "ForegroundServiceType"})
    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Service", NotificationManager.IMPORTANCE_DEFAULT);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Плеер")
                .setContentText("Играет: A$AP Rocky - STAY HERE 4 LIFE")
                .setSmallIcon(android.R.drawable.ic_media_play);

        startForeground(1, builder.build());

        mediaPlayer = MediaPlayer.create(this, getResources().getIdentifier("music", "raw", getPackageName()));
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
```

**Листинг** `MainActivity.java`(модуль serviceapp): 

```java
package com.mirea.Samsonova.serviceapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        Button buttonPlay = findViewById(R.id.buttonPlay);
        Button buttonStop = findViewById(R.id.buttonStop);

        buttonPlay.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(MainActivity.this, PlayerService.class);
            startForegroundService(serviceIntent);
            Toast.makeText(this, "Музыка запущена", Toast.LENGTH_SHORT).show();
        });

        buttonStop.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, PlayerService.class));
            Toast.makeText(this, "Музыка остановлена", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
```

**Листинг** `activity_main.xml`:

```java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center">

    <Button android:id="@+id/buttonPlay" android:layout_width="200dp" android:layout_height="wrap_content" android:text="Play Music" />
    <Button android:id="@+id/buttonStop" android:layout_width="200dp" android:layout_height="wrap_content" android:text="Stop Music" />
</LinearLayout>
```

---

### 3.7. Модуль workmanager — Надежное планирование задач

Модуль посвящен изучению `WorkManager`. Была создана задача `UploadWorker`, расширяющая класс `Worker`. В методе `doWork()` описана логика фоновой задачи.

Главной особенностью стала настройка условий выполнения через класс `Constraints`. Задача была настроена на запуск только при наличии подключения к сети (NetworkType.CONNECTED). Запрос формируется через `OneTimeWorkRequest` и передается в систему для исполнения.

**Рисунок 7: Экран модуля workmanager.** (Место для скриншота)

**Листинг** `MainActivity.java` (модуль workmanager):

```java
package com.mirea.Samsonova.workmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueue(uploadWorkRequest);
    }
}
```

**Листинг** `UploadWorker.java`:

```java
package com.mirea.Samsonova.workmanager;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;

public class UploadWorker extends Worker {
    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        Log.d("UploadWorker", "Начало работы");
        try { TimeUnit.SECONDS.sleep(5); } catch (Exception ignored) {}
        Log.d("UploadWorker", "Конец работы");
        return Result.success();
    }
}
```

**Листинг** `activity_main.xml`:

```java
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="WorkManager запущен..."
        android:id="@+id/textView"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        xmlns:app="http://schemas.android.com/apk/res-auto" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### 3.8. Контрольное задание — Проект MireaProject

В рамках контрольного задания в итоговый проект `MireaProject` был добавлен новый функционал фоновой обработки. Был создан отдельный фрагмент `WorkerFragment`, который интегрирован в общую навигацию приложения.

**Техническая реализация:**

1. **Навигация:** В файле `mobile_navigation.xml` добавлен новый узел `<fragment>` с ID `nav_worker`.
2. **Меню:** В `activity_main_drawer.xml` добавлен пункт меню для доступа к фрагменту.
3. **Логика:** Во фрагменте реализован запуск `OneTimeWorkRequest` для класса `MyWorker`. Благодаря использованию `LiveData` (метод `getWorkInfoByIdLiveData`), текстовое поле на экране динамически отображает текущее состояние задачи: от постановки в очередь до успешного завершения.

Это позволило продемонстрировать умение интегрировать сложные асинхронные механизмы в существующую архитектуру приложения с использованием компонентов `Architecture Components`.

**Рисунок 8: Новый пункт в боковом меню MireaProject.** (Место для скриншота)

**Рисунок 9: Фрагмент фоновой задачи с отображением статуса выполнения.** (Место для скриншота)

**Листинг** `WorkerFragment.java`:

```java
package com.mirea.Samsonova.mireaproject.ui.worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.mirea.Samsonova.mireaproject.MyWorker;
import com.mirea.Samsonova.mireaproject.databinding.FragmentWorkerBinding;

public class WorkerFragment extends Fragment {
    private FragmentWorkerBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnStartWork.setOnClickListener(v -> {
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class).build();
            WorkManager.getInstance(requireContext()).enqueue(workRequest);

            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(workRequest.getId())
                    .observe(getViewLifecycleOwner(), workInfo -> {
                        if (workInfo != null) {
                            binding.textStatus.setText("Статус: " + workInfo.getState().name());
                        }
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

**Листинг** `MyWorker.java`:

```java
package com.mirea.Samsonova.mireaproject;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;

public class MyWorker extends Worker {
    public MyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("MireaProjectWorker", "Фоновая задача началась");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            return Result.failure();
        }
        Log.d("MireaProjectWorker", "Фоновая задача успешно завершена");
        return Result.success();
    }
}
```

**Листинг** `activity_main_drawer.xml`:

```java
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">

    <group android:checkableBehavior="single">

        <item
            android:id="@+id/nav_home"
            android:icon="@android:drawable/ic_menu_view"
            android:title="@string/menu_home" />

        <item
            android:id="@+id/nav_data"
            android:icon="@android:drawable/ic_menu_info_details"
            android:title="@string/menu_data" />

        <item
            android:id="@+id/nav_webview"
            android:icon="@android:drawable/ic_menu_search"
            android:title="@string/menu_webview" />
        <item
            android:id="@+id/nav_worker"
            android:icon="@android:drawable/ic_menu_manage"
            android:title="Фоновая задача" />

    </group>
</menu>
```

**Листинг** `mobile_navigation.xml`:

```java
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/mobile_navigation"
    app:startDestination="@id/nav_home">

    <fragment
        android:id="@+id/nav_home"
        android:name="com.mirea.Samsonova.mireaproject.ui.home.HomeFragment"
        android:label="@string/menu_home"
        tools:layout="@layout/fragment_home" />

    <fragment
        android:id="@+id/nav_data"
        android:name="com.mirea.Samsonova.mireaproject.ui.data.DataFragment"
        android:label="@string/menu_data"
        tools:layout="@layout/fragment_data" />

    <fragment
        android:id="@+id/nav_webview"
        android:name="com.mirea.Samsonova.mireaproject.ui.webview.WebViewFragment"
        android:label="@string/menu_webview"
        tools:layout="@layout/fragment_web_view" />
    <fragment
        android:id="@+id/nav_worker"
        android:name="com.mirea.Samsonova.mireaproject.ui.worker.WorkerFragment"
        android:label="Background Task"
        tools:layout="@layout/fragment_worker" />
</navigation>
```

## 4. Результаты работы

В ходе выполнения практической работы №4 были достигнуты следующие результаты:

1. Освоена технология **ViewBinding**, обеспечивающая более безопасный и чистый код взаимодействия с UI.
2. Изучены принципы работы многопоточности и правила взаимодействия фоновых потоков с главным потоком через `runOnUiThread` и `Handler`.
3. Реализована фоновая обработка данных с сохранением состояния при конфигурационных изменениях через механизмы **Loaders**.
4. Разработан музыкальный плеер на базе **Foreground Service**, что позволило закрепить знания о жизненном цикле сервисов и системных уведомлениях.
5. Изучен и применен на практике **WorkManager** для гарантированного выполнения задач с учетом системных ограничений.
6. Все изученные технологии были успешно интегрированы в комплексный проект **MireaProject**, что подтвердило понимание архитектурных принципов Android-разработки.

Работа выполнена в полном объеме, все модули функционируют корректно.