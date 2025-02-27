package com.xabierland.librebook.data.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.xabierland.librebook.data.database.entities.Libro;

import java.util.List;

@Dao
public interface LibroDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertarLibro(Libro libro);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertarLibros(List<Libro> libros);
    
    @Update
    int actualizarLibro(Libro libro);
    
    @Delete
    int eliminarLibro(Libro libro);
    
    @Query("SELECT * FROM libros WHERE id = :id")
    Libro obtenerLibroPorId(int id);
    
    @Query("SELECT * FROM libros WHERE isbn = :isbn")
    Libro obtenerLibroPorISBN(String isbn);
    
    @Query("SELECT * FROM libros")
    List<Libro> obtenerTodosLosLibros();
    
    @Query("SELECT * FROM libros WHERE titulo LIKE '%' || :busqueda || '%' OR autor LIKE '%' || :busqueda || '%'")
    List<Libro> buscarLibros(String busqueda);
    
    @Query("SELECT * FROM libros WHERE genero = :genero")
    List<Libro> obtenerLibrosPorGenero(String genero);
    
    @Query("SELECT * FROM libros WHERE autor = :autor")
    List<Libro> obtenerLibrosPorAutor(String autor);
    
    @Query("SELECT DISTINCT genero FROM libros ORDER BY genero")
    List<String> obtenerGeneros();
    
    @Query("SELECT DISTINCT autor FROM libros ORDER BY autor")
    List<String> obtenerAutores();
}