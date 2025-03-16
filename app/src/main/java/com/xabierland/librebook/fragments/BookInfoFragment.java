package com.xabierland.librebook.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.utils.ImageLoader;

public class BookInfoFragment extends Fragment {

    private ImageView imageViewPortada;
    private TextView textViewTitulo;
    private TextView textViewAutor;
    private TextView textViewGenero;
    private TextView textViewAnio;
    private TextView textViewEditorial;
    private TextView textViewIsbn;
    private TextView textViewPaginas;
    private TextView textViewDescripcion;
    
    private Libro libro;

    public BookInfoFragment() {
        // Constructor vacío requerido
    }

    public static BookInfoFragment newInstance() {
        return new BookInfoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar vistas
        initViews(view);
    }

    private void initViews(View view) {
        imageViewPortada = view.findViewById(R.id.imageViewPortada);
        textViewTitulo = view.findViewById(R.id.textViewTitulo);
        textViewAutor = view.findViewById(R.id.textViewAutor);
        textViewGenero = view.findViewById(R.id.textViewGenero);
        textViewAnio = view.findViewById(R.id.textViewAnio);
        textViewEditorial = view.findViewById(R.id.textViewEditorial);
        textViewIsbn = view.findViewById(R.id.textViewIsbn);
        textViewPaginas = view.findViewById(R.id.textViewPaginas);
        textViewDescripcion = view.findViewById(R.id.textViewDescripcion);
    }

    // Método para actualizar la UI con los datos del libro
    public void setLibro(Libro libro) {
        this.libro = libro;
        updateUI();
    }
    
    // Método para obtener el número de páginas del libro
    public int getNumPaginas() {
        return libro != null ? libro.getNumPaginas() : 0;
    }

    @SuppressLint("DefaultLocale")
    private void updateUI() {
        if (libro == null || getContext() == null) return;

        // Actualizar todos los campos con la información del libro
        textViewTitulo.setText(libro.getTitulo());
        textViewAutor.setText(libro.getAutor());
        
        if (libro.getGenero() != null && !libro.getGenero().isEmpty()) {
            textViewGenero.setText(libro.getGenero());
            textViewGenero.setVisibility(View.VISIBLE);
        } else {
            textViewGenero.setVisibility(View.GONE);
        }
        
        textViewAnio.setText(String.format(getString(R.string.year_format), libro.getAnioPublicacion()));

        if (libro.getEditorial() != null && !libro.getEditorial().isEmpty()) {
            textViewEditorial.setText(String.format(getString(R.string.publisher_format), libro.getEditorial()));
            textViewEditorial.setVisibility(View.VISIBLE);
        } else {
            textViewEditorial.setVisibility(View.GONE);
        }
        
        if (libro.getIsbn() != null && !libro.getIsbn().isEmpty()) {
            textViewIsbn.setText(String.format(getString(R.string.isbn_format), libro.getIsbn()));
            textViewIsbn.setVisibility(View.VISIBLE);
        } else {
            textViewIsbn.setVisibility(View.GONE);
        }
        
        textViewPaginas.setText(String.format(getString(R.string.pages_format), libro.getNumPaginas()));
        
        if (libro.getDescripcion() != null && !libro.getDescripcion().isEmpty()) {
            textViewDescripcion.setText(libro.getDescripcion());
        } else {
            textViewDescripcion.setText(getString(R.string.no_description));
        }
        
        // Cargar la imagen de la portada
        if (libro.getPortadaUrl() != null && !libro.getPortadaUrl().isEmpty()) {
            imageViewPortada.setVisibility(View.VISIBLE);
            // Verificar permiso de Internet
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                ImageLoader.loadImage(libro.getPortadaUrl(), imageViewPortada);
            }
        } else {
            imageViewPortada.setVisibility(View.INVISIBLE);
        }
    }
}