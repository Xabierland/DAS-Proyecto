package com.xabierland.librebook.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

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
    public static final String ACTION_TIMER_TICK = "com.xabierland.librebook.action.TIMER_TICK";
    
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
                ReadingTimerService.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        switch (action) {
            case ACTION_START_TIMER:
                // Obtener datos del libro
                int libroId = intent.getIntExtra(ReadingTimerService.PREF_LIBRO_ID, -1);
                String libroTitulo = intent.getStringExtra(ReadingTimerService.PREF_LIBRO_TITULO);
                
                if (libroId == -1 || libroTitulo == null) {
                    Log.e(TAG, "Error: ID del libro o título no proporcionados");
                    return;
                }
                
                // Iniciar el servicio en primer plano
                Intent serviceIntent = new Intent(context, ReadingTimerService.class);
                serviceIntent.putExtra(ReadingTimerService.PREF_LIBRO_ID, libroId);
                serviceIntent.putExtra(ReadingTimerService.PREF_LIBRO_TITULO, libroTitulo);
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                
                // Asegurar que el estado es "en ejecución"
                editor.putBoolean(ReadingTimerService.PREF_IS_TIMER_RUNNING, true);
                editor.apply();
                
                Log.d(TAG, "Servicio de cronómetro iniciado para libro: " + libroTitulo);
                break;
                
            case ACTION_STOP_TIMER:
                // Pausar el timer
                boolean isRunning = prefs.getBoolean(ReadingTimerService.PREF_IS_TIMER_RUNNING, false);
                
                if (isRunning) {
                    // Detener el servicio
                    context.stopService(new Intent(context, ReadingTimerService.class));
                    
                    // Marcar como pausado
                    editor.putBoolean(ReadingTimerService.PREF_IS_TIMER_RUNNING, false);
                    editor.apply();
                    
                    // Cancelar la notificación
                    NotificationManager notificationManager = 
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_ID);
                    
                    Log.d(TAG, "Timer detenido. Tiempo acumulado: " + 
                            formatElapsedTime(getElapsedTime(context)));
                }
                break;
                
            case ACTION_RESET_TIMER:
                // Reiniciar el timer
                // Detener el servicio si está en ejecución
                context.stopService(new Intent(context, ReadingTimerService.class));
                
                // Resetear todos los valores
                editor.putBoolean(ReadingTimerService.PREF_IS_TIMER_RUNNING, false);
                editor.putLong(ReadingTimerService.PREF_ELAPSED_TIME, 0);
                editor.putLong(ReadingTimerService.PREF_START_TIME, 0);
                editor.apply();
                
                // Cancelar la notificación
                NotificationManager notificationManager = 
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
                
                Log.d(TAG, "Timer reiniciado");
                break;
                
            case ACTION_TIMER_TICK:
                // Este caso se usa para notificar a las actividades que se actualicen
                // No necesitamos hacer nada aquí, ya que las actividades escucharán este broadcast
                break;
        }
    }
    
    /**
     * Método de utilidad para comprobar si el cronómetro está en ejecución
     */
    public static boolean isTimerRunning(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerService.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        return prefs.getBoolean(ReadingTimerService.PREF_IS_TIMER_RUNNING, false);
    }
    
    /**
     * Obtiene el tiempo acumulado en el cronómetro
     */
    public static long getElapsedTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerService.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
                
        boolean isRunning = prefs.getBoolean(ReadingTimerService.PREF_IS_TIMER_RUNNING, false);
        long previousElapsed = prefs.getLong(ReadingTimerService.PREF_ELAPSED_TIME, 0);
        
        if (isRunning) {
            // Si está en ejecución, sumar el tiempo desde el último inicio
            long startTime = prefs.getLong(ReadingTimerService.PREF_START_TIME, 0);
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
                ReadingTimerService.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        return prefs.getInt(ReadingTimerService.PREF_LIBRO_ID, -1);
    }
    
    /**
     * Obtiene el título del libro actual
     */
    public static String getCurrentBookTitle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReadingTimerService.PREF_FILE_READING_TIMER, Context.MODE_PRIVATE);
        return prefs.getString(ReadingTimerService.PREF_LIBRO_TITULO, "");
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