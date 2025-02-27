package com.xabierland.librebook.data.repositories;

import android.app.Application;

import com.xabierland.librebook.data.database.AppDatabase;
import com.xabierland.librebook.data.database.daos.LibroDao;
import com.xabierland.librebook.data.database.entities.Libro;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibroRepository {
    
    private final LibroDao libroDao;
    private final ExecutorService executorService;
    
    public LibroRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        libroDao = db.libroDao();
        executorService = Executors.newFixedThreadPool(4);
    }
    
    // Método para añadir un nuevo libro
    public void insertarLibro(Libro libro, final DataCallback<Long> callback) {
        executorService.execute(() -> {
            long id = libroDao.insertarLibro(libro);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }
    
    // Método para añadir múltiples libros
    public void insertarLibros(List<Libro> libros, final DataCallback<List<Long>> callback) {
        executorService.execute(() -> {
            List<Long> ids = libroDao.insertarLibros(libros);
            if (callback != null) {
                callback.onComplete(ids);
            }
        });
    }
    
    // Método para actualizar un libro
    public void actualizarLibro(Libro libro, final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasActualizadas = libroDao.actualizarLibro(libro);
            if (callback != null) {
                callback.onComplete(filasActualizadas);
            }
        });
    }
    
    // Método para eliminar un libro
    public void eliminarLibro(Libro libro, final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasEliminadas = libroDao.eliminarLibro(libro);
            if (callback != null) {
                callback.onComplete(filasEliminadas);
            }
        });
    }
    
    // Método para obtener un libro por su ID
    public void obtenerLibroPorId(int id, final DataCallback<Libro> callback) {
        executorService.execute(() -> {
            Libro libro = libroDao.obtenerLibroPorId(id);
            if (callback != null) {
                callback.onComplete(libro);
            }
        });
    }
    
    // Método para obtener un libro por su ISBN
    public void obtenerLibroPorISBN(String isbn, final DataCallback<Libro> callback) {
        executorService.execute(() -> {
            Libro libro = libroDao.obtenerLibroPorISBN(isbn);
            if (callback != null) {
                callback.onComplete(libro);
            }
        });
    }
    
    // Método para obtener todos los libros
    public void obtenerTodosLosLibros(final DataCallback<List<Libro>> callback) {
        executorService.execute(() -> {
            List<Libro> libros = libroDao.obtenerTodosLosLibros();
            if (callback != null) {
                callback.onComplete(libros);
            }
        });
    }
    
    // Método para buscar libros por título o autor
    public void buscarLibros(String busqueda, final DataCallback<List<Libro>> callback) {
        executorService.execute(() -> {
            List<Libro> libros = libroDao.buscarLibros(busqueda);
            if (callback != null) {
                callback.onComplete(libros);
            }
        });
    }
    
    // Método para obtener libros por género
    public void obtenerLibrosPorGenero(String genero, final DataCallback<List<Libro>> callback) {
        executorService.execute(() -> {
            List<Libro> libros = libroDao.obtenerLibrosPorGenero(genero);
            if (callback != null) {
                callback.onComplete(libros);
            }
        });
    }
    
    // Método para obtener libros por autor
    public void obtenerLibrosPorAutor(String autor, final DataCallback<List<Libro>> callback) {
        executorService.execute(() -> {
            List<Libro> libros = libroDao.obtenerLibrosPorAutor(autor);
            if (callback != null) {
                callback.onComplete(libros);
            }
        });
    }
    
    // Método para obtener todos los géneros disponibles
    public void obtenerGeneros(final DataCallback<List<String>> callback) {
        executorService.execute(() -> {
            List<String> generos = libroDao.obtenerGeneros();
            if (callback != null) {
                callback.onComplete(generos);
            }
        });
    }
    
    // Método para obtener todos los autores disponibles
    public void obtenerAutores(final DataCallback<List<String>> callback) {
        executorService.execute(() -> {
            List<String> autores = libroDao.obtenerAutores();
            if (callback != null) {
                callback.onComplete(autores);
            }
        });
    }
    
    // Interfaz para callbacks
    public interface DataCallback<T> {
        void onComplete(T result);
    }
}