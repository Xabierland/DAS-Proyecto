package com.xabierland.librebook.data.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.xabierland.librebook.data.database.entities.Usuario;

import java.util.List;

@Dao
public interface UsuarioDao {
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertarUsuario(Usuario usuario);
    
    @Update
    int actualizarUsuario(Usuario usuario);
    
    @Delete
    int eliminarUsuario(Usuario usuario);
    
    @Query("SELECT * FROM usuarios WHERE id = :id")
    Usuario obtenerUsuarioPorId(int id);
    
    @Query("SELECT * FROM usuarios WHERE email = :email")
    Usuario obtenerUsuarioPorEmail(String email);
    
    @Query("SELECT * FROM usuarios")
    List<Usuario> obtenerTodosLosUsuarios();
    
    @Query("SELECT COUNT(*) FROM usuarios WHERE email = :email")
    int verificarEmailExistente(String email);
    
    @Query("SELECT id FROM usuarios WHERE email = :email AND password = :password")
    Integer autenticarUsuario(String email, String password);
}