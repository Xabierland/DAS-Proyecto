package com.xabierland.librebook.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.BookDetailActivity;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.services.ReadingTimerReceiver;
import com.xabierland.librebook.utils.ImageLoader;
import com.xabierland.librebook.widget.WidgetImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Clase que implementa el widget de recomendación de libros
 */
public class BookRecommendationWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "BookWidgetProvider";
    private static final String ACTION_UPDATE_WIDGET = "com.xabierland.librebook.widget.UPDATE_WIDGET";
    // Intervalo de actualización: 15 segundos (en milisegundos)
    private static final long UPDATE_INTERVAL_MS = 15 * 1000; 

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Actualizar todos los widgets instalados
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        
        // Programar la próxima actualización
        scheduleNextUpdate(context);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        // Si recibimos la acción de actualizar el widget, actualizamos todos los widgets
        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            Log.d(TAG, "Recibida acción de actualización periódica");
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, BookRecommendationWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            
            // Actualizar todos los widgets
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Se llama cuando se añade el primer widget
        super.onEnabled(context);
        // Iniciar las actualizaciones periódicas
        scheduleNextUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Se llama cuando se elimina el último widget
        super.onDisabled(context);
        // Cancelar las actualizaciones periódicas
        cancelUpdates(context);
    }
    
    /**
     * Programa la próxima actualización automática del widget
     */
    private void scheduleNextUpdate(Context context) {
        // Cancelar cualquier alarma pendiente para evitar duplicados
        cancelUpdates(context);
        
        // Crear un Intent para nuestra acción personalizada
        Intent intent = new Intent(context, BookRecommendationWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        
        // Crear un PendingIntent que se usará para activar la actualización
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Obtener el AlarmManager y programar la próxima actualización
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Programar la alarma para que se dispare después del intervalo especificado
            alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + UPDATE_INTERVAL_MS,
                    pendingIntent
            );
            
            Log.d(TAG, "Próxima actualización programada en " + UPDATE_INTERVAL_MS/1000 + " segundos");
        }
    }
    
    /**
     * Cancela las actualizaciones programadas
     */
    private void cancelUpdates(Context context) {
        Intent intent = new Intent(context, BookRecommendationWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Actualizaciones periódicas canceladas");
        }
    }

    /**
     * Actualiza un widget individual
     */
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Obtener la Application desde el Context
        android.app.Application application = (android.app.Application) context.getApplicationContext();
        
        // Inicializar el repositorio de libros con la Application
        LibroRepository libroRepository = new LibroRepository(application);
        
        // Crear el RemoteViews para nuestro widget
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_book_recommendation);
        
        // Mostrar un mensaje de carga mientras obtenemos los datos
        views.setTextViewText(R.id.widget_book_title, context.getString(R.string.loading));
        views.setTextViewText(R.id.widget_book_author, "");
        appWidgetManager.updateAppWidget(appWidgetId, views);
        
        // Obtener todos los libros de la base de datos
        libroRepository.obtenerTodosLosLibros(libros -> {
            if (libros != null && !libros.isEmpty()) {
                // Seleccionar un libro aleatorio
                Libro libroRecomendado = getRandomBook(libros);
                
                // Actualizar la interfaz del widget con los datos del libro
                updateWidgetWithBook(context, appWidgetManager, appWidgetId, libroRecomendado);
                
                Log.d(TAG, "Widget actualizado con libro: " + libroRecomendado.getTitulo());
            } else {
                // Si no hay libros, mostrar un mensaje
                views.setTextViewText(R.id.widget_book_title, context.getString(R.string.no_books_available));
                views.setTextViewText(R.id.widget_book_author, "");
                appWidgetManager.updateAppWidget(appWidgetId, views);
                
                Log.d(TAG, "No hay libros disponibles para mostrar en el widget");
            }
        });
    }
    
    /**
     * Actualiza el widget con los datos de un libro
     */
    private void updateWidgetWithBook(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Libro libro) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_book_recommendation);
        
        // Actualizar los textos
        views.setTextViewText(R.id.widget_book_title, libro.getTitulo());
        views.setTextViewText(R.id.widget_book_author, libro.getAutor());
        
        // Crear un intent para abrir la actividad de detalle del libro cuando se toque el widget
        Intent intent = new Intent(context, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_LIBRO_ID, libro.getId());
        
        // Crear un PendingIntent para el clic en el widget
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                appWidgetId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Asignar el PendingIntent al widget
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
        
        // Comprobar si hay un cronómetro activo para este libro
        boolean isTimerRunning = ReadingTimerReceiver.isTimerRunning(context);
        int timerBookId = ReadingTimerReceiver.getCurrentBookId(context);
        
        if (isTimerRunning && timerBookId == libro.getId()) {
            // Mostrar indicador de cronómetro activo
            views.setViewVisibility(R.id.widget_timer_indicator, View.VISIBLE);
            
            // Calcular tiempo de lectura transcurrido
            long elapsedTime = ReadingTimerReceiver.getElapsedTime(context);
            String formattedTime = formatElapsedTime(elapsedTime);
            
            views.setTextViewText(R.id.widget_timer_text, formattedTime);
        } else {
            // Ocultar indicador de cronómetro
            views.setViewVisibility(R.id.widget_timer_indicator, View.GONE);
        }
        
        // Cargar la imagen de portada si existe
        if (libro.getPortadaUrl() != null && !libro.getPortadaUrl().isEmpty()) {
            // Usar nuestro cargador de imágenes especializado para widgets
            WidgetImageLoader.loadImageForWidget(
                context,
                libro.getPortadaUrl(),
                appWidgetId,
                appWidgetManager,
                views,
                R.id.widget_book_cover
            );
        } else {
            // Si no hay portada, mostrar un icono predeterminado
            views.setImageViewResource(R.id.widget_book_cover, R.mipmap.ic_launcher);
            // Actualizar el widget inmediatamente ya que no hay carga asíncrona
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
    /**
     * Formatea el tiempo en milisegundos a formato legible
     */
    private String formatElapsedTime(long timeMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeMillis);
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Selecciona un libro aleatorio de la lista
     */
    private Libro getRandomBook(List<Libro> allBooks) {
        // Crear una copia de la lista original para no modificarla
        List<Libro> shuffledList = new ArrayList<>(allBooks);
        
        // Mezclar la lista para obtener resultados aleatorios
        Collections.shuffle(shuffledList, new Random(System.currentTimeMillis()));
        
        // Devolver el primer libro de la lista mezclada
        return shuffledList.get(0);
    }
}