package com.xabierland.librebook.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.adapters.LibroAdapter;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.data.repositories.UsuarioRepository;
import com.xabierland.librebook.fragments.MapFragment;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends BaseActivity implements MapFragment.MapFragmentListener {
    
    private static final String TAG = "MainActivity";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;
    private static final long RECOMENDACIONES_UPDATE_INTERVAL = 15000; // 15 segundos en milisegundos
    
    private UsuarioRepository usuarioRepository;
    private LibroRepository libroRepository; 
    private TextView textViewWelcome;
    private CardView cardNearbyBookstores;
    
    // Para la lista de libros recomendados
    private RecyclerView recyclerViewRecommended;
    private TextView textViewNoRecommended;
    private LibroAdapter recommendedAdapter;
    private List<Libro> recommendedBooks = new ArrayList<>();
    private static final int RECOMMENDED_BOOKS_COUNT = 4;
    
    // Control del fragment del mapa
    private MapFragment mapFragment;
    private boolean isMapFragmentShowing = false;
    
    // Handler y Runnable para actualización periódica de recomendaciones
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRecommendationsRunnable = new Runnable() {
        @Override
        public void run() {
            // Cargar nuevos libros recomendados
            loadRecommendedBooks();
            
            // Programar la siguiente actualización
            handler.postDelayed(this, RECOMENDACIONES_UPDATE_INTERVAL);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar repositorios
        usuarioRepository = new UsuarioRepository(getApplication());
        libroRepository = new LibroRepository(getApplication());
        
        // Inicializar vistas
        initViews();
        
        // Verificar si hay usuario logueado y actualizar la interfaz en consecuencia
        checkUserSessionAndUpdateUI();
        
        // Verificar la base de datos
        verificarEstadoLibros();
        
        // Cargar libros recomendados (primera vez)
        loadRecommendedBooks();

        // Suscribirse al tema "all_devices" para recibir notificaciones
        FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
        .addOnCompleteListener(task -> {
            String msg = "Suscripción exitosa al tema all_devices";
            if (!task.isSuccessful()) {
                msg = "Error al suscribirse al tema all_devices";
            }
            Log.d("FCM", msg);
        });
        
        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            // Aquí puedes manejar el resultado
        }
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
        
        // Configurar el botón de librerías cercanas
        cardNearbyBookstores = findViewById(R.id.cardNearbyBookstores);
        cardNearbyBookstores.setOnClickListener(v -> showMapFragment());
    }
    
    private void showMapFragment() {
        if (!isMapFragmentShowing) {
            isMapFragmentShowing = true;
            mapFragment = new MapFragment();
            mapFragment.show(getSupportFragmentManager(), "MapFragment");
        }
    }
    
    @Override
    public void onMapFragmentClosed() {
        isMapFragmentShowing = false;
        mapFragment = null;
    }
    
    @Override
    public void onBackPressed() {
        if (isMapFragmentShowing && mapFragment != null) {
            mapFragment.dismiss();
            return;
        }
        super.onBackPressed();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Verificar sesión cada vez que la actividad se reanuda y actualizar UI
        checkUserSessionAndUpdateUI();
        
        // Iniciar la actualización periódica de recomendaciones
        startPeriodicRecommendationsUpdate();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Detener la actualización periódica de recomendaciones cuando la actividad está en segundo plano
        stopPeriodicRecommendationsUpdate();
    }
    
    /**
     * Inicia la actualización periódica de recomendaciones
     */
    private void startPeriodicRecommendationsUpdate() {
        // Eliminar cualquier callback pendiente para evitar duplicados
        handler.removeCallbacks(updateRecommendationsRunnable);
        
        // Programar la primera actualización después de 15 segundos
        handler.postDelayed(updateRecommendationsRunnable, RECOMENDACIONES_UPDATE_INTERVAL);
        
        Log.d(TAG, "Actualización periódica de recomendaciones iniciada");
    }
    
    /**
     * Detiene la actualización periódica de recomendaciones
     */
    private void stopPeriodicRecommendationsUpdate() {
        // Eliminar el callback para detener las actualizaciones
        handler.removeCallbacks(updateRecommendationsRunnable);
        
        Log.d(TAG, "Actualización periódica de recomendaciones detenida");
    }
    
    // Métodos existentes (checkUserSessionAndUpdateUI, verificarEstadoLibros, loadRecommendedBooks, etc.)
    private void verificarEstadoLibros() {
        libroRepository.obtenerTodosLosLibros(libros -> {
            if (libros != null) {
                Log.d(TAG, "Número de libros en la base de datos: " + libros.size());
                // Resto del código...
            } else {
                Log.e(TAG, "Error al obtener los libros: la lista es nula");
                // Manejo de error
            }
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
                    
                    // Log para confirmar la actualización
                    Log.d(TAG, "Lista de recomendaciones actualizada con " + randomBooks.size() + " libros");
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
}