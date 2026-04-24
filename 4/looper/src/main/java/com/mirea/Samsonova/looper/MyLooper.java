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