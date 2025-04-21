package com.xabierland.librebook.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Clase utilitaria para calcular el tiempo estimado de lectura de un libro
 */
public class ReadingTimeUtils {

    // Constantes para las velocidades de lectura en páginas por hora
    public static final int READING_SPEED_SLOW = 20;      // 20 páginas por hora (~3 min/página)
    public static final int READING_SPEED_MEDIUM = 30;    // 30 páginas por hora (~2 min/página)
    public static final int READING_SPEED_FAST = 45;      // 45 páginas por hora (~1.33 min/página)
    public static final int READING_SPEED_VERY_FAST = 60; // 60 páginas por hora (1 min/página)

    // Clave para guardar la velocidad de lectura preferida por el usuario
    private static final String PREF_READING_SPEED = "reading_speed";

    /**
     * Calcula el tiempo estimado de lectura basado en la cantidad de páginas y velocidad
     * @param pages Número de páginas del libro
     * @param pagesPerHour Velocidad de lectura en páginas por hora
     * @return Tiempo estimado en minutos
     */
    public static int calculateReadingTime(int pages, int pagesPerHour) {
        if (pages <= 0 || pagesPerHour <= 0) {
            return 0;
        }
        
        // Fórmula: (páginas / pagesPerHour) * 60 minutos
        return (int) Math.ceil((double) pages / pagesPerHour * 60);
    }
    
    /**
     * Calcula el tiempo restante de lectura basado en la página actual
     * @param currentPage Página actual
     * @param totalPages Total de páginas
     * @param pagesPerHour Velocidad de lectura
     * @return Tiempo estimado restante en minutos
     */
    public static int calculateRemainingTime(int currentPage, int totalPages, int pagesPerHour) {
        if (currentPage >= totalPages || pagesPerHour <= 0) {
            return 0;
        }
        
        int pagesLeft = totalPages - currentPage;
        return calculateReadingTime(pagesLeft, pagesPerHour);
    }
    
    /**
     * Obtiene la velocidad de lectura guardada en preferencias
     * @param context Contexto de la aplicación
     * @return Velocidad de lectura en páginas por hora
     */
    public static int getSavedReadingSpeed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PREF_READING_SPEED, READING_SPEED_MEDIUM);
    }
    
    /**
     * Guarda la velocidad de lectura en preferencias
     * @param context Contexto de la aplicación
     * @param pagesPerHour Velocidad de lectura a guardar
     */
    public static void saveReadingSpeed(Context context, int pagesPerHour) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREF_READING_SPEED, pagesPerHour).apply();
    }
    
    /**
     * Formatea el tiempo en minutos a formato legible
     * @param minutes Tiempo en minutos
     * @return Texto formateado (ej: "2h 30m")
     */
    public static String formatReadingTime(int minutes) {
        if (minutes <= 0) {
            return "0m";
        }
        
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, remainingMinutes);
        } else {
            return String.format("%dm", remainingMinutes);
        }
    }
}