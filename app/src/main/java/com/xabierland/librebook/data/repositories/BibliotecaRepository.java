package com.xabierland.librebook.data.repositories;

import android.app.Application;
import android.util.Log;

import com.xabierland.librebook.api.ApiClient;
import com.xabierland.librebook.api.ApiParsers;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.models.LibroConEstado;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BibliotecaRepository {
    
    private static final String TAG = "BibliotecaRepository";
    private final ExecutorService executorService;
    
    public BibliotecaRepository(Application application) {
        executorService = Executors.newFixedThreadPool(4);
    }
    
    // Método para añadir un libro a la biblioteca del usuario
    public void agregarLibro(int usuarioId, int libroId, String estadoLectura, final DataCallback<Long> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("libroId", libroId);
            jsonObject.put("estadoLectura", estadoLectura);
            
            ApiClient.post("biblioteca/" + usuarioId, jsonObject, ApiParsers.idResponseParser(), new ApiClient.ApiCallback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    if (callback != null) {
                        callback.onComplete(result);
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error al agregar libro a biblioteca: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(-1L);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para agregar libro", e);
            if (callback != null) {
                callback.onComplete(-1L);
            }
        }
    }
    
    // Método para obtener todos los libros de un usuario
    public void obtenerTodosLosLibrosDeUsuario(int usuarioId, final DataCallback<List<LibroConEstado>> callback) {
        ApiClient.get("biblioteca/" + usuarioId, ApiParsers.librosConEstadoListParser(), new ApiClient.ApiCallback<List<LibroConEstado>>() {
            @Override
            public void onSuccess(List<LibroConEstado> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libros de usuario: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener un libro específico de la biblioteca del usuario
    public void obtenerLibroDeBiblioteca(int usuarioId, int libroId, final DataCallback<LibroConEstado> callback) {
        ApiClient.get("biblioteca/" + usuarioId + "/" + libroId, ApiParsers.libroConEstadoParser(), new ApiClient.ApiCallback<LibroConEstado>() {
            @Override
            public void onSuccess(LibroConEstado result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libro de biblioteca: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener libros por estado de lectura
    public void obtenerLibrosPorEstado(int usuarioId, String estadoLectura, final DataCallback<List<LibroConEstado>> callback) {
        ApiClient.get("biblioteca/" + usuarioId + "?estado=" + estadoLectura, ApiParsers.librosConEstadoListParser(), new ApiClient.ApiCallback<List<LibroConEstado>>() {
            @Override
            public void onSuccess(List<LibroConEstado> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libros por estado: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener libros por leer
    public void obtenerLibrosPorLeer(int usuarioId, final DataCallback<List<LibroConEstado>> callback) {
        obtenerLibrosPorEstado(usuarioId, UsuarioLibro.ESTADO_POR_LEER, callback);
    }
    
    // Método para obtener libros en lectura
    public void obtenerLibrosLeyendo(int usuarioId, final DataCallback<List<LibroConEstado>> callback) {
        obtenerLibrosPorEstado(usuarioId, UsuarioLibro.ESTADO_LEYENDO, callback);
    }
    
    // Método para obtener libros leídos
    public void obtenerLibrosLeidos(int usuarioId, final DataCallback<List<LibroConEstado>> callback) {
        obtenerLibrosPorEstado(usuarioId, UsuarioLibro.ESTADO_LEIDO, callback);
    }
    
    // Método para obtener libros favoritos
    public void obtenerLibrosFavoritos(int usuarioId, final DataCallback<List<LibroConEstado>> callback) {
        ApiClient.get("biblioteca/" + usuarioId + "?favoritos=1", ApiParsers.librosConEstadoListParser(), new ApiClient.ApiCallback<List<LibroConEstado>>() {
            @Override
            public void onSuccess(List<LibroConEstado> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libros favoritos: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para marcar/desmarcar un libro como favorito
    public void marcarComoFavorito(int usuarioId, int libroId, boolean esFavorito, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("esFavorito", esFavorito);
            
            ApiClient.put("biblioteca/" + usuarioId + "/" + libroId, jsonObject, response -> {
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
                    Log.e(TAG, "Error al marcar como favorito: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para marcar favorito", e);
            if (callback != null) {
                callback.onComplete(0);
            }
        }
    }
    
    // Método para cambiar el estado de lectura de un libro
    public void cambiarEstadoLectura(int usuarioId, int libroId, String estadoLectura, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("estadoLectura", estadoLectura);
            
            ApiClient.put("biblioteca/" + usuarioId + "/" + libroId, jsonObject, response -> {
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
                    Log.e(TAG, "Error al cambiar estado de lectura: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para cambiar estado", e);
            if (callback != null) {
                callback.onComplete(0);
            }
        }
    }
    
    // Método para actualizar la calificación de un libro
    public void actualizarCalificacion(int usuarioId, int libroId, float calificacion, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("calificacion", calificacion);
            
            ApiClient.put("biblioteca/" + usuarioId + "/" + libroId, jsonObject, response -> {
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
                    Log.e(TAG, "Error al actualizar calificación: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para actualizar calificación", e);
            if (callback != null) {
                callback.onComplete(0);
            }
        }
    }
    
    // Método para actualizar la página actual de un libro
    public void actualizarPaginaActual(int usuarioId, int libroId, int paginaActual, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("paginaActual", paginaActual);
            
            ApiClient.put("biblioteca/" + usuarioId + "/" + libroId, jsonObject, response -> {
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
                    Log.e(TAG, "Error al actualizar página actual: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para actualizar página", e);
            if (callback != null) {
                callback.onComplete(0);
            }
        }
    }
    
    // Método para guardar notas sobre un libro
    public void guardarNotas(int usuarioId, int libroId, String notas, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("notas", notas);
            
            ApiClient.put("biblioteca/" + usuarioId + "/" + libroId, jsonObject, response -> {
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
                    Log.e(TAG, "Error al guardar notas: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para guardar notas", e);
            if (callback != null) {
                callback.onComplete(0);
            }
        }
    }
    
    // Método para eliminar un libro de la biblioteca del usuario
    public void eliminarLibroDeBiblioteca(int usuarioId, int libroId, final DataCallback<Integer> callback) {
        ApiClient.delete("biblioteca/" + usuarioId + "/" + libroId, response -> {
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
                Log.e(TAG, "Error al eliminar libro de biblioteca: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(0);
                }
            }
        });
    }
    
    // Método para contar libros por estado
    public void contarLibrosPorEstado(int usuarioId, String estadoLectura, final DataCallback<Integer> callback) {
        // Este método requiere una funcionalidad adicional en la API
        // Podemos implementarlo obteniendo todos los libros por estado y contando el resultado
        obtenerLibrosPorEstado(usuarioId, estadoLectura, result -> {
            if (callback != null) {
                callback.onComplete(result != null ? result.size() : 0);
            }
        });
    }
    
    // Método para obtener la calificación promedio
    public void obtenerCalificacionPromedio(int usuarioId, final DataCallback<Float> callback) {
        // Este método requiere una funcionalidad adicional en la API
        // Podemos implementarlo obteniendo todos los libros leídos y calculando el promedio
        obtenerLibrosLeidos(usuarioId, result -> {
            if (result != null && !result.isEmpty()) {
                float suma = 0;
                int contador = 0;
                
                for (LibroConEstado libro : result) {
                    if (libro.getCalificacion() != null) {
                        suma += libro.getCalificacion();
                        contador++;
                    }
                }
                
                float promedio = contador > 0 ? suma / contador : 0;
                if (callback != null) {
                    callback.onComplete(promedio);
                }
            } else {
                if (callback != null) {
                    callback.onComplete(0f);
                }
            }
        });
    }
    
    // Interfaz para callbacks
    public interface DataCallback<T> {
        void onComplete(T result);
    }
}