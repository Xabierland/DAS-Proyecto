package com.xabierland.librebook.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.data.repositories.BibliotecaRepository;
import com.xabierland.librebook.data.repositories.LibroRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Content Provider para compartir libros de la aplicación LibreBook
 */
public class LibreBooksProvider extends ContentProvider {

    private static final String TAG = "LibreBooksProvider";
    
    // Autoridad del content provider
    public static final String AUTHORITY = "com.xabierland.librebook.provider";
    
    // Rutas base para diferentes tipos de contenido
    public static final String PATH_BOOKS = "books";
    public static final String PATH_BOOK_FILES = "book_files";
    
    // Códigos para el UriMatcher
    private static final int CODE_BOOK_DIR = 1;
    private static final int CODE_BOOK_ITEM = 2;
    private static final int CODE_BOOK_FILE = 3;
    
    // UriMatcher para determinar qué tipo de Uri está solicitando
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    
    // Nombre de las columnas para las consultas
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "titulo";
    public static final String COLUMN_AUTHOR = "autor";
    public static final String COLUMN_DESCRIPTION = "descripcion";
    public static final String COLUMN_GENRE = "genero";
    public static final String COLUMN_PAGES = "num_paginas";
    public static final String COLUMN_READING_STATUS = "estado_lectura";
    public static final String COLUMN_RATING = "calificacion";
    public static final String COLUMN_COVER_URL = "portada_url";
    
    // Repositorios
    private LibroRepository libroRepository;
    private BibliotecaRepository bibliotecaRepository;
    
    // Configurar UriMatcher
    static {
        URI_MATCHER.addURI(AUTHORITY, PATH_BOOKS, CODE_BOOK_DIR);
        URI_MATCHER.addURI(AUTHORITY, PATH_BOOKS + "/#", CODE_BOOK_ITEM);
        URI_MATCHER.addURI(AUTHORITY, PATH_BOOK_FILES + "/#", CODE_BOOK_FILE);
    }
    
