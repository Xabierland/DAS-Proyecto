package com.xabierland.librebook.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.models.LibroConEstado;

public class BookActionsFragment extends Fragment {

    // Interfaz para comunicación con la actividad
    public interface OnBookActionListener {
        void onAddToLibrary(String estadoLectura, float calificacion, String review);
        void onUpdateInLibrary(String estadoLectura, float calificacion, String review);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.remove_from_library)
                .setMessage(R.string.confirm_remove_book)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    listener.onRemoveFromLibrary();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    // Método para actualizar la UI según el estado del libro en la biblioteca del usuario
    public void updateUIForExistingBook(LibroConEstado libroConEstado) {
        if (libroConEstado == null || getContext() == null) return;
        
        libroYaEnBiblioteca = true;
        
        // Almacenar el estado actual
        estadoActual = libroConEstado.getEstadoLectura();
        calificacionActual = libroConEstado.getCalificacion();
        notasActuales = libroConEstado.getNotas();
        
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
        
        // Mostrar sección de calificación y reseña si existen
        if (calificacionActual != null || (notasActuales != null && !notasActuales.isEmpty())) {
            reviewSection.setVisibility(View.VISIBLE);
            
            // Mostrar calificación si existe
            if (calificacionActual != null) {
                // Configurar el RatingBar (convertir de 0-10 a 0-5 estrellas)
                float ratingStars = calificacionActual / 2;
                ratingBarDisplay.setRating(ratingStars);
                textViewRating.setText(formatRating(calificacionActual));
                textViewRating.setVisibility(View.VISIBLE);
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
            reviewSection.setVisibility(View.GONE);
        }
    }

    // Método para actualizar la UI para un libro nuevo (no está en la biblioteca)
    public void updateUIForNewBook() {
        libroYaEnBiblioteca = false;
        buttonAddToLibrary.setText(R.string.add_to_library);
        reviewSection.setVisibility(View.GONE);
        buttonRemoveFromLibrary.setVisibility(View.GONE);
    }

    private void showAddToLibraryDialog() {
        // Inflar el layout personalizado para el diálogo
        View view = getLayoutInflater().inflate(R.layout.dialog_add_book, null);
        
        // Crear el diálogo con el layout personalizado
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        
        // Referencias a las vistas del diálogo
        TextView textViewDialogTitle = view.findViewById(R.id.textViewDialogTitle);
        RadioGroup radioGroupEstado = view.findViewById(R.id.radioGroupEstado);
        RadioButton radioButtonPorLeer = view.findViewById(R.id.radioButtonPorLeer);
        RadioButton radioButtonLeyendo = view.findViewById(R.id.radioButtonLeyendo);
        RadioButton radioButtonLeido = view.findViewById(R.id.radioButtonLeido);
        RatingBar ratingBarStars = view.findViewById(R.id.ratingBarStars);
        TextView textViewRatingValue = view.findViewById(R.id.textViewRatingValue);
        TextInputEditText editTextReview = view.findViewById(R.id.editTextReview);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);
        Button buttonConfirm = view.findViewById(R.id.buttonConfirm);
        
        // Configurar el título según si el libro ya está en la biblioteca o no
        if (libroYaEnBiblioteca) {
            textViewDialogTitle.setText(R.string.update_in_library);
            buttonConfirm.setText(R.string.update);
            
            // Preseleccionar el estado actual
            if (estadoActual != null) {
                switch (estadoActual) {
                    case UsuarioLibro.ESTADO_POR_LEER:
                        radioButtonPorLeer.setChecked(true);
                        break;
                    case UsuarioLibro.ESTADO_LEYENDO:
                        radioButtonLeyendo.setChecked(true);
                        break;
                    case UsuarioLibro.ESTADO_LEIDO:
                        radioButtonLeido.setChecked(true);
                        break;
                }
            }
            
            // Establecer la calificación actual si existe
            if (calificacionActual != null) {
                // Convertir la calificación de 0-10 a 0-5 estrellas
                float ratingStars = calificacionActual / 2;
                ratingBarStars.setRating(ratingStars);
                textViewRatingValue.setText(formatRating(calificacionActual));
            }
            
            // Establecer la reseña actual si existe
            if (notasActuales != null) {
                editTextReview.setText(notasActuales);
            }
        }
        
        // Configurar el listener para el ratingBar de estrellas
        ratingBarStars.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            // Convertir la calificación de 0-5 estrellas a 0-10
            float ratingValue = rating * 2;
            textViewRatingValue.setText(formatRating(ratingValue));
        });
        
        // Configurar listeners
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        
        buttonConfirm.setOnClickListener(v -> {
            // Determinar qué estado fue seleccionado
            int selectedId = radioGroupEstado.getCheckedRadioButtonId();
            String estadoLectura;
            
            if (selectedId == R.id.radioButtonLeyendo) {
                estadoLectura = UsuarioLibro.ESTADO_LEYENDO;
            } else if (selectedId == R.id.radioButtonLeido) {
                estadoLectura = UsuarioLibro.ESTADO_LEIDO;
            } else {
                // Por defecto, Por leer
                estadoLectura = UsuarioLibro.ESTADO_POR_LEER;
            }
            
            // Obtener calificación (convertir de 0-5 estrellas a 0-10)
            float calificacion = ratingBarStars.getRating() * 2;
            
            // Obtener reseña
            String review = editTextReview.getText().toString().trim();
            
            // Cerrar el diálogo
            dialog.dismiss();
            
            // Añadir o actualizar el libro en la biblioteca
            if (libroYaEnBiblioteca) {
                listener.onUpdateInLibrary(estadoLectura, calificacion, review);
            } else {
                listener.onAddToLibrary(estadoLectura, calificacion, review);
            }
        });
        
        // Mostrar el diálogo
        dialog.show();
    }

    private String formatRating(float rating) {
        // Si el valor es un número entero (parte decimal es cero)
        if (rating == Math.floor(rating)) {
            return String.format("%.0f/10", rating);
        } else {
            return String.format("%.1f/10", rating);
        }
    }
}