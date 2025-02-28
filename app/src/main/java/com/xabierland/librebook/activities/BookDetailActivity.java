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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
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

    // Repositorios
    private LibroRepository libroRepository;
    private BibliotecaRepository bibliotecaRepository;

    // Datos
    private Libro libro;
    private int usuarioId = -1;
    private boolean isLoggedIn = false;

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
            // Cerrar diálogo de carga
            loadingDialog.dismiss();

            runOnUiThread(() -> {
                if (result != null) {
                    libro = result;
                    displayBookDetails();
                } else {
                    Toast.makeText(BookDetailActivity.this, "No se encontró el libro", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
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
    
    private void showAddToLibraryDialog() {
        // Inflar el layout personalizado para el diálogo
        View view = getLayoutInflater().inflate(R.layout.dialog_add_book, null);
        
        // Crear el diálogo con el layout personalizado
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        
        // Referencias a las vistas del diálogo
        RadioGroup radioGroupEstado = view.findViewById(R.id.radioGroupEstado);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);
        Button buttonConfirm = view.findViewById(R.id.buttonConfirm);
        
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
            
            // Cerrar el diálogo
            dialog.dismiss();
            
            // Añadir el libro a la biblioteca
            addBookToLibrary(estadoLectura);
        });
        
        // Mostrar el diálogo
        dialog.show();
    }

    private void addBookToLibrary(String estadoLectura) {
        if (libro != null && usuarioId != -1) {
            // Mostrar diálogo de carga
            AlertDialog loadingDialog = createLoadingDialog();
            loadingDialog.show();

            // Primero verificar si el libro ya está en la biblioteca del usuario
            bibliotecaRepository.obtenerTodosLosLibrosDeUsuario(usuarioId, libros -> {
                boolean libroYaExistente = false;
                for (int i = 0; i < libros.size(); i++) {
                    if (libros.get(i).getId() == libro.getId()) {
                        libroYaExistente = true;
                        break;
                    }
                }

                if (libroYaExistente) {
                    // El libro ya está en la biblioteca, actualizar su estado
                    bibliotecaRepository.cambiarEstadoLectura(usuarioId, libro.getId(), estadoLectura, result -> {
                        loadingDialog.dismiss();
                        runOnUiThread(() -> {
                            if (result > 0) {
                                Toast.makeText(BookDetailActivity.this, R.string.book_status_updated, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BookDetailActivity.this, R.string.error_adding_book, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                } else {
                    // El libro no está en la biblioteca, añadirlo
                    bibliotecaRepository.agregarLibro(usuarioId, libro.getId(), estadoLectura, result -> {
                        loadingDialog.dismiss();
                        runOnUiThread(() -> {
                            if (result > 0) {
                                Toast.makeText(BookDetailActivity.this, R.string.book_added, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BookDetailActivity.this, R.string.error_adding_book, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
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