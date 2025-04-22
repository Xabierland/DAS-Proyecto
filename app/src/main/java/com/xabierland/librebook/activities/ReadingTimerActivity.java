package com.xabierland.librebook.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.services.ReadingTimerService;

import java.util.concurrent.TimeUnit;

public class ReadingTimerActivity extends BaseActivity implements ReadingTimerService.TimerListener {

    private static final String TAG = "ReadingTimerActivity";

    private TextView textViewTimer;
    private Button buttonStartStop;
    private Button buttonReset;
    
    private ReadingTimerService timerService;
    private boolean isServiceBound = false;
    
    // Conexión con el servicio
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ReadingTimerService.LocalBinder binder = (ReadingTimerService.LocalBinder) service;
            timerService = binder.getService();
            timerService.setTimerListener(ReadingTimerActivity.this);
            isServiceBound = true;
            
            // Actualizar UI con el estado actual del temporizador
            updateUIWithServiceState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            timerService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_timer);
        
        // Inicializar vistas
        textViewTimer = findViewById(R.id.textViewTimer);
        buttonStartStop = findViewById(R.id.buttonStartStop);
        buttonReset = findViewById(R.id.buttonReset);
        
        // Configurar listeners
        buttonStartStop.setOnClickListener(v -> {
            if (isServiceBound) {
                if (timerService.isTimerRunning()) {
                    stopTimer();
                } else {
                    startTimer();
                }
            }
        });
        
        buttonReset.setOnClickListener(v -> {
            if (isServiceBound) {
                resetTimer();
            }
        });
        
        // Iniciar y vincular con el servicio
        Intent serviceIntent = new Intent(this, ReadingTimerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if (!isServiceBound) {
            Intent serviceIntent = new Intent(this, ReadingTimerService.class);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // No desvinculamos el servicio aquí para mantenerlo activo
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            timerService.setTimerListener(null);
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
    
    private void updateUIWithServiceState() {
        if (timerService != null) {
            // Actualizar el texto del temporizador
            long time = timerService.getElapsedTime();
            updateTimerText(time);
            
            // Actualizar el texto del botón según el estado
            updateButtonText(timerService.isTimerRunning());
        }
    }
    
    private void startTimer() {
        Intent intent = new Intent(this, ReadingTimerService.class);
        intent.setAction(ReadingTimerService.ACTION_START);
        startService(intent);
    }
    
    private void stopTimer() {
        Intent intent = new Intent(this, ReadingTimerService.class);
        intent.setAction(ReadingTimerService.ACTION_STOP);
        startService(intent);
    }
    
    private void resetTimer() {
        Intent intent = new Intent(this, ReadingTimerService.class);
        intent.setAction(ReadingTimerService.ACTION_RESET);
        startService(intent);
    }
    
    private void updateTimerText(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        textViewTimer.setText(timeString);
    }
    
    private void updateButtonText(boolean isRunning) {
        buttonStartStop.setText(isRunning ? R.string.stop : R.string.start);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Aseguramos que el Intent interno de la Activity se actualice
        setIntent(intent);

        if (timerService != null) {
            // Ya estábamos binded: volvemos a poner el listener y refrescamos UI
            timerService.setTimerListener(this);
            updateUIWithServiceState();
        } else {
            // Si por algún motivo aún no está binded, lo vinculamos ahora
            Intent serviceIntent = new Intent(this, ReadingTimerService.class);
            bindService(
                    serviceIntent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
            );
        }
    }


    // Implementación de la interfaz TimerListener
    @Override
    public void onTimerTick(long elapsedTime) {
        runOnUiThread(() -> updateTimerText(elapsedTime));
    }
    
    @Override
    public void onTimerStateChanged(boolean isRunning) {
        runOnUiThread(() -> updateButtonText(isRunning));
    }
    
    @Override
    protected String getActivityTitle() {
        return getString(R.string.reading_timer);
    }
}