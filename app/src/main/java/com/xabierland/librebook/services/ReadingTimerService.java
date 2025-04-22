package com.xabierland.librebook.services;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.ReadingTimerActivity;

import java.util.concurrent.TimeUnit;

public class ReadingTimerService extends Service {

    private static final String TAG = "ReadingTimerService";
    public static final String CHANNEL_ID = "reading_timer_channel";
    private static final int NOTIFICATION_ID = 1;

    // Acciones para la notificación
    public static final String ACTION_START = "com.xabierland.librebook.action.START";
    public static final String ACTION_STOP = "com.xabierland.librebook.action.STOP";
    public static final String ACTION_RESET = "com.xabierland.librebook.action.RESET";

    // Binder para interacción con actividades
    private final IBinder binder = new LocalBinder();

    // Variables para el temporizador
    private boolean isTimerRunning = false;
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long pausedTime = 0L;
    private long elapsedTime = 0L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimerRunning) {
                timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
                elapsedTime = pausedTime + timeInMilliseconds;
                updateNotification();
                // Notificar a los oyentes
                if (timerListener != null) {
                    timerListener.onTimerTick(elapsedTime);
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    // Interfaz para comunicación con la actividad
    public interface TimerListener {
        void onTimerTick(long elapsedTime);
        void onTimerStateChanged(boolean isRunning);
    }

    private TimerListener timerListener;

    public class LocalBinder extends Binder {
        public ReadingTimerService getService() {
            return ReadingTimerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio creado");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_START:
                        startTimer();
                        break;
                    case ACTION_STOP:
                        stopTimer();
                        break;
                    case ACTION_RESET:
                        resetTimer();
                        break;
                }
            }
        }

        // Iniciar como un servicio en primer plano para evitar que el sistema lo cierre
        startForeground(NOTIFICATION_ID, buildNotification());
        
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reading Timer Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        // 1) Intent para abrir la actividad
        Intent notificationIntent = new Intent(this, ReadingTimerActivity.class);
        // Usamos FLAG_CLEAR_TOP para no destruir el servicio ni reiniciar el temporizador,
        // y FLAG_NEW_TASK para permitir el lanzamiento desde background.
        notificationIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        // 2) Permitimos explícitamente el Background Activity Launch (BAL)
        ActivityOptions options = ActivityOptions.makeBasic();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            options.setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            );
        }
        Bundle optsBundle = options.toBundle();

        // 3) Creamos el PendingIntent con esas opciones
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE,
                optsBundle
        );

        // 4) Acción “Parar” sigue igual
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                new Intent(this, ReadingTimerService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE
        );

        // 5) Montamos la notificación con el timeString ya formateado
        String timeStr = formatTime(elapsedTime);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.timer_notification_title))
                .setContentText(timeStr)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .addAction(
                        android.R.drawable.ic_media_pause,
                        getString(R.string.stop),
                        stopPendingIntent
                )
                .setOnlyAlertOnce(true)
                .build();
    }
    private void updateNotification() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, buildNotification());
    }

    // Métodos para controlar el temporizador
    public void startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true;
            
            // Si es un reinicio después de pausa
            if (pausedTime > 0) {
                startTime = SystemClock.uptimeMillis();
            } else {
                startTime = SystemClock.uptimeMillis();
            }
            
            handler.post(updateTimerRunnable);
            
            // Notificar a los oyentes
            if (timerListener != null) {
                timerListener.onTimerStateChanged(true);
            }
            
            updateNotification();
        }
    }

    public void stopTimer() {
        if (isTimerRunning) {
            isTimerRunning = false;
            handler.removeCallbacks(updateTimerRunnable);
            pausedTime = elapsedTime;
            
            // Notificar a los oyentes
            if (timerListener != null) {
                timerListener.onTimerStateChanged(false);
            }
            
            updateNotification();
        }
    }

    public void resetTimer() {
        isTimerRunning = false;
        handler.removeCallbacks(updateTimerRunnable);
        startTime = 0L;
        pausedTime = 0L;
        elapsedTime = 0L;
        
        // Notificar a los oyentes
        if (timerListener != null) {
            timerListener.onTimerTick(0);
            timerListener.onTimerStateChanged(false);
        }
        
        updateNotification();
    }

    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setTimerListener(TimerListener listener) {
        this.timerListener = listener;
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimerRunnable);
        Log.d(TAG, "Servicio destruido");
    }
}