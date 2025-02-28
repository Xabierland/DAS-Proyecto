package com.xabierland.librebook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Libro;

import java.util.List;

public class LibroAdapter extends RecyclerView.Adapter<LibroAdapter.LibroViewHolder> {
    
    private List<Libro> libros;
    private OnLibroClickListener listener;
    
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
        View view = LayoutInflater.from(parent.getContext())
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
        
        // Si hay una URL de portada, cargarla (implementación simple)
        if (libro.getPortadaUrl() != null && !libro.getPortadaUrl().isEmpty()) {
            // Aquí podrías usar una biblioteca como Glide o Picasso para cargar la imagen
            // Por ahora, simplemente mostramos un placeholder
            holder.imageViewPortada.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewPortada.setVisibility(View.INVISIBLE);
        }
        
        // Configurar el listener de clic si está disponible
        if (listener != null) {
            holder.itemView.setOnClickListener(v -> {
                listener.onLibroClick(libro, holder.getAdapterPosition());
            });
        }
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