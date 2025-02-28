package com.xabierland.librebook.adapters;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.BookDetailActivity;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.utils.ImageLoader;

import java.util.List;

public class LibroAdapter extends RecyclerView.Adapter<LibroAdapter.LibroViewHolder> {
    
    private List<Libro> libros;
    private OnLibroClickListener listener;
    private Context context;
    
    // Constructor
    public LibroAdapter(List<Libro> libros) {
        this.libros = libros;
    }
    
    // Interfaz para manejar los clics en los elementos
    public interface OnLibroClickListener {
        void onLibroClick(Libro libro, int position);
    }
    
    // Método para establecer el listener
    public void setOnLibroClickListener(OnLibroClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public LibroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_libro, parent, false);
        return new LibroViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LibroViewHolder holder, int position) {
        Libro libro = libros.get(position);
        
        holder.textViewTitulo.setText(libro.getTitulo());
        holder.textViewAutor.setText(libro.getAutor());
        
        // Si hay información del género, mostrarla
        if (libro.getGenero() != null && !libro.getGenero().isEmpty()) {
            holder.textViewGenero.setText(libro.getGenero());
            holder.textViewGenero.setVisibility(View.VISIBLE);
        } else {
            holder.textViewGenero.setVisibility(View.GONE);
        }
        
        // Si hay una URL de portada, cargarla usando nuestro ImageLoader
        if (libro.getPortadaUrl() != null && !libro.getPortadaUrl().isEmpty()) {
            holder.imageViewPortada.setVisibility(View.VISIBLE);
            // Verificar permiso de Internet
            if (ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                ImageLoader.loadImage(libro.getPortadaUrl(), holder.imageViewPortada);
            }
        } else {
            holder.imageViewPortada.setVisibility(View.INVISIBLE);
        }
        
        // Configurar el listener de clic para abrir la actividad de detalle
        holder.itemView.setOnClickListener(v -> {
            // Si se definió un listener personalizado, usarlo
            if (listener != null) {
                listener.onLibroClick(libro, holder.getAdapterPosition());
            } else {
                // Comportamiento por defecto: abrir la actividad de detalle
                openBookDetail(libro);
            }
        });
    }
    
    private void openBookDetail(Libro libro) {
        Intent intent = new Intent(context, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_LIBRO_ID, libro.getId());
        context.startActivity(intent);
    }
    
    @Override
    public int getItemCount() {
        return libros != null ? libros.size() : 0;
    }
    
    // ViewHolder para los elementos de la lista
    static class LibroViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPortada;
        TextView textViewTitulo;
        TextView textViewAutor;
        TextView textViewGenero;
        
        LibroViewHolder(View itemView) {
            super(itemView);
            imageViewPortada = itemView.findViewById(R.id.imageViewPortada);
            textViewTitulo = itemView.findViewById(R.id.textViewTitulo);
            textViewAutor = itemView.findViewById(R.id.textViewAutor);
            textViewGenero = itemView.findViewById(R.id.textViewGenero);
        }
    }
}