package com.xabierland.librebook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;
import com.xabierland.librebook.data.repositories.BibliotecaRepository;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.services.ReadingTimerReceiver;
import com.xabierland.librebook.services.ReadingTimerWorker;
import com.xabierland.librebook.utils.ReadingTimeUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Actividad para gestionar el cronómetro de lectura
 */
public class ReadingTimerActivity extends BaseActivity {

    private static final String TAG = "ReadingTimerActivity";
    
    // Vistas
    private TextView textViewBookTitle;
    private TextView textViewEstimatedTime;
    private TextView textViewTimerValue;
    private Button buttonStartStop;
    private Button buttonReset;
    private Button buttonSave;
    private TextView textViewReadingSpeed;
    
    // Repositorios
    private LibroRepository libroRepository;
    private BibliotecaRepository bibliotecaRepository;
    
    // Datos del libro
    private int libroId = -1;
    private Libro libro;
    private String libroTitulo = "";
    
    // Velocidad de lectura
    private int readingSpeed;
    
    // Handler para actualizar la UI
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerDisplay();
            // Ejecutar cada segundo si el timer está en ejecución
            if (ReadingTimerReceiver.isTimerRunning(ReadingTimerActivity.this)) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_timer);
        
        // Inicializar repositorios
        libroRepository = new LibroRepository(getApplication());
        bibliotecaRepository = new BibliotecaRepository(getApplication());
        
        // Inicializar vistas
        initViews();
        
        // Obtener ID del libro de los extras o del timer en ejecución
        libroId = getIntent().getIntExtra(ReadingTimerWorker.PREF_LIBRO_ID, -1);
        
        if (libroId == -1) {
            // Si no se proporcionó un ID, comprobar si hay un timer en ejecución
            libroId = ReadingTimerReceiver.getCurrentBookId(this);
            if (libroId == -1) {
                // No hay libro, mostrar error y cerrar actividad
                Toast.makeText(this, R.string.error_book_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        
        // Cargar datos del libro
        loadBookData();
        
        // Cargar velocidad de lectura guardada
        readingSpeed = ReadingTimeUtils.getSavedReadingSpeed(this);
        updateReadingSpeedText();
        
        // Configurar botones para cambiar la velocidad de lectura
        findViewById(R.id.buttonDecreaseSpeed).setOnClickListener(v -> decreaseReadingSpeed());
        findViewById(R.id.buttonIncreaseSpeed).setOnClickListener(v -> increaseReadingSpeed());
        
        // Actualizar estado inicial de los botones
        updateButtonStates();
    }
    
    private void initViews() {
        textViewBookTitle = findViewById(R.id.textViewBookTitle);
        textViewEstimatedTime = findViewById(R.id.textViewEstimatedTime);
        textViewTimerValue = findViewById(R.id.textViewTimerValue);
        buttonStartStop = findViewById(R.id.buttonStartStop);
        buttonReset = findViewById(R.id.buttonReset);
        buttonSave = findViewById(R.id.buttonSave);
        textViewReadingSpeed = findViewById(R.id.textViewReadingSpeed);
        
        // Configurar listeners de botones
        buttonStartStop.setOnClickListener(v -> toggleTimer());
        buttonReset.setOnClickListener(v -> resetTimer());
        buttonSave.setOnClickListener(v -> saveReadingProgress());
    }
    
    private void loadBookData() {
        libroRepository.obtenerLibroPorId(libroId, result -> {
            runOnUiThread(() -> {
                if (result != null) {
                    libro = result;
                    libroTitulo = libro.getTitulo();
                    
                    // Actualizar UI con datos del libro
                    textViewBookTitle.setText(libroTitulo);
                    
                    // Calcular tiempo estimado de lectura
                    int estimatedTime = ReadingTimeUtils.calculateReadingTime(
                            libro.getNumPaginas(), readingSpeed);
                    textViewEstimatedTime.setText(getString(R.string.estimated_reading_time, 
                            ReadingTimeUtils.formatReadingTime(estimatedTime)));
                    
                    // Si ya hay un timer en ejecución con otro libro, actualizar UI
                    if (ReadingTimerReceiver.isTimerRunning(this)) {
                        String currentBookTitle = ReadingTimerReceiver.getCurrentBookTitle(this);
                        if (!libroTitulo.equals(currentBookTitle)) {
                            // Mostrar diálogo de confirmación para cambiar de libro
                            showChangeBookConfirmationDialog();
                        }
                    }
                    
                    updateTimerDisplay();
                } else {
                    Toast.makeText(this, R.string.error_book_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }
    
    private void showChangeBookConfirmationDialog() {
        String currentBookTitle = ReadingTimerReceiver.getCurrentBookTitle(this);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_book_title);
        builder.setMessage(getString(R.string.change_book_message, currentBookTitle, libroTitulo));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            // Detener el timer actual
            Intent stopIntent = new Intent(this, ReadingTimerReceiver.class);
            stopIntent.setAction(ReadingTimerReceiver.ACTION_STOP_TIMER);
            sendBroadcast(stopIntent);
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> {
            // Cargar el libro del timer actual
            int currentBookId = ReadingTimerReceiver.getCurrentBookId(this);
            if (currentBookId != -1 && currentBookId != libroId) {
                libroId = currentBookId;
                loadBookData();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    private void toggleTimer() {
        if (ReadingTimerReceiver.isTimerRunning(this)) {
            // Detener el timer
            Intent intent = new Intent(this, ReadingTimerReceiver.class);
            intent.setAction(ReadingTimerReceiver.ACTION_STOP_TIMER);
            sendBroadcast(intent);
            buttonStartStop.setText(R.string.start);
            
            // Detener las actualizaciones de UI
            handler.removeCallbacks(updateTimeRunnable);
        } else {
            // Iniciar el timer
            Intent intent = new Intent(this, ReadingTimerReceiver.class);
            intent.setAction(ReadingTimerReceiver.ACTION_START_TIMER);
            intent.putExtra(ReadingTimerWorker.PREF_LIBRO_ID, libroId);
            intent.putExtra(ReadingTimerWorker.PREF_LIBRO_TITULO, libroTitulo);
            sendBroadcast(intent);
            buttonStartStop.setText(R.string.stop);
            
            // Iniciar actualizaciones de UI
            handler.post(updateTimeRunnable);
        }
        
        updateButtonStates();
    }
    
    private void resetTimer() {
        // Confirmar antes de reiniciar
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_timer)
                .setMessage(R.string.reset_timer_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Enviar acción de reinicio
                    Intent intent = new Intent(this, ReadingTimerReceiver.class);
                    intent.setAction(ReadingTimerReceiver.ACTION_RESET_TIMER);
                    sendBroadcast(intent);
                    
                    // Actualizar UI
                    textViewTimerValue.setText("00:00:00");
                    updateButtonStates();
                    Toast.makeText(this, R.string.timer_reset, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
    
    private void saveReadingProgress() {
        // Mostrar diálogo para seleccionar la página actual
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reading_progress, null);
        TextView textViewTotalPages = dialogView.findViewById(R.id.textViewTotalPages);
        TextView textViewCurrentPage = dialogView.findViewById(R.id.editTextCurrentPage);
        
        // Establecer número total de páginas
        textViewTotalPages.setText(getString(R.string.total_pages_format, libro.getNumPaginas()));
        
        // Obtener la página actual si existe
        int usuarioId = getSharedPreferences("UserSession", MODE_PRIVATE).getInt("userId", -1);
        if (usuarioId != -1) {
            bibliotecaRepository.obtenerLibroDeBiblioteca(usuarioId, libroId, libroConEstado -> {
                if (libroConEstado != null && libroConEstado.getPaginaActual() != null) {
                    runOnUiThread(() -> 
                        textViewCurrentPage.setText(String.valueOf(libroConEstado.getPaginaActual())));
                }
            });
        }
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.save_reading_progress)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String pageStr = textViewCurrentPage.getText().toString();
                    if (!pageStr.isEmpty()) {
                        try {
                            int page = Integer.parseInt(pageStr);
                            if (page > 0 && page <= libro.getNumPaginas()) {
                                // Guardar progreso
                                savePageProgress(page);
                            } else {
                                Toast.makeText(this, R.string.invalid_page_number, Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.invalid_page_number, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void savePageProgress(int page) {
        int usuarioId = getSharedPreferences("UserSession", MODE_PRIVATE).getInt("userId", -1);
        if (usuarioId == -1) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Comprobar si el libro ya está en la biblioteca del usuario
        bibliotecaRepository.obtenerLibroDeBiblioteca(usuarioId, libroId, libroConEstado -> {
            if (libroConEstado != null) {
                // Actualizar la página actual en la biblioteca
                bibliotecaRepository.actualizarPaginaActual(usuarioId, libroId, page, result -> {
                    if (result > 0) {
                        runOnUiThread(() -> {
                            // Si no estaba en estado "leyendo", actualizar también el estado
                            if (!libroConEstado.getEstadoLectura().equals(UsuarioLibro.ESTADO_LEYENDO)) {
                                bibliotecaRepository.cambiarEstadoLectura(
                                        usuarioId, libroId, UsuarioLibro.ESTADO_LEYENDO, null);
                            }
                            
                            Toast.makeText(this, R.string.progress_saved, Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> 
                            Toast.makeText(this, R.string.error_saving_progress, Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                // El libro no está en la biblioteca, añadirlo
                runOnUiThread(() -> {
                    // Mostrar diálogo para añadir libro a biblioteca
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.add_to_library)
                            .setMessage(R.string.add_book_to_save_progress)
                            .setPositiveButton(R.string.add, (dialog, which) -> {
                                // Añadir libro a la biblioteca en estado "leyendo"
                                bibliotecaRepository.agregarLibro(
                                        usuarioId, libroId, UsuarioLibro.ESTADO_LEYENDO, result -> {
                                    if (result > 0) {
                                        // Actualizar la página actual
                                        bibliotecaRepository.actualizarPaginaActual(
                                                usuarioId, libroId, page, updateResult -> {
                                            runOnUiThread(() -> 
                                                Toast.makeText(this, R.string.progress_saved, Toast.LENGTH_SHORT).show());
                                        });
                                    } else {
                                        runOnUiThread(() -> 
                                            Toast.makeText(this, R.string.error_adding_book, Toast.LENGTH_SHORT).show());
                                    }
                                });
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                });
            }
        });
    }
    
    private void updateTimerDisplay() {
        // Obtener tiempo transcurrido
        long elapsedTimeMillis = ReadingTimerReceiver.getElapsedTime(this);
        
        // Formatear tiempo transcurrido
        String formattedTime = formatElapsedTime(elapsedTimeMillis);
        textViewTimerValue.setText(formattedTime);
        
        // Actualizar estados de los botones
        updateButtonStates();
    }
    
    private String formatElapsedTime(long timeMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(timeMillis);
        
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    private void updateButtonStates() {
        boolean isRunning = ReadingTimerReceiver.isTimerRunning(this);
        
        // Actualizar texto del botón de inicio/pausa
        buttonStartStop.setText(isRunning ? R.string.stop : R.string.start);
        
        // Habilitar/deshabilitar botón de reinicio
        long elapsedTime = ReadingTimerReceiver.getElapsedTime(this);
        buttonReset.setEnabled(elapsedTime > 0);
        
        // Cambiar color de fondo del botón de inicio/pausa según el estado
        buttonStartStop.setBackgroundResource(isRunning ? 
                R.drawable.button_stop_background : R.drawable.button_start_background);
    }
    
    private void increaseReadingSpeed() {
        if (readingSpeed < ReadingTimeUtils.READING_SPEED_VERY_FAST) {
            if (readingSpeed < ReadingTimeUtils.READING_SPEED_SLOW) {
                readingSpeed = ReadingTimeUtils.READING_SPEED_SLOW;
            } else if (readingSpeed < ReadingTimeUtils.READING_SPEED_MEDIUM) {
                readingSpeed = ReadingTimeUtils.READING_SPEED_MEDIUM;
            } else if (readingSpeed < ReadingTimeUtils.READING_SPEED_FAST) {
                readingSpeed = ReadingTimeUtils.READING_SPEED_FAST;
            } else {
                readingSpeed = ReadingTimeUtils.READING_SPEED_VERY_FAST;
            }
            
            // Guardar la nueva velocidad
            ReadingTimeUtils.saveReadingSpeed(this, readingSpeed);
            
            // Actualizar UI
            updateReadingSpeedText();
            
            // Recalcular tiempo estimado
            if (libro != null) {
                int estimatedTime = ReadingTimeUtils.calculateReadingTime(
                        libro.getNumPaginas(), readingSpeed);
                textViewEstimatedTime.setText(getString(R.string.estimated_reading_time, 
                        ReadingTimeUtils.formatReadingTime(estimatedTime)));
            }
        }
    }
    
    private void decreaseReadingSpeed() {
        if (readingSpeed > ReadingTimeUtils.READING_SPEED_SLOW) {
            if (readingSpeed > ReadingTimeUtils.READING_SPEED_VERY_FAST) {
                readingSpeed = ReadingTimeUtils.READING_SPEED_FAST;
            } else if (readingSpeed > ReadingTimeUtils.READING_SPEED_FAST) {
                readingSpeed = ReadingTimeUtils.READING_SPEED_MEDIUM;
            } else if (readingSpeed > ReadingTimeUtils.READING_SPEED_MEDIUM) {
                readingSpeed = ReadingTimeUtils.READING_SPEED_SLOW;
            }
            
            // Guardar la nueva velocidad
            ReadingTimeUtils.saveReadingSpeed(this, readingSpeed);
            
            // Actualizar UI
            updateReadingSpeedText();
            
            // Recalcular tiempo estimado
            if (libro != null) {
                int estimatedTime = ReadingTimeUtils.calculateReadingTime(
                        libro.getNumPaginas(), readingSpeed);
                textViewEstimatedTime.setText(getString(R.string.estimated_reading_time, 
                        ReadingTimeUtils.formatReadingTime(estimatedTime)));
            }
        }
    }
    
    private void updateReadingSpeedText() {
        // Obtener descripción de la velocidad
        String speedDescription;
        if (readingSpeed <= ReadingTimeUtils.READING_SPEED_SLOW) {
            speedDescription = getString(R.string.reading_speed_slow);
        } else if (readingSpeed <= ReadingTimeUtils.READING_SPEED_MEDIUM) {
            speedDescription = getString(R.string.reading_speed_medium);
        } else if (readingSpeed <= ReadingTimeUtils.READING_SPEED_FAST) {
            speedDescription = getString(R.string.reading_speed_fast);
        } else {
            speedDescription = getString(R.string.reading_speed_very_fast);
        }
        
        // Actualizar texto
        textViewReadingSpeed.setText(getString(R.string.reading_speed_format, 
                readingSpeed, speedDescription));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Actualizar display del timer
        updateTimerDisplay();
        
        // Si el timer está en ejecución, iniciar actualizaciones periódicas
        if (ReadingTimerReceiver.isTimerRunning(this)) {
            handler.post(updateTimeRunnable);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Detener actualizaciones periódicas
        handler.removeCallbacks(updateTimeRunnable);
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
        return getString(R.string.reading_timer);
    }
}