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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.BookDetailActivity;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.utils.ImageLoader;
import com.xabierland.librebook.utils.ShareUtils;

import java.util.List;

public class BookCardAdapter extends RecyclerView.Adapter<BookCardAdapter.BookViewHolder> {

    private List<LibroConEstado> books;
    private Context context;
    private boolean showProgress;

    // Añadir un listener para el botón de compartir en la clase BookCardAdapter
    public interface OnBookShareListener {
        void onBookShare(LibroConEstado book);
    }

    // Añadir campo y constructor
    private OnBookShareListener shareListener;

    public BookCardAdapter(List<LibroConEstado> books, boolean showProgress, OnBookShareListener shareListener) {
        this.books = books;
        this.showProgress = showProgress;
        this.shareListener = shareListener;
    }

    // Constructor original para mantener compatibilidad con código existente
    public BookCardAdapter(List<LibroConEstado> books, boolean showProgress) {
        this(books, showProgress, null);
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
            holder.textViewProgress.setText(String.format(context.getString(R.string.progress_percentage), progress));
        } else {
            holder.layoutProgress.setVisibility(View.GONE);
        }

        // Añadir la funcionalidad de compartir con un clic largo
        holder.itemView.setOnLongClickListener(v -> {
            showShareDialog(context, book);
            return true;
        });

        // Configurar clic para abrir detalle del libro
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra(BookDetailActivity.EXTRA_LIBRO_ID, book.getId());
            context.startActivity(intent);
        });
    }

    // Método para mostrar el diálogo de compartir
    private void showShareDialog(Context context, LibroConEstado book) {
        final CharSequence[] items = {
                context.getString(R.string.share_as_text),
                context.getString(R.string.share_as_file)
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.share_book_message);
        builder.setItems(items, (dialog, which) -> {
            switch (which) {
                case 0: // Compartir como texto
                    // Necesitamos obtener el objeto Libro completo
                    LibroRepository repository = new LibroRepository((android.app.Application) context.getApplicationContext());
                    repository.obtenerLibroPorId(book.getId(), libro -> {
                        if (libro != null) {
                            ShareUtils.shareBookAsText(context, libro);
                        } else {
                            Toast.makeText(context, R.string.error_sharing_book, Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case 1: // Compartir como archivo
                    LibroRepository repo = new LibroRepository((android.app.Application) context.getApplicationContext());
                    repo.obtenerLibroPorId(book.getId(), libro -> {
                        if (libro != null) {
                            ShareUtils.shareBookAsFile(context, libro, book);
                        } else {
                            Toast.makeText(context, R.string.error_sharing_book, Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
            }
        });
        builder.show();
        
        // Si hay un listener externo, también notificarle
        if (shareListener != null) {
            shareListener.onBookShare(book);
        }
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