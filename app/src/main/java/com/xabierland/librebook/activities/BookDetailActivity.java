package com.xabierland.librebook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
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
import com.xabierland.librebook.utils.ShareUtils;

public class BookDetailActivity extends BaseActivity implements BookActionsFragment.OnBookActionListener{

    private static final String TAG = "BookDetailActivity";
    
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
    private LibroConEstado libroConEstado;
    private int usuarioId = -1;
    private boolean isLoggedIn = false;
    private boolean libroYaEnBiblioteca = false;
    
    // Diálogo global para evitar fugas
    private AlertDialog loadingDialog;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_book_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_share) {
            showShareOptions();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.book_detail);
    }

    // Método para mostrar las opciones de compartir
    private void showShareOptions() {
        // Crear un diálogo con las opciones
        final CharSequence[] items = {
                getString(R.string.share_as_text),
                getString(R.string.share_as_file)
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.share_book_message);
        builder.setItems(items, (dialog, which) -> {
            switch (which) {
                case 0: // Compartir como texto
                    ShareUtils.shareBookAsText(this, libro);
                    break;
                case 1: // Compartir como archivo
                    ShareUtils.shareBookAsFile(this, libro, libroConEstado);
                    break;
            }
        });
        builder.show();
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
        loadingDialog = createLoadingDialog();
        loadingDialog.show();
    
        // Cargar datos del libro
        libroRepository.obtenerLibroPorId(libroId, result -> {
            // Verificar que la actividad no esté destruyéndose
            if (isFinishing() || isDestroyed()) {
                safelyDismissDialog();
                return;
            }
    
            if (result != null) {
                libro = result;
                
                // Si el usuario está logueado, verificar si el libro ya está en su biblioteca
                if (isLoggedIn && usuarioId != -1) {
                    bibliotecaRepository.obtenerLibroDeBiblioteca(usuarioId, libro.getId(), libroConEstado -> {
                        // Verificar que la actividad no esté destruyéndose
                        if (isFinishing() || isDestroyed()) {
                            safelyDismissDialog();
                            return;
                        }
    
                        safelyDismissDialog();
                        
                        if (libroConEstado != null) {
                            // El libro ya está en la biblioteca del usuario
                            libroYaEnBiblioteca = true;
                            this.libroConEstado = libroConEstado; // Guardar referencia
                            runOnUiThread(() -> {
                                // Actualizar fragments con los datos del libro
                                bookInfoFragment.setLibro(libro);
                                // Establecer el número total de páginas antes de mostrar UI
                                bookActionsFragment.setNumPaginasTotal(libro.getNumPaginas());
                                bookActionsFragment.updateUIForExistingBook(libroConEstado);
                            });
                        } else {
                            // El libro no está en la biblioteca del usuario
                            libroYaEnBiblioteca = false;
                            this.libroConEstado = null; // No hay estado
                            runOnUiThread(() -> {
                                // Actualizar fragments con los datos del libro
                                bookInfoFragment.setLibro(libro);
                                // Establecer el número total de páginas antes de mostrar UI
                                bookActionsFragment.setNumPaginasTotal(libro.getNumPaginas());
                                bookActionsFragment.updateUIForNewBook();
                            });
                        }
                    });
                } else {
                    // No hay usuario logueado, simplemente mostrar los detalles del libro
                    safelyDismissDialog();
                    runOnUiThread(() -> {
                        // Actualizar fragments con los datos del libro
                        bookInfoFragment.setLibro(libro);
                        // Establecer el número total de páginas antes de mostrar UI
                        bookActionsFragment.setNumPaginasTotal(libro.getNumPaginas());
                        bookActionsFragment.updateUIForNewBook();
                    });
                }
            } else {
                safelyDismissDialog();
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
    
    // Método para cerrar el diálogo de forma segura
    private void safelyDismissDialog() {
        runOnUiThread(() -> {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error al cerrar diálogo", e);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        safelyDismissDialog();
    }

    // Implementación de OnBookActionListener
    @Override
    public void onAddToLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual) {
        if (libro != null && usuarioId != -1) {
            try {
                // Mostrar diálogo de carga
                AlertDialog actionDialog = createLoadingDialog();
                if (!isFinishing()) {
                    actionDialog.show();
                }
    
                // Añadir el libro a la biblioteca del usuario
                bibliotecaRepository.agregarLibro(usuarioId, libro.getId(), estadoLectura, result -> {
                    if (result > 0) {
                        // Actualizar calificación si se ha proporcionado
                        if (calificacion > 0) {
                            bibliotecaRepository.actualizarCalificacion(usuarioId, libro.getId(), calificacion, null);
                        }
                        
                        // Actualizar reseña si se ha proporcionado
                        if (review != null && !review.isEmpty()) {
                            bibliotecaRepository.guardarNotas(usuarioId, libro.getId(), review, null);
                        }
                        
                        // Actualizar página actual si se ha proporcionado
                        if (paginaActual != null && paginaActual > 0) {
                            bibliotecaRepository.actualizarPaginaActual(usuarioId, libro.getId(), paginaActual, null);
                        }
                        
                        runOnUiThread(() -> {
                            try {
                                if (actionDialog != null && actionDialog.isShowing() && !isFinishing()) {
                                    actionDialog.dismiss();
                                }
                                
                                if (!isFinishing()) {
                                    Toast.makeText(BookDetailActivity.this, R.string.book_added, Toast.LENGTH_SHORT).show();
                                    recreate();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error al cerrar diálogo de acción", e);
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            try {
                                if (actionDialog != null && actionDialog.isShowing() && !isFinishing()) {
                                    actionDialog.dismiss();
                                }
                                
                                if (!isFinishing()) {
                                    Toast.makeText(BookDetailActivity.this, R.string.error_adding_book, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error al cerrar diálogo de acción", e);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error al añadir libro a biblioteca", e);
                Toast.makeText(this, R.string.error_adding_book, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onUpdateInLibrary(String estadoLectura, float calificacion, String review, Integer paginaActual) {
        if (libro != null && usuarioId != -1) {
            try {
                // Mostrar diálogo de carga
                AlertDialog actionDialog = createLoadingDialog();
                if (!isFinishing()) {
                    actionDialog.show();
                }
    
                // Para rastrear las operaciones pendientes
                final boolean[] operationsComplete = {false, false, false};
                
                // Función para comprobar si todas las operaciones han finalizado
                final Runnable checkAllOperationsComplete = () -> {
                    if (operationsComplete[0] && operationsComplete[1] && operationsComplete[2]) {
                        runOnUiThread(() -> {
                            try {
                                if (actionDialog != null && actionDialog.isShowing() && !isFinishing()) {
                                    actionDialog.dismiss();
                                }
                                
                                if (!isFinishing()) {
                                    Toast.makeText(BookDetailActivity.this, R.string.book_status_updated, Toast.LENGTH_SHORT).show();
                                    recreate();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error al cerrar diálogo de actualización", e);
                            }
                        });
                    }
                };
    
                // Primero actualizar el estado de lectura
                bibliotecaRepository.cambiarEstadoLectura(usuarioId, libro.getId(), estadoLectura, result -> {
                    // Actualizar la calificación
                    bibliotecaRepository.actualizarCalificacion(usuarioId, libro.getId(), calificacion, res -> {
                        operationsComplete[0] = true;
                        checkAllOperationsComplete.run();
                    });
                    
                    // Actualizar la reseña
                    bibliotecaRepository.guardarNotas(usuarioId, libro.getId(), review, res -> {
                        operationsComplete[1] = true;
                        checkAllOperationsComplete.run();
                    });
                    
                    // Actualizar página actual si corresponde
                    if (paginaActual != null && paginaActual > 0) {
                        bibliotecaRepository.actualizarPaginaActual(usuarioId, libro.getId(), paginaActual, res -> {
                            operationsComplete[2] = true;
                            checkAllOperationsComplete.run();
                        });
                    } else {
                        operationsComplete[2] = true;
                        checkAllOperationsComplete.run();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error al actualizar libro en biblioteca", e);
                Toast.makeText(this, R.string.error_adding_book, Toast.LENGTH_SHORT).show();
            }
        }
    }    

    @Override
    public void onRemoveFromLibrary() {
        if (libro != null && usuarioId != -1) {
            try {
                // Mostrar diálogo de carga
                AlertDialog actionDialog = createLoadingDialog();
                if (!isFinishing()) {
                    actionDialog.show();
                }
    
                // Eliminar el libro de la biblioteca
                bibliotecaRepository.eliminarLibroDeBiblioteca(usuarioId, libro.getId(), result -> {
                    runOnUiThread(() -> {
                        try {
                            if (actionDialog != null && actionDialog.isShowing() && !isFinishing()) {
                                actionDialog.dismiss();
                            }
                            
                            if (!isFinishing()) {
                                if (result > 0) {
                                    Toast.makeText(BookDetailActivity.this, R.string.book_removed, Toast.LENGTH_SHORT).show();
                                    recreate();
                                } else {
                                    Toast.makeText(BookDetailActivity.this, R.string.error_removing_book, Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al cerrar diálogo de eliminación", e);
                        }
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Error al eliminar libro de biblioteca", e);
                Toast.makeText(this, R.string.error_removing_book, Toast.LENGTH_SHORT).show();
            }
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
                .setMessage(R.string.login_required)
                .setPositiveButton(R.string.login, (dialog, which) -> {
                    // Navegar a la pantalla de login
                    handleNavigationItemSelected(R.id.nav_login);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}