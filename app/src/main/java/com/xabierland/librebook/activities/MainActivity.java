package com.xabierland.librebook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.xabierland.librebook.R;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.repositories.UsuarioRepository;

public class MainActivity extends BaseActivity {
    
    private UsuarioRepository usuarioRepository;
    private TextView textViewWelcome;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar repositorio
        usuarioRepository = new UsuarioRepository(getApplication());
        
        // Comprobar si venimos de un cambio de tema
        if (getIntent().getBooleanExtra("THEME_CHANGED", false)) {
            // No hacemos nada especial, el tema ya se ha aplicado en BaseActivity
        }
        
        // Buscar vista de bienvenida (asumimos que existe o se añadirá)
        textViewWelcome = findViewById(R.id.textViewWelcome);
        
        // Verificar si hay usuario logueado y actualizar la interfaz en consecuencia
        checkUserSessionAndUpdateUI();
    }
    
    private void checkUserSessionAndUpdateUI() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        
        if (isLoggedIn) {
            // Si hay sesión, cargar datos del usuario
            loadCurrentUserData();
        } else {
            // Si no hay sesión, mostrar mensaje de bienvenida genérico
            updateUIForGuest();
        }
    }
    
    private void updateUIForGuest() {
        if (textViewWelcome != null) {
            textViewWelcome.setText(R.string.welcome_guest);
        }
        
        // Aquí puedes adaptar otros elementos de la UI para usuarios no autenticados
        // Por ejemplo, mostrar secciones limitadas, o sugerencias para que se registren
    }
    
    private void loadCurrentUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = sharedPreferences.getInt("userId", -1);
        
        if (userId != -1) {
            usuarioRepository.obtenerUsuarioPorId(userId, usuario -> {
                if (usuario != null) {
                    runOnUiThread(() -> {
                        // Actualizar la interfaz con los datos del usuario
                        updateUserInterface(usuario);
                    });
                } else {
                    // Si no se encuentra el usuario (incoherencia en datos), limpiar sesión
                    runOnUiThread(this::clearUserSession);
                }
            });
        }
    }
    
    private void updateUserInterface(Usuario usuario) {
        if (textViewWelcome != null) {
            textViewWelcome.setText(getString(R.string.welcome_user, usuario.getNombre()));
        }
        
        // Aquí puedes actualizar más elementos de la UI con los datos del usuario
    }
    
    private void clearUserSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        // Actualizar la interfaz para usuario invitado
        updateUIForGuest();
        
        // Actualizar el menú de navegación
        updateNavigationMenu();
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.app_name);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Verificar sesión cada vez que la actividad se reanuda y actualizar UI
        checkUserSessionAndUpdateUI();
    }
}