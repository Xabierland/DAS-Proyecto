package com.xabierland.librebook.data.repositories;

import android.app.Application;
import android.util.Log;

import com.xabierland.librebook.api.ApiClient;
import com.xabierland.librebook.api.ApiParsers;
import com.xabierland.librebook.data.database.entities.Usuario;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsuarioRepository {
    
    private static final String TAG = "UsuarioRepository";
    private final ExecutorService executorService;
    
    public UsuarioRepository(Application application) {
        executorService = Executors.newFixedThreadPool(4);
    }
    
    // Método para registrar un nuevo usuario
    public void registrarUsuario(Usuario usuario, final DataCallback<Long> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("nombre", usuario.getNombre());
            jsonObject.put("email", usuario.getEmail());
            jsonObject.put("password", usuario.getPassword());
            if (usuario.getFotoPerfil() != null) {
                jsonObject.put("fotoPerfil", usuario.getFotoPerfil());
            }
            
            ApiClient.post("usuarios", jsonObject, ApiParsers.idResponseParser(), new ApiClient.ApiCallback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    if (callback != null) {
                        callback.onComplete(result);
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error al registrar usuario: " + errorMessage);
                    if (callback != null) {
                        // Si el error es por email duplicado, devolvemos -1
                        if (errorMessage.contains("409") || errorMessage.contains("ya está en uso")) {
                            callback.onComplete(-1L);
                        } else {
                            callback.onComplete(0L);
                        }
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para registrar usuario", e);
            if (callback != null) {
                callback.onComplete(0L);
            }
        }
    }
    
    // Método para autenticar un usuario
    public void autenticarUsuario(String email, String password, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("email", email);
            jsonObject.put("password", password);
            
            ApiClient.post("usuarios/autenticar", jsonObject, response -> {
                JSONObject json = new JSONObject(response);
                return json.getInt("id");
            }, new ApiClient.ApiCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    if (callback != null) {
                        callback.onComplete(result);
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error al autenticar usuario: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(null);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para autenticar usuario", e);
            if (callback != null) {
                callback.onComplete(null);
            }
        }
    }
    
    // Método para obtener un usuario por su ID
    public void obtenerUsuarioPorId(int id, final DataCallback<Usuario> callback) {
        ApiClient.get("usuarios/" + id, ApiParsers.usuarioParser(), new ApiClient.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener usuario por ID: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener un usuario por su email
    public void obtenerUsuarioPorEmail(String email, final DataCallback<Usuario> callback) {
        ApiClient.get("usuarios?email=" + email, ApiParsers.usuariosListParser(), new ApiClient.ApiCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> result) {
                if (callback != null) {
                    callback.onComplete(result.isEmpty() ? null : result.get(0));
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener usuario por email: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para actualizar los datos de un usuario con imagen base64
    public void actualizarUsuario(Usuario usuario, String base64Image, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("nombre", usuario.getNombre());
            jsonObject.put("email", usuario.getEmail());
            
            // Añadir imagen en base64 si existe
            if (base64Image != null && !base64Image.isEmpty()) {
                jsonObject.put("fotoPerfil", base64Image);
            }
            
            ApiClient.put("usuarios/" + usuario.getId(), jsonObject, response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    // Actualizar la URL de la foto en el objeto Usuario
                    if (jsonResponse.has("foto_perfil")) {
                        usuario.setFotoPerfil(jsonResponse.getString("foto_perfil"));
                    }
                    return 1;
                } catch (JSONException e) {
                    Log.e(TAG, "Error al procesar respuesta JSON", e);
                    return 1; // Devolvemos éxito aunque haya error al procesar la respuesta
                }
            }, new ApiClient.ApiCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    if (callback != null) {
                        callback.onComplete(result);
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error al actualizar usuario: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para actualizar usuario", e);
            if (callback != null) {
                callback.onComplete(0);
            }
        }
    }
    
    // Método para actualizar los datos de un usuario (mantener compatibilidad)
    public void actualizarUsuario(Usuario usuario, final DataCallback<Integer> callback) {
        actualizarUsuario(usuario, null, callback);
    }
    
    // Método para eliminar un usuario
    public void eliminarUsuario(Usuario usuario, final DataCallback<Integer> callback) {
        ApiClient.delete("usuarios/" + usuario.getId(), response -> {
            // La respuesta no es importante, solo nos interesa si fue exitoso
            return 1;
        }, new ApiClient.ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al eliminar usuario: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(0);
                }
            }
        });
    }
    
    // Método para obtener todos los usuarios (administrador)
    public void obtenerTodosLosUsuarios(final DataCallback<List<Usuario>> callback) {
        ApiClient.get("usuarios", ApiParsers.usuariosListParser(), new ApiClient.ApiCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener todos los usuarios: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }

    // Método para buscar usuarios
    public void buscarUsuarios(String busqueda, final DataCallback<List<Usuario>> callback) {
        ApiClient.get("usuarios?busqueda=" + busqueda, ApiParsers.usuariosListParser(), new ApiClient.ApiCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al buscar usuarios: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Interfaz para callbacks
    public interface DataCallback<T> {
        void onComplete(T result);
    }
}