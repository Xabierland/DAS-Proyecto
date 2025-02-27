package com.xabierland.librebook.data.repositories;

import android.app.Application;

import com.xabierland.librebook.data.database.AppDatabase;
import com.xabierland.librebook.data.database.daos.UsuarioDao;
import com.xabierland.librebook.data.database.entities.Usuario;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsuarioRepository {
    
    private final UsuarioDao usuarioDao;
    private final ExecutorService executorService;
    
    public UsuarioRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        usuarioDao = db.usuarioDao();
        executorService = Executors.newFixedThreadPool(4);
    }
    
    // Método para registrar un nuevo usuario
    public void registrarUsuario(Usuario usuario, final DataCallback<Long> callback) {
        executorService.execute(() -> {
            // Verificar si el email ya existe
            int existentes = usuarioDao.verificarEmailExistente(usuario.getEmail());
            if (existentes > 0) {
                // El email ya existe
                if (callback != null) {
                    callback.onComplete(-1L); // -1 indica error
                }
                return;
            }
            
            // Registrar el usuario
            long id = usuarioDao.insertarUsuario(usuario);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }
    
    // Método para autenticar un usuario
    public void autenticarUsuario(String email, String password, final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            Integer userId = usuarioDao.autenticarUsuario(email, password);
            if (callback != null) {
                callback.onComplete(userId); // Será null si la autenticación falla
            }
        });
    }
    
    // Método para obtener un usuario por su ID
    public void obtenerUsuarioPorId(int id, final DataCallback<Usuario> callback) {
        executorService.execute(() -> {
            Usuario usuario = usuarioDao.obtenerUsuarioPorId(id);
            if (callback != null) {
                callback.onComplete(usuario);
            }
        });
    }
    
    // Método para obtener un usuario por su email
    public void obtenerUsuarioPorEmail(String email, final DataCallback<Usuario> callback) {
        executorService.execute(() -> {
            Usuario usuario = usuarioDao.obtenerUsuarioPorEmail(email);
            if (callback != null) {
                callback.onComplete(usuario);
            }
        });
    }
    
    // Método para actualizar los datos de un usuario
    public void actualizarUsuario(Usuario usuario, final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasActualizadas = usuarioDao.actualizarUsuario(usuario);
            if (callback != null) {
                callback.onComplete(filasActualizadas);
            }
        });
    }
    
    // Método para eliminar un usuario
    public void eliminarUsuario(Usuario usuario, final DataCallback<Integer> callback) {
        executorService.execute(() -> {
            int filasEliminadas = usuarioDao.eliminarUsuario(usuario);
            if (callback != null) {
                callback.onComplete(filasEliminadas);
            }
        });
    }
    
    // Método para obtener todos los usuarios (administrador)
    public void obtenerTodosLosUsuarios(final DataCallback<List<Usuario>> callback) {
        executorService.execute(() -> {
            List<Usuario> usuarios = usuarioDao.obtenerTodosLosUsuarios();
            if (callback != null) {
                callback.onComplete(usuarios);
            }
        });
    }
    
    // Interfaz para callbacks
    public interface DataCallback<T> {
        void onComplete(T result);
    }
}