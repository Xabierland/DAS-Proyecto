package com.xabierland.librebook;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.util.Collections;
import java.util.List;

/**
 * Inicializador personalizado para WorkManager
 * Permite configurar WorkManager para toda la aplicación
 */
public class LibreBookWorkManagerInitializer implements Initializer<WorkManager> {

    private static final String TAG = "WorkManagerInit";

    @NonNull
    @Override
    public WorkManager create(@NonNull Context context) {
        // Configuración personalizada de WorkManager
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
                
        // Inicializar WorkManager con la configuración personalizada
        WorkManager.initialize(context, config);
        Log.d(TAG, "WorkManager inicializado correctamente");
        
        return WorkManager.getInstance(context);
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        // Este inicializador no tiene dependencias
        return Collections.emptyList();
    }
}