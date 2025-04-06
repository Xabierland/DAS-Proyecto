package com.xabierland.librebook.utils;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.xabierland.librebook.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

/**
 * Ventana de información personalizada para los marcadores de librerías
 */
public class CustomInfoWindow extends InfoWindow {
    
    private final Activity activity;
    private BookstoreFinder.Bookstore bookstore;
    
    public CustomInfoWindow(MapView mapView, Activity activity) {
        super(R.layout.marker_info_window, mapView);
        this.activity = activity;
    }
    
    public void setBookstore(BookstoreFinder.Bookstore bookstore) {
        this.bookstore = bookstore;
    }
    
    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker) item;
        
        // Obtener referencias a las vistas
        TextView tvTitle = mView.findViewById(R.id.bubble_title);
        TextView tvDescription = mView.findViewById(R.id.bubble_description);
        
        // Colocar los datos de la librería
        if (bookstore != null) {
            tvTitle.setText(bookstore.getName());
            
            // Formatear la distancia
            String distance = String.format(
                    activity.getString(R.string.distance_format), 
                    bookstore.getDistance());
            
            tvDescription.setText(distance);
        }
    }

    @Override
    public void onClose() {
        // No es necesario implementar nada aquí
    }
}