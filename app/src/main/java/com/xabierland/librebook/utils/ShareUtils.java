package com.xabierland.librebook.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.providers.LibreBooksProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Clase utilitaria para compartir información de libros
 */
public class ShareUtils {

    private static final String TAG = "ShareUtils";
    
    /**
     * Comparte la información básica de un libro como texto
     * @param context Contexto desde donde se llama
     * @param libro Libro a compartir
     */
    public static void shareBookAsText(Context context, Libro libro) {
        if (libro == null) {
            Toast.makeText(context, R.string.error_sharing_book, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Construir el mensaje a compartir
        StringBuilder message = new StringBuilder();
        message.append("📚 ").append(libro.getTitulo()).append("\n");
        message.append("✍️ ").append(libro.getAutor()).append("\n");
        
        if (libro.getGenero() != null && !libro.getGenero().isEmpty()) {
            message.append("🏷️ ").append(libro.getGenero()).append("\n");
        }
        
        message.append("📄 ").append(libro.getNumPaginas()).append(" ").append(context.getString(R.string.pages)).append("\n\n");
        
        if (libro.getDescripcion() != null && !libro.getDescripcion().isEmpty()) {
            // Incluir la descripción completa sin limitación
            message.append(libro.getDescripcion()).append("\n\n");
        }
        
        message.append("💬 ").append(context.getString(R.string.shared_from_librebook));
        
        // Crear el intent para compartir
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, libro.getTitulo());
        shareIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
        
        // Mostrar el selector de aplicaciones
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_book_via)));
    }
    
    /**
     * Comparte la información de un libro como archivo de texto
     * @param context Contexto desde donde se llama
     * @param libro Libro a compartir
     * @param estado Información de estado si existe (puede ser null)
     */
    public static void shareBookAsFile(Context context, Libro libro, LibroConEstado estado) {
        if (libro == null) {
            Toast.makeText(context, R.string.error_sharing_book, Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Crear archivo temporal
            File bookFile = createBookFile(context, libro, estado);
            
            if (bookFile == null) {
                Toast.makeText(context, R.string.error_creating_file, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Log para depuración
            Log.d(TAG, "Ruta del archivo: " + bookFile.getAbsolutePath());
            
            // Obtener la URI del archivo usando FileProvider para compatibilidad
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    "com.xabierland.librebook.fileprovider", // Usar el authority correcto definido en AndroidManifest
                    bookFile);
            
            // Crear intent para compartir
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, libro.getTitulo());
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            
            // Añadir flag para dar permiso temporal al receptor
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Mostrar el selector de aplicaciones
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_book_via)));
            
        } catch (Exception e) {
            Log.e(TAG, "Error al compartir libro como archivo", e);
            Toast.makeText(context, R.string.error_sharing_book, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Comparte un libro usando el ContentProvider
     * @param context Contexto desde donde se llama
     * @param libroId ID del libro a compartir
     */
    public static void shareBookViaContentProvider(Context context, int libroId) {
        // URI para el archivo a través del ContentProvider
        Uri bookFileUri = Uri.parse("content://" + LibreBooksProvider.AUTHORITY + "/" + 
                LibreBooksProvider.PATH_BOOK_FILES + "/" + libroId);
        
        try {
            // Crear intent para compartir
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.book_info));
            shareIntent.putExtra(Intent.EXTRA_STREAM, bookFileUri);
            
            // Añadir flag para dar permiso temporal al receptor
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Mostrar el selector de aplicaciones
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_book_via)));
            
        } catch (Exception e) {
            Log.e(TAG, "Error al compartir libro a través del ContentProvider", e);
            Toast.makeText(context, R.string.error_sharing_book, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Crea un archivo temporal con la información del libro
     */
    private static File createBookFile(Context context, Libro libro, LibroConEstado estado) {
        try {
            // Generar un nombre de archivo basado en el título del libro
            String safeFileName = sanitizeFileName(libro.getTitulo()) + "_" + libro.getId() + ".txt";
            
            // Crear archivo en el directorio de caché (que ahora estará correctamente configurado)
            File file = new File(context.getCacheDir(), safeFileName);
            
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
                writer.println("LIBRO COMPARTIDO DESDE LIBREBOOK");
                writer.println("===============================");
                writer.println();
                writer.println("Título: " + libro.getTitulo());
                writer.println("Autor: " + libro.getAutor());
                
                if (libro.getGenero() != null && !libro.getGenero().isEmpty()) {
                    writer.println("Género: " + libro.getGenero());
                }
                
                writer.println("Páginas: " + libro.getNumPaginas());
                
                if (libro.getEditorial() != null && !libro.getEditorial().isEmpty()) {
                    writer.println("Editorial: " + libro.getEditorial());
                }
                
                if (libro.getAnioPublicacion() > 0) {
                    writer.println("Año de publicación: " + libro.getAnioPublicacion());
                }
                
                if (libro.getIsbn() != null && !libro.getIsbn().isEmpty()) {
                    writer.println("ISBN: " + libro.getIsbn());
                }
                
                writer.println();
                
                if (libro.getDescripcion() != null && !libro.getDescripcion().isEmpty()) {
                    writer.println("DESCRIPCIÓN");
                    writer.println("===========");
                    writer.println(libro.getDescripcion());
                    writer.println();
                }
                
                // Si hay información de estado del usuario, incluirla
                if (estado != null) {
                    writer.println("MI OPINIÓN");
                    writer.println("==========");
                    
                    // Estado de lectura
                    String estadoLectura = "Por leer";
                    switch (estado.getEstadoLectura()) {
                        case "leyendo":
                            estadoLectura = "Leyendo";
                            break;
                        case "leido":
                            estadoLectura = "Leído";
                            break;
                    }
                    writer.println("Estado: " + estadoLectura);
                    
                    // Mostrar calificación si existe
                    if (estado.getCalificacion() != null) {
                        writer.println(String.format(Locale.getDefault(), 
                                "Calificación: %.1f/10", estado.getCalificacion()));
                    }
                    
                    // Mostrar progreso si está leyendo
                    if ("leyendo".equals(estado.getEstadoLectura()) && 
                            estado.getPaginaActual() != null) {
                        writer.println(String.format(Locale.getDefault(),
                                "Progreso: %d de %d páginas (%d%%)", 
                                estado.getPaginaActual(), 
                                libro.getNumPaginas(),
                                estado.getProgresoLectura()));
                    }
                    
                    // Mostrar notas/reseña si existen
                    if (estado.getNotas() != null && !estado.getNotas().isEmpty()) {
                        writer.println();
                        writer.println("Mi reseña:");
                        writer.println(estado.getNotas());
                    }
                }
                
                // Añadir pie de página con "Compartido desde LibreBook"
                writer.println();
                writer.println("-----------------------------------");
                writer.println("Compartido desde la app LibreBook");
                writer.println("Descárgala ahora y gestiona tus lecturas");
            }
            
            // Verificar que el archivo se creó correctamente
            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "Archivo creado con éxito: " + file.getAbsolutePath() + ", tamaño: " + file.length());
                return file;
            } else {
                Log.e(TAG, "El archivo no existe o está vacío después de crearlo");
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al crear archivo de libro", e);
            return null;
        }
    }
    
    /**
     * Sanitiza un nombre de archivo eliminando caracteres no permitidos
     * @param fileName Nombre de archivo original
     * @return Nombre de archivo sanitizado
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "libro";
        }
        
        // Limitar la longitud a 50 caracteres para evitar nombres demasiado largos
        if (fileName.length() > 50) {
            fileName = fileName.substring(0, 50);
        }
        
        // Eliminar acentos y caracteres especiales
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
        
        // Reemplazar caracteres no permitidos en nombres de archivo
        return normalized.replaceAll("[^a-zA-Z0-9\\-_\\. ]", "_")
                .trim()
                .replaceAll("\\s+", "_");
    }
}