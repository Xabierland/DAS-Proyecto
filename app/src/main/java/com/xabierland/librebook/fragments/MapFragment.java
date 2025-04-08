package com.xabierland.librebook.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

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
import java.util.List;

public class MapFragment extends DialogFragment {

    private static final String TAG = "MapFragment";
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private static final float DEFAULT_ZOOM_LEVEL = 15.5f;

    // Vistas
    private MapView mapView;
    private ProgressBar progressBarMap;
    private TextView textViewNoBookstores;
    private FloatingActionButton fabMyLocation;
    private ImageButton buttonCloseMap;

    // Cliente de ubicación
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private boolean requestingLocationUpdates = false;

    // Overlays del mapa
    private MyLocationNewOverlay myLocationOverlay;
    private CustomInfoWindow customInfoWindow;
    private List<BookstoreFinder.Bookstore> currentBookstores = new ArrayList<>();

    // Callback para comunicar eventos al activity
    public interface MapFragmentListener {
        void onMapFragmentClosed();
    }

    private MapFragmentListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (MapFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " debe implementar MapFragmentListener");
        }
        
        // Inicializar la configuración de osmdroid
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        Configuration.getInstance().setUserAgentValue(context.getPackageName());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_LibreBook);
        
        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        // Inicializar vistas
        mapView = view.findViewById(R.id.mapViewFragment);
        progressBarMap = view.findViewById(R.id.progressBarMapFragment);
        textViewNoBookstores = view.findViewById(R.id.textViewNoBookstoresFragment);
        fabMyLocation = view.findViewById(R.id.fabMyLocationFragment);
        buttonCloseMap = view.findViewById(R.id.buttonCloseMap);
        
        // Configurar eventos de clic
        buttonCloseMap.setOnClickListener(v -> dismiss());
        
        // Cerrar el fragment al hacer clic en el fondo oscurecido
        view.setOnClickListener(v -> {
            // Solo cerrar si el clic fue directamente en el fondo, no en el contenido
            if (v.getId() == R.id.root_layout) {
                dismiss();
            }
        });
        
        fabMyLocation.setOnClickListener(v -> {
            if (lastLocation != null) {
                GeoPoint myLocation = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                mapView.getController().animateTo(myLocation);
                mapView.getController().setZoom(DEFAULT_ZOOM_LEVEL);
            } else {
                Toast.makeText(requireContext(), R.string.error_location, Toast.LENGTH_SHORT).show();
                startLocationUpdates();
            }
        });
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Configurar el mapa
        setupMapView();
        
        // Solicitar permisos para acceder a la ubicación
        requestLocationPermissions();
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Hacer que el diálogo ocupe toda la pantalla
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        
        // Reiniciar las actualizaciones de ubicación si es necesario
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        
        // Detener actualizaciones de ubicación para ahorrar batería
        stopLocationUpdates();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null) {
            listener.onMapFragmentClosed();
        }
    }

    private void setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        
        // Añadir overlay de ubicación
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
        
        // Añadir overlay de compass
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
        
        // Permitir rotación con gestos
        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);
        
        // Crear la ventana de información personalizada
        customInfoWindow = new CustomInfoWindow(mapView, requireActivity());
        
        // Configurar escala inicial
        mapView.getController().setZoom(DEFAULT_ZOOM_LEVEL);
        
        // Centrar el mapa en una ubicación por defecto (Madrid) hasta que tengamos la ubicación real
        GeoPoint startPoint = new GeoPoint(40.416775, -3.703790);
        mapView.getController().setCenter(startPoint);
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            // Tenemos permiso, iniciar actualizaciones de ubicación
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, iniciar actualizaciones de ubicación
                startLocationUpdates();
            } else {
                // Permiso denegado
                Toast.makeText(requireContext(), R.string.location_permission_needed, Toast.LENGTH_LONG).show();
                textViewNoBookstores.setText(R.string.location_permission_needed);
                textViewNoBookstores.setVisibility(View.VISIBLE);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startLocationUpdates() {
        // Verificar que los servicios de ubicación estén activados
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        
        SettingsClient client = LocationServices.getSettingsClient(requireActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        
        task.addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
            // Los ajustes de ubicación son adecuados, iniciar actualizaciones
            requestingLocationUpdates = true;
            startLocationClient();
        });
        
        task.addOnFailureListener(requireActivity(), e -> {
            if (e instanceof ResolvableApiException) {
                // Los ajustes de ubicación no son adecuados, pero pueden resolverse
                try {
                    // Mostrar diálogo para habilitar ubicación
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS);
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
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
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
                if (!isAdded()) return; // Verificar que el fragment sigue añadido
                
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
                if (!isAdded()) return; // Verificar que el fragment sigue añadido
                
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
        Drawable icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces);
        marker.setIcon(icon);
        
        // Configurar el info window personalizado
        marker.setInfoWindow(customInfoWindow);
        
        // Guardar la referencia a la librería en el marcador
        marker.setRelatedObject(bookstore);
        
        // Añadir el marcador al mapa
        mapView.getOverlays().add(marker);
    }

    private void showLocationRequiredDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
}