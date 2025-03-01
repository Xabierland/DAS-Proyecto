package com.xabierland.librebook.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.data.repositories.BibliotecaRepository;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.utils.ImageLoader;

public class BookDetailActivity extends BaseActivity {

    // Constantes para los extras
    public static final String EXTRA_LIBRO_ID = "libro_id";

    // Vistas
    private ImageView imageViewPortada;
    private TextView textViewTitulo;
    private TextView textViewAutor;
    private TextView textViewGenero;
    private TextView textViewAnio;
    private TextView textViewEditorial;
    private TextView textViewIsbn;
    private TextView textViewPaginas;
    private TextView textViewDescripcion;
    private MaterialButton buttonAddToLibrary;
    
    // Vistas para la sección de calificación y reseña
    private View reviewSection;
    private TextView textViewRating;
    private TextView textViewReview;

    // Repositorios
    private LibroRepository libroRepository;
    private BibliotecaRepository bibliotecaRepository;

    // Datos
    private Libro libro;
    private UsuarioLibro usuarioLibro;
    private int usuarioId = -1;
    private boolean isLoggedIn = false;
    private boolean libroYaEnBiblioteca = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        // Inicializar repositorios
        libroRepository = new LibroRepository(getApplication());
        bibliotecaRepository = new BibliotecaRepository(getApplication());

        // Inicializar vistas
        initViews();

        // Verificar si hay un usuario logueado
        checkUserSession();

        // Cargar datos del libro
        loadBookData();

