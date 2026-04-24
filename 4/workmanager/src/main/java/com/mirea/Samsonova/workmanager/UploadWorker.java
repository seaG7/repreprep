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