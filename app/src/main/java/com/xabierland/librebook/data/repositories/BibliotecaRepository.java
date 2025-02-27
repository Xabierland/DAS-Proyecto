package com.xabierland.librebook.data.repositories;

import android.app.Application;

import com.xabierland.librebook.data.database.AppDatabase;
import com.xabierland.librebook.data.database.daos.UsuarioLibroDao;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.models.LibroConEstado;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BibliotecaRepository {
    
    private final UsuarioLibroDao usuarioLibroDao;
    private final ExecutorService executorService;
    
    public BibliotecaRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        usuarioLibroDao = db.usuarioLibroDao();
        executorService = Executors.newFixedThreadPool(4);
    }
    
    // Método para añadir un libro a la biblioteca del usuario
    public void agregarLibro(int usuarioId, int libroId, String estadoLectura, 
                           final DataCallback<Long> callback) {
        executorService.execute(() -> {
            UsuarioLibro usuarioLibro = new UsuarioLibro(usuarioId, libroId, estadoLectura);
            long id = usuarioLibroDao.insertarUsuarioLibro(usuarioLibro);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }
    
    // Método para obtener todos los libros de un usuario
    public void obtenerTodosLosLibrosDeUsuario(int usuarioId, 
                                             final DataCallback<List<LibroConEstado>> callback) {
        executorService.execute(() -> {
            List<LibroConEstado> libros = usuarioLibroDao.obtenerTodosLosLibrosDeUsuario(usuarioId);
            if (callback != null) {
                callback.onComplete(libros);
            }
        });
    }
    
    // Método para obtener libros por estado de lectura
    public void obtenerLibrosPorEstado(int usuarioId, String estadoLectura, 
                                     final DataCallback<List<LibroConEstado>> callback) {
        executorService.execute(() -> {
            List<LibroConEstado> libros = usuarioLibroDao.obtenerLibrosPorEstado(
                    usuarioId, estadoLectura);
            if (callback != null) {
                callback.onComplete(libros);
            }
        });
    }
    
    // Método para obtener libros por leer
    public void obtenerLibrosPorLeer(int usuarioId, 
                                   final DataCallback<List<LibroConEstado>> callback) {
        obtenerLibrosPorEstado(usuarioId, UsuarioLibro.ESTADO_POR_LEER, callback);
    }
    
    // Método para obtener libros en lectura
    public void obtenerLibrosLeyendo(int usuarioId, 
                                   final DataCallback<List<LibroConEstado>> callback) {
        obtenerLibrosPorEstado(usuarioId, UsuarioLibro.ESTADO_LEYENDO, callback);
    }
    
    // Método para obtener libros leídos
    public void obtenerLibrosLeidos(int usuarioId, 
                                  final DataCallback<List<LibroConEstado>> callback) {
        obtenerLibrosPorEstado(usuarioId, UsuarioLibro.ESTADO_LEIDO, callback);
    }
    
    // Método para obtener libros favoritos
    public void obtenerLibrosFavoritos(int usuarioId, 
                                     final DataCallback<List<LibroConEstado>> callback) {
        executorService.execute(() -> {
            List<LibroConEstado> libros = usuarioLibroDao.obtenerLibrosFavoritos(usuarioId);
            if (callback != null) {
                callback.onComplete(libros);
            }
        });
    }
    
    // Método para marcar/desmarcar un libro como favorito
    public void marcarComoFavorito(int usuarioId, int libroId, boolean esFavorito, 
                                 final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasActualizadas = usuarioLibroDao.marcarComoFavorito(
                    usuarioId, libroId, esFavorito);
            if (callback != null) {
                callback.onComplete(filasActualizadas);
            }
        });
    }
    
    // Método para cambiar el estado de lectura de un libro
    public void cambiarEstadoLectura(int usuarioId, int libroId, String estadoLectura, 
                                   final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasActualizadas = usuarioLibroDao.cambiarEstadoLectura(
                    usuarioId, libroId, estadoLectura);
            if (callback != null) {
                callback.onComplete(filasActualizadas);
            }
        });
    }
    
    // Método para actualizar la calificación de un libro
    public void actualizarCalificacion(int usuarioId, int libroId, float calificacion, 
                                     final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasActualizadas = usuarioLibroDao.actualizarCalificacion(
                    usuarioId, libroId, calificacion);
            if (callback != null) {
                callback.onComplete(filasActualizadas);
            }
        });
    }
    
    // Método para actualizar la página actual de un libro
    public void actualizarPaginaActual(int usuarioId, int libroId, int paginaActual, 
                                     final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasActualizadas = usuarioLibroDao.actualizarPaginaActual(
                    usuarioId, libroId, paginaActual);
            if (callback != null) {
                callback.onComplete(filasActualizadas);
            }
        });
    }
    
    // Método para guardar notas sobre un libro
    public void guardarNotas(int usuarioId, int libroId, String notas, 
                           final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasActualizadas = usuarioLibroDao.guardarNotas(
                    usuarioId, libroId, notas);
            if (callback != null) {
                callback.onComplete(filasActualizadas);
            }
        });
    }
    
    // Método para eliminar un libro de la biblioteca del usuario
    public void eliminarLibroDeBiblioteca(int usuarioId, int libroId, 
                                        final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            UsuarioLibro usuarioLibro = usuarioLibroDao.obtenerPorUsuarioYLibro(
                    usuarioId, libroId);
            int resultado = 0;
            if (usuarioLibro != null) {
                resultado = usuarioLibroDao.eliminarUsuarioLibro(usuarioLibro);
            }
            if (callback != null) {
                callback.onComplete(resultado);
            }
        });
    }
    
    // Métodos para obtener estadísticas
    
    // Método para contar libros por estado
    public void contarLibrosPorEstado(int usuarioId, String estadoLectura, 
                                    final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int cantidad = usuarioLibroDao.contarLibrosPorEstado(usuarioId, estadoLectura);
            if (callback != null) {
                callback.onComplete(cantidad);
            }
        });
    }
    
    // Método para obtener la calificación promedio
    public void obtenerCalificacionPromedio(int usuarioId, 
                                          final DataCallback<Float> callback) {
        executorService.execute(() -> {
            float promedio = usuarioLibroDao.obtenerCalificacionPromedio(usuarioId);
            if (callback != null) {
                callback.onComplete(promedio);
            }
        });
    }
    
    // Interfaz para callbacks
    public interface DataCallback<T> {
        void onComplete(T result);
    }
}