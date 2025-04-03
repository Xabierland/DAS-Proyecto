package com.xabierland.librebook.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final Executor executor = Executors.newFixedThreadPool(4);

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    // Método genérico para solicitudes GET
    public static <T> void get(String endpoint, ApiResponseParser<T> parser, ApiCallback<T> callback) {
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(urlConnection.getInputStream());
                    T result = parser.parse(response);
                    callback.onSuccess(result);
                } else {
                    String errorResponse = readStream(urlConnection.getErrorStream());
                    callback.onError("Error " + responseCode + ": " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en solicitud GET: " + e.getMessage(), e);
                callback.onError("Error de conexión: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    // Método genérico para solicitudes POST
    public static <T> void post(String endpoint, JSONObject requestBody, ApiResponseParser<T> parser, ApiCallback<T> callback) {
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setDoOutput(true);

                // Escribir cuerpo de la solicitud
                try (OutputStream os = urlConnection.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                    writer.write(requestBody.toString());
                    writer.flush();
                }

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    String response = readStream(urlConnection.getInputStream());
                    T result = parser.parse(response);
                    callback.onSuccess(result);
                } else {
                    String errorResponse = readStream(urlConnection.getErrorStream());
                    callback.onError("Error " + responseCode + ": " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en solicitud POST: " + e.getMessage(), e);
                callback.onError("Error de conexión: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    // Método genérico para solicitudes PUT
    public static <T> void put(String endpoint, JSONObject requestBody, ApiResponseParser<T> parser, ApiCallback<T> callback) {
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("PUT");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setDoOutput(true);

                // Escribir cuerpo de la solicitud
                try (OutputStream os = urlConnection.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                    writer.write(requestBody.toString());
                    writer.flush();
                }

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(urlConnection.getInputStream());
                    T result = parser.parse(response);
                    callback.onSuccess(result);
                } else {
                    String errorResponse = readStream(urlConnection.getErrorStream());
                    callback.onError("Error " + responseCode + ": " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en solicitud PUT: " + e.getMessage(), e);
                callback.onError("Error de conexión: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    // Método genérico para solicitudes DELETE
    public static <T> void delete(String endpoint, ApiResponseParser<T> parser, ApiCallback<T> callback) {
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(ApiConfig.BASE_URL + endpoint);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("DELETE");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    String response = readStream(urlConnection.getInputStream());
                    T result = parser.parse(response);
                    callback.onSuccess(result);
                } else {
                    String errorResponse = readStream(urlConnection.getErrorStream());
                    callback.onError("Error " + responseCode + ": " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en solicitud DELETE: " + e.getMessage(), e);
                callback.onError("Error de conexión: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    // Método para leer la respuesta del servidor
    private static String readStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }

    // Método para manejar errores de respuesta
    private static <T> void handleErrorResponse(HttpURLConnection urlConnection, ApiCallback<T> callback) {
        try {
            int responseCode = urlConnection.getResponseCode();
            String errorResponse = "";
            try {
                errorResponse = readStream(urlConnection.getErrorStream());
            } catch (IOException e) {
                errorResponse = "No se pudo leer el mensaje de error";
            }
            callback.onError("Error " + responseCode + ": " + errorResponse);
        } catch (IOException e) {
            callback.onError("Error de conexión: " + e.getMessage());
        }
    }

    // Interfaz para parsear respuestas
    public interface ApiResponseParser<T> {
        T parse(String jsonResponse) throws JSONException;
    }
}