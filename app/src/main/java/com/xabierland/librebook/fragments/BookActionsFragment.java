package com.xabierland.librebook.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
        builder.setCancelable(false); // Evitar que se cierre al tocar fuera
        
        AlertDialog dialog = builder.create();
        
        // Referencias a las vistas del diálogo
        TextView textViewDialogTitle = view.findViewById(R.id.textViewDialogTitle);
        RadioGroup radioGroupEstado = view.findViewById(R.id.radioGroupEstado);
        RadioButton radioButtonPorLeer = view.findViewById(R.id.radioButtonPorLeer);
        RadioButton radioButtonLeyendo = view.findViewById(R.id.radioButtonLeyendo);
        RadioButton radioButtonLeido = view.findViewById(R.id.radioButtonLeido);
        
        // Para la funcionalidad de cambiar campos según estado de lectura
        LinearLayout layoutPaginaActual = view.findViewById(R.id.layoutPaginaActual);
        LinearLayout layoutCalificacion = view.findViewById(R.id.layoutCalificacion);
        LinearLayout layoutReview = view.findViewById(R.id.layoutReview);
        
        TextView textViewTotalPaginas = view.findViewById(R.id.textViewTotalPaginas);
        TextInputEditText editTextPaginaActual = view.findViewById(R.id.editTextPaginaActual);
        RatingBar ratingBarStars = view.findViewById(R.id.ratingBarStars);
        TextView textViewRatingValue = view.findViewById(R.id.textViewRatingValue);
        TextInputEditText editTextReview = view.findViewById(R.id.editTextReview);
        
        Button buttonCancel = view.findViewById(R.id.buttonCancel);
        Button buttonConfirm = view.findViewById(R.id.buttonConfirm);
        
        // Actualizar texto con el número total de páginas
        textViewTotalPaginas.setText(" (de " + numPaginasTotal + " páginas)");
        
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
                        // Mostrar campo de página actual
                        layoutPaginaActual.setVisibility(View.VISIBLE);
                        if (paginaActual != null) {
                            editTextPaginaActual.setText(String.valueOf(paginaActual));
                        }
                        break;
                    case UsuarioLibro.ESTADO_LEIDO:
                        radioButtonLeido.setChecked(true);
                        // Mostrar calificación y reseña
                        layoutCalificacion.setVisibility(View.VISIBLE);
                        layoutReview.setVisibility(View.VISIBLE);
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
        
        // Configurar el listener para el RadioGroup
        radioGroupEstado.setOnCheckedChangeListener((group, checkedId) -> {
            // Ocultar todos los campos adicionales primero
            layoutPaginaActual.setVisibility(View.GONE);
            layoutCalificacion.setVisibility(View.GONE);
            layoutReview.setVisibility(View.GONE);
            
            // Mostrar campos según el estado seleccionado
            if (checkedId == R.id.radioButtonPorLeer) {
                // No mostrar campos adicionales
            } else if (checkedId == R.id.radioButtonLeyendo) {
                // Mostrar solo el campo de página actual
                layoutPaginaActual.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radioButtonLeido) {
                // Mostrar calificación y reseña
                layoutCalificacion.setVisibility(View.VISIBLE);
                layoutReview.setVisibility(View.VISIBLE);
            }
        });
        
        // Validación de entrada para el campo de página actual
        editTextPaginaActual.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No necesario
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No necesario
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        int paginaValue = Integer.parseInt(s.toString());
                        if (paginaValue > numPaginasTotal) {
                            editTextPaginaActual.setError("La página no puede ser mayor que " + numPaginasTotal);
                        } else if (paginaValue < 1) {
                            editTextPaginaActual.setError("La página debe ser mayor o igual a 1");
                        } else {
                            editTextPaginaActual.setError(null);
                        }
                    } catch (NumberFormatException e) {
                        editTextPaginaActual.setError("Introduce un número válido");
                    }
                }
            }
        });
        
        // Configurar el listener para el ratingBar de estrellas
        ratingBarStars.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            // Convertir la calificación de 0-5 estrellas a 0-10
            float ratingValue = rating * 2;
            textViewRatingValue.setText(formatRating(ratingValue));
        });
        
        // Configurar listeners para los botones
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
            
            // Valores por defecto
            float calificacion = 0;
            String review = "";
            Integer pagina = null;
            
            // Obtener valores según el estado
            if (estadoLectura.equals(UsuarioLibro.ESTADO_LEIDO)) {
                // Obtener calificación (convertir de 0-5 estrellas a 0-10)
                calificacion = ratingBarStars.getRating() * 2;
                // Obtener reseña
                review = editTextReview.getText().toString().trim();
            } else if (estadoLectura.equals(UsuarioLibro.ESTADO_LEYENDO)) {
                // Obtener página actual
                String paginaStr = editTextPaginaActual.getText().toString().trim();
                if (!paginaStr.isEmpty()) {
                    try {
                        int paginaValue = Integer.parseInt(paginaStr);
                        
                        // Validar que el número de página esté dentro del rango válido
                        if (paginaValue < 1) {
                            Toast.makeText(getContext(), "La página debe ser mayor o igual a 1", Toast.LENGTH_SHORT).show();
                            return;
                        } else if (paginaValue > numPaginasTotal) {
                            Toast.makeText(getContext(), "La página no puede ser mayor que " + numPaginasTotal, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        pagina = paginaValue;
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Introduce un número válido para la página", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            // Cerrar el diálogo
            dialog.dismiss();
            
            // Añadir o actualizar el libro en la biblioteca
            if (libroYaEnBiblioteca) {
                listener.onUpdateInLibrary(estadoLectura, calificacion, review, pagina);
            } else {
                listener.onAddToLibrary(estadoLectura, calificacion, review, pagina);
            }
        });
        
        // Ejecutar el listener inicial para configurar la visibilidad según el estado actual
        int selectedId = radioGroupEstado.getCheckedRadioButtonId();
        radioGroupEstado.clearCheck(); // Limpiar para forzar el evento
        if (selectedId != -1) {
            ((RadioButton)view.findViewById(selectedId)).setChecked(true);
        } else {
            radioButtonPorLeer.setChecked(true);
        }
        
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