    @Override
    public boolean onCreate() {
        libroRepository = new LibroRepository(getContext().getApplicationContext());
        bibliotecaRepository = new BibliotecaRepository(getContext().getApplicationContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        
        int match = URI_MATCHER.match(uri);
        
        switch (match) {
            case CODE_BOOK_DIR:
                // Devolver todos los libros, posiblemente filtrados (no implementado en esta versión básica)
                return queryAllBooks(projection, selection, selectionArgs, sortOrder);
                
            case CODE_BOOK_ITEM:
                // Devolver un libro específico
                int bookId = Integer.parseInt(uri.getLastPathSegment());
                return queryBook(bookId, projection);
                
            default:
                throw new IllegalArgumentException("Uri desconocida: " + uri);
        }
    }
    
    /**
     * Consulta todos los libros
     */
    private Cursor queryAllBooks(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Crear un cursor de matriz que simulará los resultados
        MatrixCursor cursor = createBooksCursor(projection);
        
        // Lista para almacenar los libros que obtendremos de manera asíncrona
        final List<Libro> libros = new ArrayList<>();
        
        // Utilizar un CountDownLatch para hacer que la operación asíncrona sea síncrona
        final CountDownLatch latch = new CountDownLatch(1);
        
        // Obtener todos los libros de forma asíncrona
        libroRepository.obtenerTodosLosLibros(result -> {
            if (result != null) {
                libros.addAll(result);
            }
            latch.countDown();
        });
        
        try {
            // Esperar a que se complete la operación asíncrona
            latch.await();
            
            // Llenar el cursor con los datos de los libros
            for (Libro libro : libros) {
                cursor.addRow(getBookRowValues(libro, null, projection));
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Error al esperar los resultados de la consulta", e);
        }
        
        return cursor;
    }
    
    /**
     * Consulta un libro específico por su ID
     */
    private Cursor queryBook(int bookId, String[] projection) {
        // Crear un cursor de matriz
        MatrixCursor cursor = createBooksCursor(projection);
        
        // Variables para almacenar los resultados
        final Libro[] libro = new Libro[1];
        
        // Para almacenar la información de estado si el usuario ha guardado este libro
        final LibroConEstado[] libroConEstado = new LibroConEstado[1];
        
        // CountDownLatch para sincronización
        final CountDownLatch latch = new CountDownLatch(1);
        
        // Obtener el libro específico
        libroRepository.obtenerLibroPorId(bookId, result -> {
            libro[0] = result;
            
            // Intentar obtener información de estado si el usuario está logueado
            int usuarioId = getUserId();
            if (usuarioId != -1 && result != null) {
                bibliotecaRepository.obtenerLibroDeBiblioteca(usuarioId, result.getId(), estadoResult -> {
                    libroConEstado[0] = estadoResult;
                    latch.countDown();
                });
            } else {
                latch.countDown();
            }
        });
        
        try {
            // Esperar a que se complete la operación asíncrona
            latch.await();
            
            // Si se encontró el libro, añadirlo al cursor
            if (libro[0] != null) {
                cursor.addRow(getBookRowValues(libro[0], libroConEstado[0], projection));
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Error al esperar los resultados de la consulta del libro", e);
        }
        
        return cursor;
    }
    
    /**
     * Crea un cursor vacío con las columnas adecuadas
     */
    private MatrixCursor createBooksCursor(String[] projection) {
        // Si no se especifican columnas, usar todas
        if (projection == null) {
            projection = new String[] {
                COLUMN_ID, COLUMN_TITLE, COLUMN_AUTHOR, COLUMN_DESCRIPTION,
                COLUMN_GENRE, COLUMN_PAGES, COLUMN_READING_STATUS, COLUMN_RATING,
                COLUMN_COVER_URL
            };
        }
        
        return new MatrixCursor(projection);
    }
    
    /**
     * Obtiene los valores para una fila del cursor a partir de un Libro
     */
    private Object[] getBookRowValues(Libro libro, LibroConEstado estado, String[] projection) {
        Object[] values = new Object[projection.length];
        
        for (int i = 0; i < projection.length; i++) {
            switch (projection[i]) {
                case COLUMN_ID:
                    values[i] = libro.getId();
                    break;
                case COLUMN_TITLE:
                    values[i] = libro.getTitulo();
                    break;
                case COLUMN_AUTHOR:
                    values[i] = libro.getAutor();
                    break;
                case COLUMN_DESCRIPTION:
                    values[i] = libro.getDescripcion();
                    break;
                case COLUMN_GENRE:
                    values[i] = libro.getGenero();
                    break;
                case COLUMN_PAGES:
                    values[i] = libro.getNumPaginas();
                    break;
                case COLUMN_COVER_URL:
                    values[i] = libro.getPortadaUrl();
                    break;
                case COLUMN_READING_STATUS:
                    values[i] = estado != null ? estado.getEstadoLectura() : null;
                    break;
                case COLUMN_RATING:
                    values[i] = estado != null ? estado.getCalificacion() : null;
                    break;
                default:
                    values[i] = null;
                    break;
            }
        }
        
        return values;
    }
    
    /**
     * Obtiene el ID del usuario actual si está logueado
     */
    private int getUserId() {
        android.content.SharedPreferences sharedPreferences = 
                getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        return sharedPreferences.getInt("userId", -1);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = URI_MATCHER.match(uri);
        
        switch (match) {
            case CODE_BOOK_DIR:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + PATH_BOOKS;
            case CODE_BOOK_ITEM:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + PATH_BOOKS;
            case CODE_BOOK_FILE:
                return "text/plain";
            default:
                throw new IllegalArgumentException("Uri desconocida: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // Este provider es de solo lectura
        throw new UnsupportedOperationException("No se admiten inserciones en este provider");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Este provider es de solo lectura
        throw new UnsupportedOperationException("No se admiten eliminaciones en este provider");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Este provider es de solo lectura
        throw new UnsupportedOperationException("No se admiten actualizaciones en este provider");
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        int match = URI_MATCHER.match(uri);
        
        if (match == CODE_BOOK_FILE) {
            int bookId = Integer.parseInt(uri.getLastPathSegment());
            
            // Crear un archivo temporal con la información del libro
            File bookFile = generateBookFile(bookId);
            
            if (bookFile != null && bookFile.exists()) {
                return ParcelFileDescriptor.open(bookFile, ParcelFileDescriptor.MODE_READ_ONLY);
            } else {
                throw new FileNotFoundException("No se pudo generar el archivo para el libro con ID: " + bookId);
            }
        }
        
        return super.openFile(uri, mode);
    }
    
    /**
     * Genera un archivo con información detallada del libro
     */
    private File generateBookFile(int bookId) {
        final File[] resultFile = new File[1];
        final CountDownLatch latch = new CountDownLatch(1);
        
        libroRepository.obtenerLibroPorId(bookId, libro -> {
            if (libro != null) {
                try {
                    // Crear archivo temporal
                    File file = File.createTempFile("libro_" + libro.getId() + "_", ".txt", 
                            getContext().getCacheDir());
                    
                    // Escribir la información del libro
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
                        
                        // Comprobar si el usuario tiene este libro en su biblioteca
                        int usuarioId = getUserId();
                        if (usuarioId != -1) {
                            final LibroConEstado[] estado = new LibroConEstado[1];
                            final CountDownLatch estadoLatch = new CountDownLatch(1);
                            
                            bibliotecaRepository.obtenerLibroDeBiblioteca(usuarioId, libro.getId(), result -> {
                                estado[0] = result;
                                estadoLatch.countDown();
                            });
                            
                            try {
                                estadoLatch.await();
                                if (estado[0] != null) {
                                    writer.println("MI OPINIÓN");
                                    writer.println("==========");
                                    
                                    // Estado de lectura
                                    String estadoLectura = "Por leer";
                                    switch (estado[0].getEstadoLectura()) {
                                        case "leyendo":
                                            estadoLectura = "Leyendo";
                                            break;
                                        case "leido":
                                            estadoLectura = "Leído";
                                            break;
                                    }
                                    writer.println("Estado: " + estadoLectura);
                                    
                                    // Mostrar calificación si existe
                                    if (estado[0].getCalificacion() != null) {
                                        writer.println(String.format(Locale.getDefault(), 
                                                "Calificación: %.1f/10", estado[0].getCalificacion()));
                                    }
                                    
                                    // Mostrar progreso si está leyendo
                                    if ("leyendo".equals(estado[0].getEstadoLectura()) && 
                                            estado[0].getPaginaActual() != null) {
                                        writer.println(String.format(Locale.getDefault(),
                                                "Progreso: %d de %d páginas (%d%%)", 
                                                estado[0].getPaginaActual(), 
                                                libro.getNumPaginas(),
                                                estado[0].getProgresoLectura()));
                                    }
                                    
                                    // Mostrar notas/reseña si existen
                                    if (estado[0].getNotas() != null && !estado[0].getNotas().isEmpty()) {
                                        writer.println();
                                        writer.println("Mi reseña:");
                                        writer.println(estado[0].getNotas());
                                    }
                                }
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Error al esperar los datos de estado del libro", e);
                            }
                        }
                        
                        // Añadir pie de página con "Compartido desde LibreBook"
                        writer.println();
                        writer.println("-----------------------------------");
                        writer.println("Compartido desde la app LibreBook");
                        writer.println("Descárgala ahora y gestiona tus lecturas");
                    }
                    
                    resultFile[0] = file;
                } catch (IOException e) {
                    Log.e(TAG, "Error al generar archivo del libro", e);
                }
            }
            latch.countDown();
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Error al esperar la generación del archivo", e);
        }
        
        return resultFile[0];
    }
}