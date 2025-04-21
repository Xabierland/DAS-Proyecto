package com.xabierland.librebook.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.ReadingTimerActivity;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.services.ReadingTimerReceiver;
import com.xabierland.librebook.services.ReadingTimerWorker;

public class BookActionsFragment extends Fragment implements AddBookDialogFragment.OnBookActionListener {

    // Interfaz para comunicación con la actividad
    public interface OnBookActionListener {
        void onAddToLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual);
        void onUpdateInLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual);
        void onRemoveFromLibrary();
        boolean isUserLoggedIn();
        void showLoginRequiredDialog();
    }

    private OnBookActionListener listener;
    private MaterialButton buttonAddToLibrary;
    private MaterialButton buttonRemoveFromLibrary;
    private View reviewSection;
    private TextView textViewRating;
    private TextView textViewReview;
    private RatingBar ratingBarDisplay;
    
    private boolean libroYaEnBiblioteca = false;
    private String estadoActual = "";
    private Float calificacionActual = null;
    private String notasActuales = null;
    private Integer paginaActual = null;
    
    // Referencia al libro actual
    private LibroConEstado libro;
    
    // Nuevo campo para el número total de páginas
    private int numPaginasTotal = 0;
    
    // Método para establecer el número total de páginas
    public void setNumPaginasTotal(int numPaginas) {
        this.numPaginasTotal = numPaginas;
    }

    public BookActionsFragment() {
        // Constructor vacío requerido
    }

    public static BookActionsFragment newInstance() {
        return new BookActionsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnBookActionListener) {
            listener = (OnBookActionListener) context;
        } else {
            throw new RuntimeException(context + " debe implementar OnBookActionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar vistas
        initViews(view);
        
        // Configurar listeners
        setupListeners();
    }

    private void initViews(View view) {
        buttonAddToLibrary = view.findViewById(R.id.buttonAddToLibrary);
        buttonRemoveFromLibrary = view.findViewById(R.id.buttonRemoveFromLibrary);
        reviewSection = view.findViewById(R.id.reviewSection);
        textViewRating = view.findViewById(R.id.textViewRating);
        ratingBarDisplay = view.findViewById(R.id.ratingBarDisplay);
        textViewReview = view.findViewById(R.id.textViewReview);
        
        // Añadir el botón del cronómetro de lectura
        Button buttonReadingTimer = view.findViewById(R.id.buttonReadingTimer);
        if (buttonReadingTimer != null) {
            buttonReadingTimer.setOnClickListener(v -> startReadingTimer());
        }
    }

    private void setupListeners() {
        buttonAddToLibrary.setOnClickListener(v -> {
            if (listener.isUserLoggedIn()) {
                showAddToLibraryDialog();
            } else {
                listener.showLoginRequiredDialog();
            }
        });
        
        buttonRemoveFromLibrary.setOnClickListener(v -> {
            if (listener.isUserLoggedIn()) {
                showRemoveFromLibraryDialog();
            } else {
                listener.showLoginRequiredDialog();
            }
        });
    }
    
    private void showRemoveFromLibraryDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.remove_from_library)
                .setMessage(R.string.confirm_remove_book)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    listener.onRemoveFromLibrary();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }
    
    private void startReadingTimer() {
        if (!listener.isUserLoggedIn()) {
            listener.showLoginRequiredDialog();
            return;
        }
        
        // Iniciar la actividad del cronómetro
        Intent intent = new Intent(requireContext(), ReadingTimerActivity.class);
        intent.putExtra(ReadingTimerWorker.PREF_LIBRO_ID, libro.getId());
        startActivity(intent);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Comprobar si hay un cronómetro activo
        updateTimerButtonState();
    }
    
    private void updateTimerButtonState() {
        Button buttonReadingTimer = getView().findViewById(R.id.buttonReadingTimer);
        if (buttonReadingTimer == null || libro == null) return;
        
        boolean isTimerRunning = ReadingTimerReceiver.isTimerRunning(requireContext());
        int currentBookId = ReadingTimerReceiver.getCurrentBookId(requireContext());
        
        // Si hay un cronómetro en ejecución
        if (isTimerRunning) {
            if (currentBookId == libro.getId()) {
                // El cronómetro está activo para este libro
                buttonReadingTimer.setText(R.string.continue_timer);
                buttonReadingTimer.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.purple_700)));
            } else {
                // El cronómetro está activo para otro libro
                String currentTitle = ReadingTimerReceiver.getCurrentBookTitle(requireContext());
                buttonReadingTimer.setText(getString(R.string.timer_active_for, currentTitle));
                buttonReadingTimer.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.purple_500)));
            }
        } else {
            // No hay cronómetro activo
            buttonReadingTimer.setText(R.string.start_reading_timer);
            buttonReadingTimer.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.purple_500)));
        }
    }

    // Método para actualizar la UI según el estado del libro en la biblioteca del usuario
    public void updateUIForExistingBook(LibroConEstado libroConEstado) {
        if (libroConEstado == null || getContext() == null) return;
        
        libroYaEnBiblioteca = true;
        this.libro = libroConEstado;
        
        // Almacenar el estado actual
        estadoActual = libroConEstado.getEstadoLectura();
        calificacionActual = libroConEstado.getCalificacion();
        notasActuales = libroConEstado.getNotas();
        paginaActual = libroConEstado.getPaginaActual();
        
        // Cambiar el texto del botón según el estado actual
        String estadoLabel = "";
        switch (estadoActual) {
            case UsuarioLibro.ESTADO_POR_LEER:
                estadoLabel = getString(R.string.status_to_read);
                break;
            case UsuarioLibro.ESTADO_LEYENDO:
                estadoLabel = getString(R.string.status_reading);
                break;
            case UsuarioLibro.ESTADO_LEIDO:
                estadoLabel = getString(R.string.status_read);
                break;
        }
        
        buttonAddToLibrary.setText(getString(R.string.update_in_library) + " (" + estadoLabel + ")");
        
        // Mostrar el botón de eliminar
        buttonRemoveFromLibrary.setVisibility(View.VISIBLE);
        
        // Determinar si se debe mostrar la sección de calificación y reseña
        boolean mostrarReviewSection = UsuarioLibro.ESTADO_LEIDO.equals(estadoActual) && 
                                      (calificacionActual != null || (notasActuales != null && !notasActuales.isEmpty()));
        
        if (mostrarReviewSection) {
            reviewSection.setVisibility(View.VISIBLE);
            
            // Mostrar calificación si existe
            if (calificacionActual != null) {
                // Configurar el RatingBar (convertir de 0-10 a 0-5 estrellas)
                float ratingStars = calificacionActual / 2;
                ratingBarDisplay.setRating(ratingStars);
                textViewRating.setText(formatRating(calificacionActual));
                textViewRating.setVisibility(View.VISIBLE);
                ratingBarDisplay.setVisibility(View.VISIBLE);
            } else {
                ratingBarDisplay.setVisibility(View.GONE);
                textViewRating.setVisibility(View.GONE);
            }
            
            // Mostrar reseña si existe
            if (notasActuales != null && !notasActuales.isEmpty()) {
                textViewReview.setText(notasActuales);
                textViewReview.setVisibility(View.VISIBLE);
            } else {
                textViewReview.setVisibility(View.GONE);
            }
        } else {
            // Si no está en estado leído, ocultar la sección completa
            reviewSection.setVisibility(View.GONE);
        }
        
        // Actualizar estado del botón de cronómetro
        updateTimerButtonState();
    }

    // Método para actualizar la UI para un libro nuevo (no está en la biblioteca)
    public void updateUIForNewBook() {
        libroYaEnBiblioteca = false;
        buttonAddToLibrary.setText(R.string.add_to_library);
        reviewSection.setVisibility(View.GONE);
        buttonRemoveFromLibrary.setVisibility(View.GONE);
    }

    private void showAddToLibraryDialog() {
        AddBookDialogFragment dialogFragment = AddBookDialogFragment.newInstance(
            numPaginasTotal,
            libroYaEnBiblioteca,
            estadoActual,
            calificacionActual,
            notasActuales,
            paginaActual
        );
        dialogFragment.show(getChildFragmentManager(), "AddBookDialog");
    }

    private String formatRating(float rating) {
        // Si el valor es un número entero (parte decimal es cero)
        if (rating == Math.floor(rating)) {
            return String.format("%.0f/10", rating);
        } else {
            return String.format("%.1f/10", rating);
        }
    }
    
    // Implementación de los métodos de la interfaz AddBookDialogFragment.OnBookActionListener
    @Override
    public void onAddToLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual) {
        // Redirigir al listener original
        if (listener != null) {
            listener.onAddToLibrary(estadoLectura, calificacion, review, paginaActual);
        }
    }
    
    @Override
    public void onUpdateInLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual) {
        // Redirigir al listener original
        if (listener != null) {
            listener.onUpdateInLibrary(estadoLectura, calificacion, review, paginaActual);
        }
    }
}