        // Configurar listeners de botones
        setupButtonListeners();
    }

    private void initViews() {
        imageViewPortada = findViewById(R.id.imageViewPortada);
        textViewTitulo = findViewById(R.id.textViewTitulo);
        textViewAutor = findViewById(R.id.textViewAutor);
        textViewGenero = findViewById(R.id.textViewGenero);
        textViewAnio = findViewById(R.id.textViewAnio);
        textViewEditorial = findViewById(R.id.textViewEditorial);
        textViewIsbn = findViewById(R.id.textViewIsbn);
        textViewPaginas = findViewById(R.id.textViewPaginas);
        textViewDescripcion = findViewById(R.id.textViewDescripcion);
        buttonAddToLibrary = findViewById(R.id.buttonAddToLibrary);
        
        // Inicializar sección de calificación y reseña
        reviewSection = findViewById(R.id.reviewSection);
        textViewRating = findViewById(R.id.textViewRating);
        textViewReview = findViewById(R.id.textViewReview);
    }

    private void checkUserSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        usuarioId = sharedPreferences.getInt("userId", -1);
    }

    private void loadBookData() {
        // Obtener el ID del libro desde los extras
        int libroId = getIntent().getIntExtra(EXTRA_LIBRO_ID, -1);

        if (libroId == -1) {
            // Si no hay ID válido, cerrar la actividad
            Toast.makeText(this, "Error al cargar el libro", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Mostrar diálogo de carga
        AlertDialog loadingDialog = createLoadingDialog();
        loadingDialog.show();

        // Cargar datos del libro
        libroRepository.obtenerLibroPorId(libroId, result -> {
            if (result != null) {
                libro = result;
                
                // Si el usuario está logueado, verificar si el libro ya está en su biblioteca
                if (isLoggedIn && usuarioId != -1) {
                    bibliotecaRepository.obtenerLibroDeBiblioteca(usuarioId, libro.getId(), libroConEstado -> {
                        loadingDialog.dismiss();
                        
                        if (libroConEstado != null) {
                            // El libro ya está en la biblioteca del usuario
                            libroYaEnBiblioteca = true;
                            usuarioLibro = new UsuarioLibro(usuarioId, libro.getId(), libroConEstado.getEstadoLectura());
                            usuarioLibro.setCalificacion(libroConEstado.getCalificacion());
                            usuarioLibro.setNotas(libroConEstado.getNotas());
                            
                            runOnUiThread(() -> {
                                displayBookDetails();
                                updateUIForExistingBook(libroConEstado);
                            });
                        } else {
                            // El libro no está en la biblioteca del usuario
                            libroYaEnBiblioteca = false;
                            runOnUiThread(() -> {
                                displayBookDetails();
                                updateUIForNewBook();
                            });
                        }
                    });
                } else {
                    // No hay usuario logueado, simplemente mostrar los detalles del libro
                    loadingDialog.dismiss();
                    runOnUiThread(() -> {
                        displayBookDetails();
                        updateUIForNewBook();
                    });
                }
            } else {
                loadingDialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(BookDetailActivity.this, "No se encontró el libro", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private void displayBookDetails() {
        // Mostrar datos del libro en las vistas
        textViewTitulo.setText(libro.getTitulo());
        textViewAutor.setText(libro.getAutor());
        
        if (libro.getGenero() != null && !libro.getGenero().isEmpty()) {
            textViewGenero.setText(libro.getGenero());
            textViewGenero.setVisibility(View.VISIBLE);
        } else {
            textViewGenero.setVisibility(View.GONE);
        }
        
        textViewAnio.setText(String.format("Año: %d", libro.getAnioPublicacion()));
        
        if (libro.getEditorial() != null && !libro.getEditorial().isEmpty()) {
            textViewEditorial.setText(String.format("Editorial: %s", libro.getEditorial()));
        } else {
            textViewEditorial.setVisibility(View.GONE);
        }
        
        if (libro.getIsbn() != null && !libro.getIsbn().isEmpty()) {
            textViewIsbn.setText(String.format("ISBN: %s", libro.getIsbn()));
        } else {
            textViewIsbn.setVisibility(View.GONE);
        }
        
        textViewPaginas.setText(String.format("Páginas: %d", libro.getNumPaginas()));
        
        if (libro.getDescripcion() != null && !libro.getDescripcion().isEmpty()) {
            textViewDescripcion.setText(libro.getDescripcion());
        } else {
            textViewDescripcion.setText("No hay descripción disponible.");
        }
        
        // Cargar la imagen de la portada
        if (libro.getPortadaUrl() != null && !libro.getPortadaUrl().isEmpty()) {
            imageViewPortada.setVisibility(View.VISIBLE);
            // Verificar permiso de Internet
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                ImageLoader.loadImage(libro.getPortadaUrl(), imageViewPortada);
            }
        } else {
            imageViewPortada.setVisibility(View.INVISIBLE);
        }
    }

    private void updateUIForExistingBook(LibroConEstado libroConEstado) {
        // Cambiar el texto del botón según el estado actual
        String estadoActual = "";
        switch (libroConEstado.getEstadoLectura()) {
            case UsuarioLibro.ESTADO_POR_LEER:
                estadoActual = getString(R.string.status_to_read);
                break;
            case UsuarioLibro.ESTADO_LEYENDO:
                estadoActual = getString(R.string.status_reading);
                break;
            case UsuarioLibro.ESTADO_LEIDO:
                estadoActual = getString(R.string.status_read);
                break;
        }
        
        buttonAddToLibrary.setText(getString(R.string.update_in_library) + " (" + estadoActual + ")");
        
        // Mostrar sección de calificación y reseña si existen
        if (libroConEstado.getCalificacion() != null || (libroConEstado.getNotas() != null && !libroConEstado.getNotas().isEmpty())) {
            reviewSection.setVisibility(View.VISIBLE);
            
            // Mostrar calificación si existe
            if (libroConEstado.getCalificacion() != null) {
                // Obtener referencias a las vistas de calificación
                RatingBar ratingBarDisplay = findViewById(R.id.ratingBarDisplay);
                TextView textViewRating = findViewById(R.id.textViewRating);
                
                // Configurar el RatingBar (convertir de 0-10 a 0-5 estrellas)
                float ratingValue = libroConEstado.getCalificacion();
                float ratingStars = ratingValue / 2;
                ratingBarDisplay.setRating(ratingStars);
                textViewRating.setText(formatRating(ratingValue));
                textViewRating.setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.ratingBarDisplay).setVisibility(View.GONE);
                findViewById(R.id.textViewRating).setVisibility(View.GONE);
            }
            
            // Mostrar reseña si existe
            TextView textViewReview = findViewById(R.id.textViewReview);
            if (libroConEstado.getNotas() != null && !libroConEstado.getNotas().isEmpty()) {
                textViewReview.setText(libroConEstado.getNotas());
                textViewReview.setVisibility(View.VISIBLE);
            } else {
                textViewReview.setVisibility(View.GONE);
                findViewById(R.id.textViewReview).setVisibility(View.GONE);
            }
        } else {
            reviewSection.setVisibility(View.GONE);
        }
    }

    private void updateUIForNewBook() {
        buttonAddToLibrary.setText(R.string.add_to_library);
        reviewSection.setVisibility(View.GONE);
    }

    private void setupButtonListeners() {
        // Botón para añadir a biblioteca
        buttonAddToLibrary.setOnClickListener(v -> {
            if (isLoggedIn) {
                showAddToLibraryDialog();
            } else {
                showLoginRequiredDialog();
            }
        });
    }
    
    /**
     * Formatea la calificación para mostrar solo decimales cuando es necesario.
     * Ejemplo: 10.0 se muestra como "10/10", mientras que 8.5 se muestra como "8.5/10"
     */
    private String formatRating(float rating) {
        // Si el valor es un número entero (parte decimal es cero)
        if (rating == Math.floor(rating)) {
            return String.format("%.0f/10", rating);
        } else {
            return String.format("%.1f/10", rating);
        }
    }
    
    private void showAddToLibraryDialog() {
        // Inflar el layout personalizado para el diálogo
        View view = getLayoutInflater().inflate(R.layout.dialog_add_book, null);
        
        // Crear el diálogo con el layout personalizado
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            if (usuarioLibro != null) {
                switch (usuarioLibro.getEstadoLectura()) {
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
                
                // Establecer la calificación actual si existe
                if (usuarioLibro.getCalificacion() != null) {
                    // Convertir la calificación de 0-10 a 0-5 estrellas
                    float ratingStars = usuarioLibro.getCalificacion() / 2;
                    ratingBarStars.setRating(ratingStars);
                    textViewRatingValue.setText(formatRating(usuarioLibro.getCalificacion()));
                }
                
                // Establecer la reseña actual si existe
                if (usuarioLibro.getNotas() != null) {
                    editTextReview.setText(usuarioLibro.getNotas());
                }
            }
        }
        
        // Configurar el listener para el ratingBar de estrellas
        ratingBarStars.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                // Convertir la calificación de 0-5 estrellas a 0-10
                float ratingValue = rating * 2;
                textViewRatingValue.setText(formatRating(ratingValue));
            }
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
                updateBookInLibrary(estadoLectura, calificacion, review);
            } else {
                addBookToLibrary(estadoLectura, calificacion, review);
            }
        });
        
        // Mostrar el diálogo
        dialog.show();
    }

    private void addBookToLibrary(String estadoLectura, float calificacion, String review) {
        if (libro != null && usuarioId != -1) {
            // Mostrar diálogo de carga
            AlertDialog loadingDialog = createLoadingDialog();
            loadingDialog.show();

            // Añadir el libro a la biblioteca del usuario
            bibliotecaRepository.agregarLibro(usuarioId, libro.getId(), estadoLectura, result -> {
                if (result > 0) {
                    // Actualizar calificación si se ha proporcionado
                    if (calificacion > 0) {
                        bibliotecaRepository.actualizarCalificacion(usuarioId, libro.getId(), calificacion, null);
                    }
                    
                    // Actualizar reseña si se ha proporcionado
                    if (!review.isEmpty()) {
                        bibliotecaRepository.guardarNotas(usuarioId, libro.getId(), review, null);
                    }
                    
                    loadingDialog.dismiss();
                    
                    // Recrear la actividad para actualizar la UI
                    runOnUiThread(() -> {
                        Toast.makeText(BookDetailActivity.this, R.string.book_added, Toast.LENGTH_SHORT).show();
                        recreate();
                    });
                } else {
                    loadingDialog.dismiss();
                    
                    runOnUiThread(() -> {
                        Toast.makeText(BookDetailActivity.this, R.string.error_adding_book, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void updateBookInLibrary(String estadoLectura, float calificacion, String review) {
        if (libro != null && usuarioId != -1) {
            // Mostrar diálogo de carga
            AlertDialog loadingDialog = createLoadingDialog();
            loadingDialog.show();

            // Primero actualizar el estado de lectura
            bibliotecaRepository.cambiarEstadoLectura(usuarioId, libro.getId(), estadoLectura, result -> {
                // Luego actualizar la calificación
                bibliotecaRepository.actualizarCalificacion(usuarioId, libro.getId(), calificacion, null);
                
                // Finalmente actualizar la reseña
                bibliotecaRepository.guardarNotas(usuarioId, libro.getId(), review, updateResult -> {
                    loadingDialog.dismiss();
                    
                    runOnUiThread(() -> {
                        Toast.makeText(BookDetailActivity.this, R.string.book_status_updated, Toast.LENGTH_SHORT).show();
                        recreate();
                    });
                });
            });
        }
    }

    private void showLoginRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.login_required)
                .setMessage("Para añadir libros a tu biblioteca, necesitas iniciar sesión.")
                .setPositiveButton("Iniciar sesión", (dialog, which) -> {
                    // Navegar a la pantalla de login
                    handleNavigationItemSelected(R.id.nav_login);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private AlertDialog createLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        return builder.create();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.book_detail);
    }
}