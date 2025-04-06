package com.xabierland.librebook.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xabierland.librebook.R;
import com.xabierland.librebook.adapters.LibroAdapter;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.data.repositories.UsuarioRepository;
import com.xabierland.librebook.utils.BookstoreFinder;
import com.xabierland.librebook.utils.CustomInfoWindow;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

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
    
    // Nuevas variables para el mapa OpenStreetMap
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private static final float DEFAULT_ZOOM_LEVEL = 15.5f;
    
    private MapView mapView;
    private ProgressBar progressBarMap;
    private TextView textViewNoBookstores;
    private FloatingActionButton fabMyLocation;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private boolean requestingLocationUpdates = false;
    
    private MyLocationNewOverlay myLocationOverlay;
    private CustomInfoWindow customInfoWindow;
    private List<BookstoreFinder.Bookstore> currentBookstores = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar la configuración de osmdroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        setContentView(R.layout.activity_main);
        
        // Inicializar repositorios
        usuarioRepository = new UsuarioRepository(getApplication());
        libroRepository = new LibroRepository(getApplication());
        
        // Inicializar vistas
        initViews();
        
        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Configurar el mapa
        setupMapView();
        
        // Configurar la solicitud de ubicación
        createLocationRequest();
        
        // Configurar el callback de ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Actualizar la UI con la ubicación
                    onLocationReceived(location);
                }
            }
        };
        
        // Verificar si hay usuario logueado y actualizar la interfaz en consecuencia
        checkUserSessionAndUpdateUI();
        
        // Verificar la base de datos
        verificarEstadoLibros();
        
        // Cargar libros recomendados
        loadRecommendedBooks();
        
        // Solicitar permisos para acceder a la ubicación
        requestLocationPermissions();
    }

    private void setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        
        // Añadir overlay de ubicación
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
        
        // Añadir overlay de compass
        CompassOverlay compassOverlay = new CompassOverlay(this, mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
        
        // Permitir rotación con gestos
        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);
        
        // Crear la ventana de información personalizada
        customInfoWindow = new CustomInfoWindow(mapView, this);
        
        // Configurar escala inicial
        mapView.getController().setZoom(DEFAULT_ZOOM_LEVEL);
        
        // Centrar el mapa en una ubicación por defecto (Madrid) hasta que tengamos la ubicación real
        GeoPoint startPoint = new GeoPoint(40.416775, -3.703790);
        mapView.getController().setCenter(startPoint);
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
        
        // Inicializar vistas del mapa
        mapView = findViewById(R.id.mapView);
        progressBarMap = findViewById(R.id.progressBarMap);
        textViewNoBookstores = findViewById(R.id.textViewNoBookstores);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        
        // Configurar botón de mi ubicación
        fabMyLocation.setOnClickListener(v -> {
            if (lastLocation != null) {
                GeoPoint myLocation = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                mapView.getController().animateTo(myLocation);
                mapView.getController().setZoom(DEFAULT_ZOOM_LEVEL);
            } else {
                Toast.makeText(this, R.string.error_location, Toast.LENGTH_SHORT).show();
                startLocationUpdates();
            }
        });
    }
    
    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();
    }
    
    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            // Tenemos permiso, iniciar actualizaciones de ubicación
            startLocationUpdates();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, iniciar actualizaciones de ubicación
                startLocationUpdates();
            } else {
                // Permiso denegado
                Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_LONG).show();
                textViewNoBookstores.setText(R.string.location_permission_needed);
                textViewNoBookstores.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void startLocationUpdates() {
        // Verificar que los servicios de ubicación estén activados
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // Los ajustes de ubicación son adecuados, iniciar actualizaciones
            requestingLocationUpdates = true;
            startLocationClient();
        });
        
        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Los ajustes de ubicación no son adecuados, pero pueden resolverse
                try {
                    // Mostrar diálogo para habilitar ubicación
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignorar el error
                    Log.e(TAG, "Error mostrando el diálogo de ajustes de ubicación", sendEx);
                }
            } else {
                // No se pueden resolver los ajustes de ubicación
                showLocationRequiredDialog();
            }
        });
    }
    
    private void startLocationClient() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        // Mostrar el indicador de carga
        progressBarMap.setVisibility(View.VISIBLE);
        textViewNoBookstores.setVisibility(View.GONE);
        
        // Solicitar actualizaciones de ubicación
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper());
        
        // Obtener la última ubicación conocida
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                onLocationReceived(location);
            }
        });
    }
    
    private void onLocationReceived(Location location) {
        lastLocation = location;
        
        // Centrar el mapa en la ubicación actual
        GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapView.getController().animateTo(currentLocation);
        
        // Buscar librerías cercanas
        findNearbyBookstores(location);
        
        // Podemos detener las actualizaciones de ubicación después de obtener una ubicación
        if (requestingLocationUpdates) {
            stopLocationUpdates();
        }
    }
    
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        requestingLocationUpdates = false;
    }
    
    private void findNearbyBookstores(Location location) {
        // Mostrar el indicador de carga
        progressBarMap.setVisibility(View.VISIBLE);
        textViewNoBookstores.setVisibility(View.GONE);
        
        // Obtener el radio de búsqueda desde resources
        double searchRadius = Double.parseDouble(getString(R.string.bookstore_search_radius));
        
        // Buscar librerías cercanas
        BookstoreFinder.findNearbyBookstores(location, searchRadius, new BookstoreFinder.BookstoreFinderCallback() {
            @Override
            public void onBookstoresFound(List<BookstoreFinder.Bookstore> bookstores) {
                progressBarMap.setVisibility(View.GONE);
                
                if (bookstores.isEmpty()) {
                    textViewNoBookstores.setVisibility(View.VISIBLE);
                    return;
                }
                
                // Guardar la lista actual de librerías
                currentBookstores.clear();
                currentBookstores.addAll(bookstores);
                
                // Limpiar marcadores anteriores (excepto el de ubicación)
                for (int i = mapView.getOverlays().size() - 1; i >= 0; i--) {
                    if (mapView.getOverlays().get(i) instanceof Marker && 
                            !(mapView.getOverlays().get(i).equals(myLocationOverlay))) {
                        mapView.getOverlays().remove(i);
                    }
                }
                
                // Añadir marcadores para cada librería
                for (BookstoreFinder.Bookstore bookstore : bookstores) {
                    addBookstoreMarker(bookstore);
                }
                
                // Actualizar el mapa
                mapView.invalidate();
            }
            
            @Override
            public void onError(String errorMessage) {
                progressBarMap.setVisibility(View.GONE);
                textViewNoBookstores.setVisibility(View.VISIBLE);
                textViewNoBookstores.setText(errorMessage);
            }
        });
    }
    
    private void addBookstoreMarker(BookstoreFinder.Bookstore bookstore) {
        Marker marker = new Marker(mapView);
        marker.setPosition(bookstore.getLocation());
        marker.setTitle(bookstore.getName());
        
        // Obtener icono para librerías
        Drawable icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces);
        marker.setIcon(icon);
        
        // Configurar el info window personalizado
        marker.setInfoWindow(customInfoWindow);
        customInfoWindow.setBookstore(bookstore);
        
        // Añadir un tag con el ID de la librería para identificación
        marker.setRelatedObject(bookstore);
        
        // Añadir el marcador al mapa
        mapView.getOverlays().add(marker);
    }
    
    private void showLocationRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.location_required)
                .setMessage(R.string.enable_location)
                .setPositiveButton(R.string.location_settings, (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // El usuario activó los servicios de ubicación
                startLocationClient();
            } else {
                // El usuario no activó los servicios de ubicación
                showLocationRequiredDialog();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        
        // Verificar sesión cada vez que la actividad se reanuda y actualizar UI
        checkUserSessionAndUpdateUI();
        
        // Reiniciar las actualizaciones de ubicación si es necesario
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        
        // Detener actualizaciones de ubicación para ahorrar batería
        stopLocationUpdates();
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