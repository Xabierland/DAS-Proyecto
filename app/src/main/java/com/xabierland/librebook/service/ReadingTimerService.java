package com.xabierland.librebook.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.ReadingTimerActivity;

import java.util.concurrent.TimeUnit;

public class ReadingTimerService extends Service {
    
    private static final String TAG = "ReadingTimerService";
    private static final String CHANNEL_ID = "reading_timer_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Claves para SharedPreferences (trasladadas de ReadingTimerWorker)
    public static final String PREF_FILE_READING_TIMER = "reading_timer_prefs";
    public static final String PREF_IS_TIMER_RUNNING = "is_timer_running";
    public static final String PREF_START_TIME = "start_time";
    public static final String PREF_ELAPSED_TIME = "elapsed_time";
    public static final String PREF_LIBRO_ID = "libro_id";
    public static final String PREF_LIBRO_TITULO = "libro_titulo";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTimerRunnable;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Crear canal de notificación para Android 8.0+
        createNotificationChannel();
        
        // Inicializar el Runnable que actualizará el timer
        updateTimerRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimer();
                // Programar la próxima actualización después de 1 segundo
                handler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        // Obtener datos del libro
        int libroId = intent.getIntExtra(PREF_LIBRO_ID, -1);
        String libroTitulo = intent.getStringExtra(PREF_LIBRO_TITULO);
        
        if (libroId == -1 || libroTitulo == null) {
            Log.e(TAG, "Error: ID del libro o título no proporcionados");
            stopSelf();
            return START_NOT_STICKY;
        }
        
        // Guardar estado inicial del timer
        SharedPreferences prefs = getSharedPreferences(
                PREF_FILE_READING_TIMER, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Si el timer no estaba corriendo, inicializar valores
        if (!prefs.getBoolean(PREF_IS_TIMER_RUNNING, false)) {
            editor.putBoolean(PREF_IS_TIMER_RUNNING, true);
            editor.putLong(PREF_START_TIME, System.currentTimeMillis());
            editor.putInt(PREF_LIBRO_ID, libroId);
            editor.putString(PREF_LIBRO_TITULO, libroTitulo);
            editor.apply();
        }
        
        // Iniciar el servicio en primer plano con la notificación inicial
        startForeground(NOTIFICATION_ID, buildNotification(libroTitulo, "00:00:00"));
        
        // Comenzar las actualizaciones periódicas
        handler.post(updateTimerRunnable);
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Detener las actualizaciones
        handler.removeCallbacks(updateTimerRunnable);
        
        // Guardar el tiempo acumulado
        saveElapsedTime();
        
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void updateTimer() {
        SharedPreferences prefs = getSharedPreferences(
                PREF_FILE_READING_TIMER, MODE_PRIVATE);
        
        // Verificar si el timer debe seguir ejecutándose
        boolean isRunning = prefs.getBoolean(PREF_IS_TIMER_RUNNING, false);
        if (!isRunning) {
            stopSelf();
            return;
        }
        
        // Calcular tiempo transcurrido
        long startTime = prefs.getLong(PREF_START_TIME, 0);
        long previousElapsed = prefs.getLong(PREF_ELAPSED_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long totalElapsedTime = previousElapsed + (currentTime - startTime);
        
        // Actualizar la notificación con el tiempo actual
        String libroTitulo = prefs.getString(PREF_LIBRO_TITULO, "");
        String formattedTime = formatElapsedTime(totalElapsedTime);
        
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, buildNotification(libroTitulo, formattedTime));
        
        // Enviar broadcast para actualizar la UI
        Intent updateIntent = new Intent(ReadingTimerReceiver.ACTION_TIMER_TICK);
        sendBroadcast(updateIntent);
    }
    
    private void saveElapsedTime() {
        SharedPreferences prefs = getSharedPreferences(
                PREF_FILE_READING_TIMER, MODE_PRIVATE);
        
        // Si el timer está ejecutándose, guardar el tiempo acumulado
        if (prefs.getBoolean(PREF_IS_TIMER_RUNNING, false)) {
            long startTime = prefs.getLong(PREF_START_TIME, 0);
            long previousElapsed = prefs.getLong(PREF_ELAPSED_TIME, 0);
            long currentTime = System.currentTimeMillis();
            long totalElapsedTime = previousElapsed + (currentTime - startTime);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(PREF_ELAPSED_TIME, totalElapsedTime);
            editor.putLong(PREF_START_TIME, currentTime);
            editor.apply();
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.reading_timer_channel_name);
            String description = getString(R.string.reading_timer_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW; // Bajo para no interrumpir al usuario
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private Notification buildNotification(String bookTitle, String elapsedTime) {
        // Intent para abrir la actividad al tocar la notificación
        Intent intent = new Intent(this, ReadingTimerActivity.class);
        intent.putExtra(PREF_LIBRO_ID, 
                getSharedPreferences(PREF_FILE_READING_TIMER, MODE_PRIVATE)
                        .getInt(PREF_LIBRO_ID, -1));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Intent para detener el cronómetro
        Intent stopIntent = new Intent(this, ReadingTimerReceiver.class);
        stopIntent.setAction(ReadingTimerReceiver.ACTION_STOP_TIMER);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Construir la notificación
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.reading_timer_notification_title))
                .setContentText(getString(R.string.reading_timer_notification_text, 
                        bookTitle, elapsedTime))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_pause, 
                        getString(R.string.stop), stopPendingIntent)
                .build();
    }
    
    private String formatElapsedTime(long timeMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeMillis);
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    // Método de utilidad trasladado de ReadingTimerWorker
    public static Data createInputData(int libroId, String libroTitulo) {
        return new Data.Builder()
                .putInt(PREF_LIBRO_ID, libroId)
                .putString(PREF_LIBRO_TITULO, libroTitulo)
                .build();
    }
}