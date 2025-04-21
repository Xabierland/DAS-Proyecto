package com.xabierland.librebook.utils;

import android.location.Location;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Clase utilitaria para buscar librerías cercanas usando la API de Overpass
 */
public class BookstoreFinder {
    
    private static final String TAG = "BookstoreFinder";
    
    /**
     * Clase para almacenar información de una librería
     */
    public static class Bookstore {
        private final String id;
        private final String name;
        private final GeoPoint location;
        private final double distance; // Distancia en km
        
        public Bookstore(String id, String name, GeoPoint location, double distance) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.distance = distance;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public GeoPoint getLocation() {
            return location;
        }
        
        public double getDistance() {
            return distance;
        }
    }
    
    /**
     * Interfaz para el callback de búsqueda de librerías
     */
    public interface BookstoreFinderCallback {
        void onBookstoresFound(List<Bookstore> bookstores);
        void onError(String errorMessage);
    }
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    
    /**
     * Busca librerías cercanas a una ubicación dada usando la API de Overpass
     * @param userLocation Ubicación del usuario
     * @param radiusKm Radio de búsqueda en kilómetros
     * @param callback Callback para manejar los resultados
     */
    public static void findNearbyBookstores(Location userLocation, double radiusKm, BookstoreFinderCallback callback) {
        if (userLocation == null) {
            callback.onError("Ubicación no disponible");
            return;
        }
        
        double lat = userLocation.getLatitude();
        double lon = userLocation.getLongitude();
        
        // Ejecutar en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                String overpassQuery = generateOverpassQuery(lat, lon, radiusKm);
                
                Request request = new Request.Builder()
                        .url(overpassQuery)
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (!response.isSuccessful()) {
                    reportError(callback, "Error en la consulta: " + response.code());
                    return;
                }
                
                String jsonResponse = response.body().string();
                List<Bookstore> bookstores = parseOverpassResponse(jsonResponse, lat, lon);
                
                // Ordenar librerías por distancia
                bookstores.sort((b1, b2) -> Double.compare(b1.getDistance(), b2.getDistance()));
                
                // Enviar resultados al callback en el hilo principal
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onBookstoresFound(bookstores));
                
            } catch (Exception e) {
                Log.e(TAG, "Error al buscar librerías", e);
                reportError(callback, "Error al buscar librerías: " + e.getMessage());
            }
        }).start();
    }
    
    private static void reportError(BookstoreFinderCallback callback, String message) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> callback.onError(message));
    }
    
    /**
     * Genera la URL para la consulta de Overpass API
     */
    private static String generateOverpassQuery(double lat, double lon, double radiusKm) {
        int radiusMeters = (int) (radiusKm * 1000);
        
        String query = "[out:json];" +
                "(node[\"shop\"=\"books\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "node[\"shop\"=\"comic\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "node[\"amenity\"=\"library\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "node[\"shop\"=\"bookstore\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "way[\"shop\"=\"books\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "way[\"shop\"=\"comic\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "way[\"amenity\"=\"library\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "way[\"shop\"=\"bookstore\"](around:" + radiusMeters + "," + lat + "," + lon + "););" +
                "out center;";
        
        // Codificar la consulta para URL
        String encodedQuery;
        try {
            // Usamos la sobrecarga compatible con API antiguas
            encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 siempre está soportado, así que esto no debería ocurrir
            throw new RuntimeException("UTF-8 no soportado", e);
        }
        
        return "https://overpass-api.de/api/interpreter?data=" + encodedQuery;
    }
    
    /**
     * Parsea la respuesta JSON de Overpass API
     */
    private static List<Bookstore> parseOverpassResponse(String jsonResponse, double userLat, double userLon) {
        List<Bookstore> bookstores = new ArrayList<>();
        
        try {
            JsonObject responseObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray elements = responseObject.getAsJsonArray("elements");
            
            for (JsonElement element : elements) {
                JsonObject feature = element.getAsJsonObject();
                
                // Obtener el ID
                String id = feature.get("id").getAsString();
                
                // Obtener las coordenadas (según sea nodo o way)
                double lat, lon;
                if (feature.get("type").getAsString().equals("way")) {
                    JsonObject center = feature.getAsJsonObject("center");
                    lat = center.get("lat").getAsDouble();
                    lon = center.get("lon").getAsDouble();
                } else {
                    lat = feature.get("lat").getAsDouble();
                    lon = feature.get("lon").getAsDouble();
                }
                
                // Obtener el nombre (puede no estar disponible)
                String name = "Librería";
                JsonObject tags = feature.getAsJsonObject("tags");
                if (tags != null && tags.has("name")) {
                    name = tags.get("name").getAsString();
                }
                
                // Calcular distancia al usuario en kilómetros
                float[] results = new float[1];
                Location.distanceBetween(userLat, userLon, lat, lon, results);
                double distanceKm = results[0] / 1000.0; // Convertir metros a kilómetros
                
                GeoPoint location = new GeoPoint(lat, lon);
                bookstores.add(new Bookstore(id, name, location, distanceKm));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear respuesta JSON", e);
        }
        
        return bookstores;
    }
}