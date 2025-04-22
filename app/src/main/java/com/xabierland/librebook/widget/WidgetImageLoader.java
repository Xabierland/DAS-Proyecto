package com.xabierland.librebook.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import com.xabierland.librebook.R;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Clase utilitaria para cargar imágenes en widgets desde URL
 */
public class WidgetImageLoader {
    private static final String TAG = "WidgetImageLoader";

    /**
     * Carga una imagen desde una URL y la establece en un widget
     */
    public static void loadImageForWidget(Context context, String imageUrl, int appWidgetId, 
                                         AppWidgetManager appWidgetManager, RemoteViews views, int imageViewId) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            // Si la URL está vacía, utilizar una imagen por defecto
            views.setImageViewResource(imageViewId, R.mipmap.ic_launcher);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Iniciar carga asíncrona
        new DownloadImageTask(context, appWidgetId, appWidgetManager, views, imageViewId)
                .execute(imageUrl);
    }

    /**
     * AsyncTask para descargar imágenes sin bloquear el hilo principal
     */
    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<Context> contextRef;
        private final int appWidgetId;
        private final AppWidgetManager appWidgetManager;
        private final RemoteViews views;
        private final int imageViewId;

        DownloadImageTask(Context context, int appWidgetId, AppWidgetManager appWidgetManager, 
                          RemoteViews views, int imageViewId) {
            this.contextRef = new WeakReference<>(context);
            this.appWidgetId = appWidgetId;
            this.appWidgetManager = appWidgetManager;
            this.views = views;
            this.imageViewId = imageViewId;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String imageUrl = urls[0];
            Bitmap bitmap = null;
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                Log.e(TAG, "Error al descargar imagen: " + e.getMessage(), e);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Context context = contextRef.get();
            if (context == null) return;

            if (result != null) {
                // Establecer la imagen descargada en el widget
                views.setImageViewBitmap(imageViewId, result);
            } else {
                // En caso de error, mostrar imagen por defecto
                views.setImageViewResource(imageViewId, R.mipmap.ic_launcher);
            }

            // Actualizar el widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}