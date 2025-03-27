package com.xabierland.librebook.data.repositories;

import android.app.Application;
import android.util.Log;

import com.xabierland.librebook.api.ApiClient;
import com.xabierland.librebook.api.ApiParsers;
import com.xabierland.librebook.data.database.entities.Libro;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibroRepository {
    
    private static final String TAG = "LibroRepository";
    private final ExecutorService executorService;
    
    public LibroRepository(Application application) {
        executorService = Executors.newFixedThreadPool(4);
    }
    
    // Método para añadir un nuevo libro
    public void insertarLibro(Libro libro, final DataCallback<Long> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("titulo", libro.getTitulo());
            jsonObject.put("autor", libro.getAutor());
            jsonObject.put("isbn", libro.getIsbn());
            jsonObject.put("descripcion", libro.getDescripcion());
            jsonObject.put("portadaUrl", libro.getPortadaUrl());
            jsonObject.put("anioPublicacion", libro.getAnioPublicacion());
            jsonObject.put("editorial", libro.getEditorial());
            jsonObject.put("genero", libro.getGenero());
            jsonObject.put("numPaginas", libro.getNumPaginas());
            
            ApiClient.post("libros", jsonObject, ApiParsers.idResponseParser(), new ApiClient.ApiCallback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    if (callback != null) {
                        callback.onComplete(result);
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error al insertar libro: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(-1L);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para insertar libro", e);
            if (callback != null) {
                callback.onComplete(-1L);
            }
        }
    }
    
    // Método para añadir múltiples libros
    public void insertarLibros(List<Libro> libros, final DataCallback<List<Long>> callback) {
        // Este método requeriría múltiples llamadas a la API
        // Por simplicidad, podríamos implementarlo como múltiples llamadas individuales
        // o agregar un endpoint específico en la API
        Log.w(TAG, "Método insertarLibros no implementado para la versión de API");
        if (callback != null) {
            callback.onComplete(null);
        }
    }
    
    // Método para actualizar un libro
    public void actualizarLibro(Libro libro, final DataCallback<Integer> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("titulo", libro.getTitulo());
            jsonObject.put("autor", libro.getAutor());
            jsonObject.put("isbn", libro.getIsbn());
            jsonObject.put("descripcion", libro.getDescripcion());
            jsonObject.put("portadaUrl", libro.getPortadaUrl());
            jsonObject.put("anioPublicacion", libro.getAnioPublicacion());
            jsonObject.put("editorial", libro.getEditorial());
            jsonObject.put("genero", libro.getGenero());
            jsonObject.put("numPaginas", libro.getNumPaginas());
            
            ApiClient.put("libros/" + libro.getId(), jsonObject, response -> {
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
                    Log.e(TAG, "Error al actualizar libro: " + errorMessage);
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON para actualizar libro", e);
            if (callback != null) {
                callback.onComplete(0);
            }
        }
    }
    
    // Método para eliminar un libro
    public void eliminarLibro(Libro libro, final DataCallback<Integer> callback) {
        ApiClient.delete("libros/" + libro.getId(), response -> {
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
                Log.e(TAG, "Error al eliminar libro: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(0);
                }
            }
        });
    }
    
    // Método para obtener un libro por su ID
    public void obtenerLibroPorId(int id, final DataCallback<Libro> callback) {
        ApiClient.get("libros/" + id, ApiParsers.libroParser(), new ApiClient.ApiCallback<Libro>() {
            @Override
            public void onSuccess(Libro result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libro por ID: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener un libro por su ISBN
    public void obtenerLibroPorISBN(String isbn, final DataCallback<Libro> callback) {
        ApiClient.get("libros?isbn=" + isbn, ApiParsers.librosListParser(), new ApiClient.ApiCallback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (callback != null) {
                    callback.onComplete(result.isEmpty() ? null : result.get(0));
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libro por ISBN: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener todos los libros
    public void obtenerTodosLosLibros(final DataCallback<List<Libro>> callback) {
        ApiClient.get("libros", ApiParsers.librosListParser(), new ApiClient.ApiCallback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener todos los libros: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para buscar libros por título o autor
    public void buscarLibros(String busqueda, final DataCallback<List<Libro>> callback) {
        ApiClient.get("libros?busqueda=" + busqueda, ApiParsers.librosListParser(), new ApiClient.ApiCallback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al buscar libros: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener libros por género
    public void obtenerLibrosPorGenero(String genero, final DataCallback<List<Libro>> callback) {
        ApiClient.get("libros?genero=" + genero, ApiParsers.librosListParser(), new ApiClient.ApiCallback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libros por género: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener libros por autor
    public void obtenerLibrosPorAutor(String autor, final DataCallback<List<Libro>> callback) {
        ApiClient.get("libros?autor=" + autor, ApiParsers.librosListParser(), new ApiClient.ApiCallback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener libros por autor: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener todos los géneros disponibles
    public void obtenerGeneros(final DataCallback<List<String>> callback) {
        // Este método requeriría un endpoint específico en la API
        // Podríamos implementarlo como una consulta a todos los libros y luego extrayendo los géneros únicos
        ApiClient.get("libros", ApiParsers.librosListParser(), new ApiClient.ApiCallback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (callback != null) {
                    // Extraer géneros únicos
                    List<String> generos = new ArrayList<>();
                    for (Libro libro : result) {
                        String genero = libro.getGenero();
                        if (genero != null && !genero.isEmpty() && !generos.contains(genero)) {
                            generos.add(genero);
                        }
                    }
                    Collections.sort(generos);
                    callback.onComplete(generos);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener géneros: " + errorMessage);
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }
    
    // Método para obtener todos los autores disponibles
    public void obtenerAutores(final DataCallback<List<String>> callback) {
        // Similar a obtenerGeneros
        ApiClient.get("libros", ApiParsers.librosListParser(), new ApiClient.ApiCallback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (callback != null) {
                    // Extraer autores únicos
                    List<String> autores = new ArrayList<>();
                    for (Libro libro : result) {
                        String autor = libro.getAutor();
                        if (autor != null && !autor.isEmpty() && !autores.contains(autor)) {
                            autores.add(autor);
                        }
                    }
                    Collections.sort(autores);
                    callback.onComplete(autores);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error al obtener autores: " + errorMessage);
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