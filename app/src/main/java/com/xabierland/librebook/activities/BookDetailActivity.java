package com.xabierland.librebook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.data.repositories.BibliotecaRepository;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.fragments.BookActionsFragment;
import com.xabierland.librebook.fragments.BookInfoFragment;

public class BookDetailActivity extends BaseActivity implements BookActionsFragment.OnBookActionListener {

    // Constantes para los extras
    public static final String EXTRA_LIBRO_ID = "libro_id";

    // Fragments
    private BookInfoFragment bookInfoFragment;
    private BookActionsFragment bookActionsFragment;

    // Repositorios
    private LibroRepository libroRepository;
    private BibliotecaRepository bibliotecaRepository;

    // Datos
    private Libro libro;
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

        // Inicializar fragments si no existen
        if (savedInstanceState == null) {
            bookInfoFragment = BookInfoFragment.newInstance();
            bookActionsFragment = BookActionsFragment.newInstance();

            // Añadir fragments a sus contenedores
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentInfoContainer, bookInfoFragment)
                .replace(R.id.fragmentActionsContainer, bookActionsFragment)
                .commit();
        } else {
            // Recuperar fragments existentes
            bookInfoFragment = (BookInfoFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentInfoContainer);
            bookActionsFragment = (BookActionsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentActionsContainer);
        }

        // Verificar si hay un usuario logueado
        checkUserSession();

        // Cargar datos del libro
        loadBookData();
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
                            
                            runOnUiThread(() -> {
                                // Actualizar fragments con los datos del libro
                                bookInfoFragment.setLibro(libro);
                                bookActionsFragment.updateUIForExistingBook(libroConEstado);
                            });
                        } else {
                            // El libro no está en la biblioteca del usuario
                            libroYaEnBiblioteca = false;
                            runOnUiThread(() -> {
                                // Actualizar fragments con los datos del libro
                                bookInfoFragment.setLibro(libro);
                                bookActionsFragment.updateUIForNewBook();
                            });
                        }
                    });
                } else {
                    // No hay usuario logueado, simplemente mostrar los detalles del libro
                    loadingDialog.dismiss();
                    runOnUiThread(() -> {
                        // Actualizar fragments con los datos del libro
                        bookInfoFragment.setLibro(libro);
                        bookActionsFragment.updateUIForNewBook();
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

    // Implementación de OnBookActionListener

    @Override
    public void onAddToLibrary(String estadoLectura, float calificacion, String review) {
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

    @Override
    public void onUpdateInLibrary(String estadoLectura, float calificacion, String review) {
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
    
    @Override
    public void onRemoveFromLibrary() {
        if (libro != null && usuarioId != -1) {
            // Mostrar diálogo de carga
            AlertDialog loadingDialog = createLoadingDialog();
            loadingDialog.show();

            // Eliminar el libro de la biblioteca
            bibliotecaRepository.eliminarLibroDeBiblioteca(usuarioId, libro.getId(), result -> {
                loadingDialog.dismiss();
                
                runOnUiThread(() -> {
                    if (result > 0) {
                        Toast.makeText(BookDetailActivity.this, R.string.book_removed, Toast.LENGTH_SHORT).show();
                        recreate();
                    } else {
                        Toast.makeText(BookDetailActivity.this, R.string.error_removing_book, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    @Override
    public boolean isUserLoggedIn() {
        return isLoggedIn;
    }

    @Override
    public void showLoginRequiredDialog() {
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
}