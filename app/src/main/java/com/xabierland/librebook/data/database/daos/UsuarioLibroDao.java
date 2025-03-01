package com.xabierland.librebook.data.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.RewriteQueriesToDropUnusedColumns;

import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.models.LibroConEstado;

import java.util.List;

@Dao
public interface UsuarioLibroDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertarUsuarioLibro(UsuarioLibro usuarioLibro);
    
    @Update
    int actualizarUsuarioLibro(UsuarioLibro usuarioLibro);
    
    @Delete
    int eliminarUsuarioLibro(UsuarioLibro usuarioLibro);
    
    @Query("SELECT * FROM usuarios_libros WHERE id = :id")
    UsuarioLibro obtenerPorId(int id);
    
    @Query("SELECT * FROM usuarios_libros WHERE usuario_id = :usuarioId AND libro_id = :libroId")
    UsuarioLibro obtenerPorUsuarioYLibro(int usuarioId, int libroId);
    
    // Obtener todos los libros de un usuario
    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas " +
           "FROM libros l " +
           "INNER JOIN usuarios_libros ul ON l.id = ul.libro_id " +
           "WHERE ul.usuario_id = :usuarioId")
    List<LibroConEstado> obtenerTodosLosLibrosDeUsuario(int usuarioId);
    
    // Obtener un libro específico de un usuario
    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas " +
           "FROM libros l " +
           "INNER JOIN usuarios_libros ul ON l.id = ul.libro_id " +
           "WHERE ul.usuario_id = :usuarioId AND l.id = :libroId")
    LibroConEstado obtenerLibroDeUsuario(int usuarioId, int libroId);
    
    // Obtener libros por estado de lectura (por leer, leyendo, leídos)
    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas " +
           "FROM libros l " +
           "INNER JOIN usuarios_libros ul ON l.id = ul.libro_id " +
           "WHERE ul.usuario_id = :usuarioId AND ul.estado_lectura = :estadoLectura")
    List<LibroConEstado> obtenerLibrosPorEstado(int usuarioId, String estadoLectura);
    
    // Obtener libros favoritos
    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas " +
           "FROM libros l " +
           "INNER JOIN usuarios_libros ul ON l.id = ul.libro_id " +
           "WHERE ul.usuario_id = :usuarioId AND ul.es_favorito = 1")
    List<LibroConEstado> obtenerLibrosFavoritos(int usuarioId);
    
    // Marcar/desmarcar libro como favorito
    @Query("UPDATE usuarios_libros SET es_favorito = :esFavorito " +
           "WHERE usuario_id = :usuarioId AND libro_id = :libroId")
    int marcarComoFavorito(int usuarioId, int libroId, boolean esFavorito);
    
    // Cambiar estado de lectura
    @Query("UPDATE usuarios_libros SET estado_lectura = :estadoLectura " +
           "WHERE usuario_id = :usuarioId AND libro_id = :libroId")
    int cambiarEstadoLectura(int usuarioId, int libroId, String estadoLectura);
    
    // Actualizar calificación
    @Query("UPDATE usuarios_libros SET calificacion = :calificacion " +
           "WHERE usuario_id = :usuarioId AND libro_id = :libroId")
    int actualizarCalificacion(int usuarioId, int libroId, float calificacion);
    
    // Actualizar página actual
    @Query("UPDATE usuarios_libros SET pagina_actual = :paginaActual " +
           "WHERE usuario_id = :usuarioId AND libro_id = :libroId")
    int actualizarPaginaActual(int usuarioId, int libroId, int paginaActual);
    
    // Guardar notas
    @Query("UPDATE usuarios_libros SET notas = :notas " +
           "WHERE usuario_id = :usuarioId AND libro_id = :libroId")
    int guardarNotas(int usuarioId, int libroId, String notas);
    
    // Obtener estadísticas de lectura
    @Query("SELECT COUNT(*) FROM usuarios_libros " +
           "WHERE usuario_id = :usuarioId AND estado_lectura = :estadoLectura")
    int contarLibrosPorEstado(int usuarioId, String estadoLectura);
    
    @Query("SELECT AVG(calificacion) FROM usuarios_libros " +
           "WHERE usuario_id = :usuarioId AND calificacion IS NOT NULL")
    float obtenerCalificacionPromedio(int usuarioId);
}