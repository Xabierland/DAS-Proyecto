package com.xabierland.librebook.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Receptor para gestionar acciones del cronómetro de lectura
 */
public class ReadingTimerReceiver extends BroadcastReceiver {

    private static final String TAG = "ReadingTimerReceiver";
    
    // Acciones
    public static final String ACTION_START_TIMER = "com.xabierland.librebook.action.START_TIMER";
    public static final String ACTION_STOP_TIMER = "com.xabierland.librebook.action.STOP_TIMER";
    public static final String ACTION_RESET_TIMER = "com.xabierland.librebook.action.RESET_TIMER";
    
    // Nombre único para el trabajo periódico
    public static final String WORK_NAME = "reading_timer_work";
    
    // ID de la notificación del timer
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Acción recibida: " + intent.getAction());
        
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerWorker.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        switch (action) {
            case ACTION_START_TIMER:
                // Obtener datos del libro
                int libroId = intent.getIntExtra(ReadingTimerWorker.PREF_LIBRO_ID, -1);
                String libroTitulo = intent.getStringExtra(ReadingTimerWorker.PREF_LIBRO_TITULO);
                
                if (libroId == -1 || libroTitulo == null) {
                    Log.e(TAG, "Error: ID del libro o título no proporcionados");
                    return;
                }
                
                // Guardar estado inicial del timer
                editor.putBoolean(ReadingTimerWorker.PREF_IS_TIMER_RUNNING, true);
                editor.putLong(ReadingTimerWorker.PREF_START_TIME, System.currentTimeMillis());
                editor.putInt(ReadingTimerWorker.PREF_LIBRO_ID, libroId);
                editor.putString(ReadingTimerWorker.PREF_LIBRO_TITULO, libroTitulo);
                editor.apply();
                
                // Programar el worker periódico
                schedulePeriodicWork(context, libroId, libroTitulo);
                break;
                
            case ACTION_STOP_TIMER:
                // Pausar el timer
                boolean isRunning = prefs.getBoolean(ReadingTimerWorker.PREF_IS_TIMER_RUNNING, false);
                
                if (isRunning) {
                    // Calcular tiempo transcurrido
                    long startTime = prefs.getLong(ReadingTimerWorker.PREF_START_TIME, 0);
                    long previousElapsed = prefs.getLong(ReadingTimerWorker.PREF_ELAPSED_TIME, 0);
                    long currentTime = System.currentTimeMillis();
                    long newElapsed = previousElapsed + (currentTime - startTime);
                    
                    // Actualizar tiempo acumulado y marcar como pausado
                    editor.putBoolean(ReadingTimerWorker.PREF_IS_TIMER_RUNNING, false);
                    editor.putLong(ReadingTimerWorker.PREF_ELAPSED_TIME, newElapsed);
                    editor.apply();
                    
                    // Cancelar el trabajo periódico
                    WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
                    
                    // Cancelar la notificación
                    NotificationManager notificationManager = 
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_ID);
                    
                    Log.d(TAG, "Timer detenido. Tiempo acumulado: " + formatElapsedTime(newElapsed));
                }
                break;
                
            case ACTION_RESET_TIMER:
                // Reiniciar el timer
                editor.putBoolean(ReadingTimerWorker.PREF_IS_TIMER_RUNNING, false);
                editor.putLong(ReadingTimerWorker.PREF_ELAPSED_TIME, 0);
                editor.putLong(ReadingTimerWorker.PREF_START_TIME, 0);
                editor.apply();
                
                // Cancelar el trabajo periódico
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
                
                // Cancelar la notificación
                NotificationManager notificationManager = 
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
                
                Log.d(TAG, "Timer reiniciado");
                break;
        }
    }
    
    /**
     * Programa el trabajo periódico para actualizar el cronómetro
     */
    private void schedulePeriodicWork(Context context, int libroId, String libroTitulo) {
        // Crear solicitud de trabajo periódico que se ejecute cada segundo
        // Reducimos el intervalo a 1 segundo para actualizaciones más frecuentes
        PeriodicWorkRequest timerWorkRequest =
                new PeriodicWorkRequest.Builder(ReadingTimerWorker.class, 1, TimeUnit.SECONDS)
                        .setInputData(ReadingTimerWorker.createInputData(libroId, libroTitulo))
                        .build();
        
        // Programar trabajo único reemplazando cualquier existente
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        timerWorkRequest);
        
        Log.d(TAG, "Worker programado para actualizar cada segundo");
    }
    
    /**
     * Método de utilidad para comprobar si el cronómetro está en ejecución
     */
    public static boolean isTimerRunning(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerWorker.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        return prefs.getBoolean(ReadingTimerWorker.PREF_IS_TIMER_RUNNING, false);
    }
    
    /**
     * Obtiene el tiempo acumulado en el cronómetro
     */
    public static long getElapsedTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerWorker.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
                
        boolean isRunning = prefs.getBoolean(ReadingTimerWorker.PREF_IS_TIMER_RUNNING, false);
        long previousElapsed = prefs.getLong(ReadingTimerWorker.PREF_ELAPSED_TIME, 0);
        
        if (isRunning) {
            // Si está en ejecución, sumar el tiempo desde el último inicio
            long startTime = prefs.getLong(ReadingTimerWorker.PREF_START_TIME, 0);
            long currentTime = System.currentTimeMillis();
            return previousElapsed + (currentTime - startTime);
        } else {
            // Si está detenido, devolver el tiempo acumulado
            return previousElapsed;
        }
    }
    
    /**
     * Obtiene el ID del libro actual
     */
    public static int getCurrentBookId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerWorker.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        return prefs.getInt(ReadingTimerWorker.PREF_LIBRO_ID, -1);
    }
    
    /**
     * Obtiene el título del libro actual
     */
    public static String getCurrentBookTitle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerWorker.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        return prefs.getString(ReadingTimerWorker.PREF_LIBRO_TITULO, "");
    }
    
    /**
     * Formatea el tiempo en milisegundos a formato legible
     */
    private static String formatElapsedTime(long timeMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeMillis);
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}