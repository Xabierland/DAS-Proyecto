package com.xabierland.librebook.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;

public class AddBookDialogFragment extends DialogFragment {
    // Argumentos para el diálogo
    private static final String ARG_LIBRO_YA_EN_BIBLIOTECA = "libro_ya_en_biblioteca";
    private static final String ARG_ESTADO_ACTUAL = "estado_actual";
    private static final String ARG_CALIFICACION_ACTUAL = "calificacion_actual";
    private static final String ARG_NOTAS_ACTUALES = "notas_actuales";
    private static final String ARG_PAGINA_ACTUAL = "pagina_actual";
    private static final String ARG_NUM_PAGINAS_TOTAL = "num_paginas_total";
    
    // Interfaz para comunicación con la actividad/fragmento
    public interface OnBookActionListener {
        void onAddToLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual);
        void onUpdateInLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual);
    }
    
    private OnBookActionListener listener;
    
    // Variables de estado
    private boolean libroYaEnBiblioteca;
    private String estadoActual;
    private Float calificacionActual;
    private String notasActuales;
    private Integer paginaActual;
    private int numPaginasTotal;
    
    // Vistas del diálogo
    private TextView textViewDialogTitle;
    private RadioGroup radioGroupEstado;
    private RadioButton radioButtonPorLeer, radioButtonLeyendo, radioButtonLeido;
    private LinearLayout layoutPaginaActual, layoutCalificacion, layoutReview;
    private TextInputEditText editTextPaginaActual, editTextReview;
    private RatingBar ratingBarStars;
    private TextView textViewRatingValue, textViewTotalPaginas;
    private Button buttonCancel, buttonConfirm;
    
    // Constructor estático
    public static AddBookDialogFragment newInstance(int numPaginasTotal, boolean libroYaEnBiblioteca,
                                              String estadoActual, Float calificacionActual,
                                              String notasActuales, Integer paginaActual) {
        AddBookDialogFragment fragment = new AddBookDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_NUM_PAGINAS_TOTAL, numPaginasTotal);
        args.putBoolean(ARG_LIBRO_YA_EN_BIBLIOTECA, libroYaEnBiblioteca);
        if (estadoActual != null) args.putString(ARG_ESTADO_ACTUAL, estadoActual);
        if (calificacionActual != null) args.putFloat(ARG_CALIFICACION_ACTUAL, calificacionActual);
        if (notasActuales != null) args.putString(ARG_NOTAS_ACTUALES, notasActuales);
        if (paginaActual != null) args.putInt(ARG_PAGINA_ACTUAL, paginaActual);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperar argumentos
        if (getArguments() != null) {
            numPaginasTotal = getArguments().getInt(ARG_NUM_PAGINAS_TOTAL);
            libroYaEnBiblioteca = getArguments().getBoolean(ARG_LIBRO_YA_EN_BIBLIOTECA);
            estadoActual = getArguments().getString(ARG_ESTADO_ACTUAL);
            if (getArguments().containsKey(ARG_CALIFICACION_ACTUAL)) {
                calificacionActual = getArguments().getFloat(ARG_CALIFICACION_ACTUAL);
            }
            notasActuales = getArguments().getString(ARG_NOTAS_ACTUALES);
            if (getArguments().containsKey(ARG_PAGINA_ACTUAL)) {
                paginaActual = getArguments().getInt(ARG_PAGINA_ACTUAL);
            }
        }
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Conectar con la interfaz de callback
        try {
            if (getParentFragment() instanceof OnBookActionListener) {
                listener = (OnBookActionListener) getParentFragment();
            } else if (context instanceof OnBookActionListener) {
                listener = (OnBookActionListener) context;
            } else {
                throw new RuntimeException(context + " debe implementar OnBookActionListener");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " debe implementar OnBookActionListener");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_book, null);
        
        // Inicializar vistas
        initViews(view);
        
        // Configurar estado inicial o restaurar estado guardado
        if (savedInstanceState != null) {
            // Restaurar estado desde savedInstanceState
            restoreState(savedInstanceState);
        } else {
            // Configurar estado inicial
            setupInitialState();
        }
        
        // Configurar listeners
        setupListeners();
        
        builder.setView(view);
        setCancelable(false); // Evita que se cierre al tocar fuera
        return builder.create();
    }
    
    private void initViews(View view) {
        textViewDialogTitle = view.findViewById(R.id.textViewDialogTitle);
        radioGroupEstado = view.findViewById(R.id.radioGroupEstado);
        radioButtonPorLeer = view.findViewById(R.id.radioButtonPorLeer);
        radioButtonLeyendo = view.findViewById(R.id.radioButtonLeyendo);
        radioButtonLeido = view.findViewById(R.id.radioButtonLeido);
        
        layoutPaginaActual = view.findViewById(R.id.layoutPaginaActual);
        layoutCalificacion = view.findViewById(R.id.layoutCalificacion);
        layoutReview = view.findViewById(R.id.layoutReview);
        
        textViewTotalPaginas = view.findViewById(R.id.textViewTotalPaginas);
        editTextPaginaActual = view.findViewById(R.id.editTextPaginaActual);
        ratingBarStars = view.findViewById(R.id.ratingBarStars);
        textViewRatingValue = view.findViewById(R.id.textViewRatingValue);
        editTextReview = view.findViewById(R.id.editTextReview);
        
        buttonCancel = view.findViewById(R.id.buttonCancel);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        
        // Actualizar texto con el número total de páginas
        textViewTotalPaginas.setText(String.format(getString(R.string.total_pages), numPaginasTotal));
    }
    
    private void setupInitialState() {
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
            if (calificacionActual != null && UsuarioLibro.ESTADO_LEIDO.equals(estadoActual)) {
                // Convertir la calificación de 0-10 a 0-5 estrellas
                float ratingStars = calificacionActual / 2;
                ratingBarStars.setRating(ratingStars);
                textViewRatingValue.setText(formatRating(calificacionActual));
            }
            
            // Establecer la reseña actual si existe
            if (notasActuales != null && UsuarioLibro.ESTADO_LEIDO.equals(estadoActual)) {
                editTextReview.setText(notasActuales);
            }
        } else {
            textViewDialogTitle.setText(R.string.add_to_library);
            buttonConfirm.setText(R.string.add);
        }
    }
    
    private void setupListeners() {
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
                            editTextPaginaActual.setError(String.format(getString(R.string.page_error_too_large), numPaginasTotal));
                        } else if (paginaValue < 1) {
                            editTextPaginaActual.setError(getString(R.string.page_error_min));
                        } else {
                            editTextPaginaActual.setError(null);
                        }
                    } catch (NumberFormatException e) {
                        editTextPaginaActual.setError(getString(R.string.page_error_invalid));
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
        buttonCancel.setOnClickListener(v -> dismiss());
        
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
                            Toast.makeText(getContext(), getString(R.string.page_error_min), Toast.LENGTH_SHORT).show();
                            return;
                        } else if (paginaValue > numPaginasTotal) {
                            Toast.makeText(getContext(), String.format(getString(R.string.page_error_too_large), numPaginasTotal), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        pagina = paginaValue;
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), getString(R.string.page_error_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            // Invocar el callback adecuado
            if (listener != null) {
                if (libroYaEnBiblioteca) {
                    listener.onUpdateInLibrary(estadoLectura, calificacion, review, pagina);
                } else {
                    listener.onAddToLibrary(estadoLectura, calificacion, review, pagina);
                }
            }
            
            // Cerrar el diálogo
            dismiss();
        });
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guardar el estado actual de todas las vistas
        if (radioGroupEstado != null) {
            outState.putInt("selected_radio_id", radioGroupEstado.getCheckedRadioButtonId());
        }
        if (editTextPaginaActual != null && editTextPaginaActual.getText() != null) {
            outState.putString("pagina_actual_text", editTextPaginaActual.getText().toString());
        }
        if (ratingBarStars != null) {
            outState.putFloat("rating_value", ratingBarStars.getRating());
        }
        if (editTextReview != null && editTextReview.getText() != null) {
            outState.putString("review_text", editTextReview.getText().toString());
        }
        
        // Guardar visibilidad de layouts
        if (layoutPaginaActual != null) {
            outState.putInt("layout_pagina_visibility", layoutPaginaActual.getVisibility());
        }
        if (layoutCalificacion != null) {
            outState.putInt("layout_calificacion_visibility", layoutCalificacion.getVisibility());
        }
        if (layoutReview != null) {
            outState.putInt("layout_review_visibility", layoutReview.getVisibility());
        }
    }
    
    private void restoreState(Bundle savedInstanceState) {
        // Restaurar valores guardados
        if (savedInstanceState.containsKey("selected_radio_id")) {
            int radioId = savedInstanceState.getInt("selected_radio_id");
            if (radioId != -1) {
                radioGroupEstado.check(radioId);
            }
        }
        
        // Restaurar visibilidad de layouts
        if (savedInstanceState.containsKey("layout_pagina_visibility")) {
            layoutPaginaActual.setVisibility(savedInstanceState.getInt("layout_pagina_visibility"));
        }
        if (savedInstanceState.containsKey("layout_calificacion_visibility")) {
            layoutCalificacion.setVisibility(savedInstanceState.getInt("layout_calificacion_visibility"));
        }
        if (savedInstanceState.containsKey("layout_review_visibility")) {
            layoutReview.setVisibility(savedInstanceState.getInt("layout_review_visibility"));
        }
        
        // Restaurar valores de entrada
        if (savedInstanceState.containsKey("pagina_actual_text")) {
            editTextPaginaActual.setText(savedInstanceState.getString("pagina_actual_text"));
        }
        if (savedInstanceState.containsKey("rating_value")) {
            ratingBarStars.setRating(savedInstanceState.getFloat("rating_value"));
            // Actualizar también el texto de calificación
            float ratingValue = ratingBarStars.getRating() * 2;
            textViewRatingValue.setText(formatRating(ratingValue));
        }
        if (savedInstanceState.containsKey("review_text")) {
            editTextReview.setText(savedInstanceState.getString("review_text"));
        }
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