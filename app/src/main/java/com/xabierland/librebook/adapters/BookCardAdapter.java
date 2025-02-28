package com.xabierland.librebook.adapters;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.BookDetailActivity;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.utils.ImageLoader;

import java.util.List;

public class BookCardAdapter extends RecyclerView.Adapter<BookCardAdapter.BookViewHolder> {

    private List<LibroConEstado> books;
    private Context context;
    private boolean showProgress;

    public BookCardAdapter(List<LibroConEstado> books, boolean showProgress) {
        this.books = books;
        this.showProgress = showProgress;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_book_card, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        LibroConEstado book = books.get(position);

        // Configurar título y autor
        holder.textViewBookTitle.setText(book.getTitulo());
        holder.textViewBookAuthor.setText(book.getAutor());

        // Cargar portada del libro si está disponible
        if (book.getPortadaUrl() != null && !book.getPortadaUrl().isEmpty()) {
            // Verificar permiso de Internet
            if (ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                ImageLoader.loadImage(book.getPortadaUrl(), holder.imageViewBookCover);
            }
        }

        // Mostrar progreso de lectura si está en estado "leyendo" y se ha solicitado mostrar progreso
        if (showProgress && book.estaEnProgreso() && book.getPaginaActual() != null) {
            holder.layoutProgress.setVisibility(View.VISIBLE);
            int progress = book.getProgresoLectura();
            holder.progressBarReading.setProgress(progress);
            holder.textViewProgress.setText(progress + "%");
        } else {
            holder.layoutProgress.setVisibility(View.GONE);
        }

        // Configurar clic para abrir detalle del libro
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra(BookDetailActivity.EXTRA_LIBRO_ID, book.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return books != null ? books.size() : 0;
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewBookCover;
        TextView textViewBookTitle;
        TextView textViewBookAuthor;
        LinearLayout layoutProgress;
        ProgressBar progressBarReading;
        TextView textViewProgress;

        BookViewHolder(View itemView) {
            super(itemView);
            imageViewBookCover = itemView.findViewById(R.id.imageViewBookCover);
            textViewBookTitle = itemView.findViewById(R.id.textViewBookTitle);
            textViewBookAuthor = itemView.findViewById(R.id.textViewBookAuthor);
            layoutProgress = itemView.findViewById(R.id.layoutProgress);
            progressBarReading = itemView.findViewById(R.id.progressBarReading);
            textViewProgress = itemView.findViewById(R.id.textViewProgress);
        }
    }
}