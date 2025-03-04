package com.xabierland.librebook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.adapters.LibroAdapter;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.data.repositories.UsuarioRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends BaseActivity {
    
    private static final String TAG = "MainActivity";
    private UsuarioRepository usuarioRepository;
    private LibroRepository libroRepository; 
    private TextView textViewWelcome;
    
    // Para la lista de libros recomendados
    private RecyclerView recyclerViewRecommended;
    private TextView textViewNoRecommended;
    private LibroAdapter recommendedAdapter;
    private List<Libro> recommendedBooks = new ArrayList<>();
    private static final int RECOMMENDED_BOOKS_COUNT = 4;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar repositorios
        usuarioRepository = new UsuarioRepository(getApplication());
        libroRepository = new LibroRepository(getApplication());
        
        // Comprobar si venimos de un cambio de tema
        if (getIntent().getBooleanExtra("THEME_CHANGED", false)) {
            // No hacemos nada especial, el tema ya se ha aplicado en BaseActivity
        }
        
        // Inicializar vistas
        initViews();
        
        // Verificar si hay usuario logueado y actualizar la interfaz en consecuencia
        checkUserSessionAndUpdateUI();
        
        // Verificar la base de datos
        verificarEstadoLibros();
        
        // Cargar libros recomendados
        loadRecommendedBooks();
    }
    
    private void initViews() {
        textViewWelcome = findViewById(R.id.textViewWelcome);
        
        // Inicializar vistas de libros recomendados
        recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
        textViewNoRecommended = findViewById(R.id.textViewNoRecommended);
        
        // Configurar RecyclerView de libros recomendados en orientación vertical
        recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recommendedAdapter = new LibroAdapter(recommendedBooks);
        recyclerViewRecommended.setAdapter(recommendedAdapter);
    }
    
    private void verificarEstadoLibros() {
        libroRepository.obtenerTodosLosLibros(libros -> {
            Log.d(TAG, "Número de libros en la base de datos: " + libros.size());
            
            // Si quieres mostrar un mensaje para verificar, descomenta esta línea
            // runOnUiThread(() -> Toast.makeText(MainActivity.this, "Libros en la BD: " + libros.size(), Toast.LENGTH_SHORT).show());
        });
    }
    
    private void loadRecommendedBooks() {
        // Mostrar indicador de carga (opcional)
        recyclerViewRecommended.setVisibility(View.GONE);
        textViewNoRecommended.setVisibility(View.VISIBLE);
        
        // Obtener todos los libros
        libroRepository.obtenerTodosLosLibros(libros -> {
            if (libros != null && !libros.isEmpty()) {
                // Seleccionar 4 libros aleatorios
                List<Libro> randomBooks = getRandomBooks(libros, RECOMMENDED_BOOKS_COUNT);
                
                // Actualizar la UI en el hilo principal
                runOnUiThread(() -> {
                    recommendedBooks.clear();
                    recommendedBooks.addAll(randomBooks);
                    recommendedAdapter.notifyDataSetChanged();
                    
                    recyclerViewRecommended.setVisibility(View.VISIBLE);
                    textViewNoRecommended.setVisibility(View.GONE);
                });
            } else {
                // No hay libros disponibles
                runOnUiThread(() -> {
                    recyclerViewRecommended.setVisibility(View.GONE);
                    textViewNoRecommended.setVisibility(View.VISIBLE);
                });
            }
        });
    }
    
    /**
     * Selecciona libros aleatorios de una lista
     */
    private List<Libro> getRandomBooks(List<Libro> allBooks, int count) {
        // Si hay menos libros que count, devolver todos
        if (allBooks.size() <= count) {
            return new ArrayList<>(allBooks);
        }
        
        // Crear una copia de la lista original para no modificarla
        List<Libro> shuffledList = new ArrayList<>(allBooks);
        
        // Mezclar la lista para obtener resultados aleatorios
        Collections.shuffle(shuffledList, new Random(System.currentTimeMillis()));
        
        // Tomar los primeros 'count' elementos
        return shuffledList.subList(0, count);
    }
    
    private void checkUserSessionAndUpdateUI() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        
        if (isLoggedIn) {
            // Si hay sesión, cargar datos del usuario
            loadCurrentUserData();
        } else {
            // Si no hay sesión, mostrar mensaje de bienvenida genérico
            updateUIForGuest();
        }
    }
    
    private void updateUIForGuest() {
        if (textViewWelcome != null) {
            textViewWelcome.setText(R.string.welcome_guest);
        }
        
        // Aquí puedes adaptar otros elementos de la UI para usuarios no autenticados
        // Por ejemplo, mostrar secciones limitadas, o sugerencias para que se registren
    }
    
    private void loadCurrentUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = sharedPreferences.getInt("userId", -1);
        
        if (userId != -1) {
            usuarioRepository.obtenerUsuarioPorId(userId, usuario -> {
                if (usuario != null) {
                    runOnUiThread(() -> {
                        // Actualizar la interfaz con los datos del usuario
                        updateUserInterface(usuario);
                    });
                } else {
                    // Si no se encuentra el usuario (incoherencia en datos), limpiar sesión
                    runOnUiThread(this::clearUserSession);
                }
            });
        }
    }
    
    private void updateUserInterface(Usuario usuario) {
        if (textViewWelcome != null) {
            textViewWelcome.setText(getString(R.string.welcome_user, usuario.getNombre()));
        }
        
        // Aquí puedes actualizar más elementos de la UI con los datos del usuario
    }
    
    private void clearUserSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        // Actualizar la interfaz para usuario invitado
        updateUIForGuest();
        
        // Actualizar el menú de navegación
        updateNavigationMenu();
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.app_name);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Verificar sesión cada vez que la actividad se reanuda y actualizar UI
        checkUserSessionAndUpdateUI();
    }
}