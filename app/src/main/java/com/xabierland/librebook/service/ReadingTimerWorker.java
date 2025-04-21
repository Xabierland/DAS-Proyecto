package com.xabierland.librebook.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.ReadingTimerActivity;
import com.xabierland.librebook.utils.NotificationUtils;

import java.util.concurrent.TimeUnit;

/**
 * Worker para registrar el tiempo de lectura en segundo plano
 */
public class ReadingTimerWorker extends Worker {
    
    private static final String TAG = "ReadingTimerWorker";
    private static final String CHANNEL_ID = "reading_timer_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Claves para SharedPreferences
    public static final String PREF_FILE_READING_TIMER = "reading_timer_prefs";
    public static final String PREF_IS_TIMER_RUNNING = "is_timer_running";
    public static final String PREF_START_TIME = "start_time";
    public static final String PREF_ELAPSED_TIME = "elapsed_time";
    public static final String PREF_LIBRO_ID = "libro_id";
    public static final String PREF_LIBRO_TITULO = "libro_titulo";

    public ReadingTimerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        
        // Crear canal de notificación
        createNotificationChannel(context);
        
        // Obtener datos de entrada
        int libroId = getInputData().getInt(PREF_LIBRO_ID, -1);
        String libroTitulo = getInputData().getString(PREF_LIBRO_TITULO);
        
        // Comprobar si el timer está en ejecución
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        boolean isTimerRunning = prefs.getBoolean(PREF_IS_TIMER_RUNNING, false);
        
        if (isTimerRunning) {
            long startTime = prefs.getLong(PREF_START_TIME, 0);
            long previousElapsedTime = prefs.getLong(PREF_ELAPSED_TIME, 0);
            
            // Calcular tiempo transcurrido
            long currentTime = System.currentTimeMillis();
            long elapsedSinceStart = currentTime - startTime;
            long totalElapsedTime = previousElapsedTime + elapsedSinceStart;
            
            // Actualizar tiempo transcurrido en SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(PREF_ELAPSED_TIME, totalElapsedTime);
            editor.putLong(PREF_START_TIME, currentTime); // Actualizar tiempo de inicio
            editor.apply();
            
            // Mostrar notificación con tiempo de lectura
            showTimerNotification(context, libroTitulo, formatTime(totalElapsedTime), libroId);
            
            Log.d(TAG, "Timer actualizado. Tiempo total: " + formatTime(totalElapsedTime));
            
            return Result.success();
        }
        
        // Si el timer no está en ejecución pero se ejecuta el worker, detener
        return Result.success();
    }
    
    /**
     * Crea el canal de notificación para Android 8.0+
     */
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.reading_timer_channel_name);
            String description = context.getString(R.string.reading_timer_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Muestra una notificación con el tiempo de lectura actual
     */
    private void showTimerNotification(Context context, String bookTitle, String elapsedTime, int libroId) {
        // Intent para abrir la actividad al tocar la notificación
        Intent intent = new Intent(context, ReadingTimerActivity.class);
        intent.putExtra(PREF_LIBRO_ID, libroId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Intent para detener el cronómetro
        Intent stopIntent = new Intent(context, ReadingTimerReceiver.class);
        stopIntent.setAction(ReadingTimerReceiver.ACTION_STOP_TIMER);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reading_timer_notification_title))
                .setContentText(context.getString(R.string.reading_timer_notification_text, 
                        bookTitle, elapsedTime))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_pause, 
                        context.getString(R.string.stop), stopPendingIntent);
        
        // Mostrar la notificación
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    
    /**
     * Formatea el tiempo en milisegundos a formato legible
     */
    private String formatTime(long timeMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeMillis);
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Crea los datos de entrada para el worker
     */
    public static Data createInputData(int libroId, String libroTitulo) {
        return new Data.Builder()
                .putInt(PREF_LIBRO_ID, libroId)
                .putString(PREF_LIBRO_TITULO, libroTitulo)
                .build();
    }
}