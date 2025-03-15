package com.xabierland.librebook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.xabierland.librebook.R;
import com.xabierland.librebook.data.repositories.UsuarioRepository;

public class LoginActivity extends BaseActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private MaterialButton buttonLogin;
    private TextView textViewRegister;
    
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Inicializar el repositorio
        usuarioRepository = new UsuarioRepository(getApplication());
        
        // Inicializar vistas
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        
        // Configurar escuchadores
        setupListeners();
        
        // Verificar si el usuario ya inició sesión
        checkIfUserIsLoggedIn();
    }
    
    private void setupListeners() {
        // Botón de inicio de sesión
        buttonLogin.setOnClickListener(v -> attemptLogin());
        
        // Enlace para ir a la pantalla de registro
        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }
    
    private void attemptLogin() {
        // Restablecer errores
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        
        // Obtener valores
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        
        // Validar campos
        if (email.isEmpty() || password.isEmpty()) {
            if (email.isEmpty()) {
                emailInputLayout.setError(getString(R.string.empty_fields));
            }
            if (password.isEmpty()) {
                passwordInputLayout.setError(getString(R.string.empty_fields));
            }
            return;
        }
        
        // Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.invalid_email));
            return;
        }
        
        // Realizar inicio de sesión
        loginUser(email, password);
    }
    
    private void loginUser(String email, String password) {
        // Mostrar diálogo de carga
        AlertDialog loadingDialog = createLoadingDialog();
        loadingDialog.show();
        
        usuarioRepository.autenticarUsuario(email, password, result -> {
            // Cerrar diálogo de carga
            loadingDialog.dismiss();
            
            runOnUiThread(() -> {
                if (result != null) {
                    // Guardar sesión del usuario
                    saveUserSession(result, email);
                    
                    // Mostrar mensaje de éxito
                    Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                    
                    // Ir a MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);

                    // Volver a la actividad principal
                    finish();
                } else {
                    // Mostrar mensaje de error
                    Toast.makeText(LoginActivity.this, getString(R.string.login_error), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void saveUserSession(int userId, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("userId", userId);
        editor.putString("userEmail", email);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
        
        // Actualizar el menú de navegación para reflejar que el usuario ha iniciado sesión
        updateNavigationMenu();
    }
    
    private void checkIfUserIsLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        
        if (isLoggedIn) {
            // Si el usuario ya inició sesión, ir a MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    private AlertDialog createLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        return builder.create();
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.login);
    }
}