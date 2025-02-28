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
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.repositories.UsuarioRepository;

public class RegisterActivity extends BaseActivity {

    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private MaterialButton buttonRegister;
    private TextView textViewLogin;
    
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Inicializar el repositorio
        usuarioRepository = new UsuarioRepository(getApplication());
        
        // Inicializar vistas
        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        
        // Configurar escuchadores
        setupListeners();
    }
    
    private void setupListeners() {
        // Botón de registro
        buttonRegister.setOnClickListener(v -> attemptRegister());
        
        // Enlace para ir a la pantalla de inicio de sesión
        textViewLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
    
    private void attemptRegister() {
        // Restablecer errores
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        
        // Obtener valores
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        
        // Validar campos vacíos
        boolean hasError = false;
        
        if (name.isEmpty()) {
            nameInputLayout.setError(getString(R.string.empty_fields));
            hasError = true;
        }
        
        if (email.isEmpty()) {
            emailInputLayout.setError(getString(R.string.empty_fields));
            hasError = true;
        }
        
        if (password.isEmpty()) {
            passwordInputLayout.setError(getString(R.string.empty_fields));
            hasError = true;
        }
        
        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.setError(getString(R.string.empty_fields));
            hasError = true;
        }
        
        if (hasError) {
            return;
        }
        
        // Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.invalid_email));
            return;
        }
        
        // Validar longitud de contraseña
        if (password.length() < 6) {
            passwordInputLayout.setError(getString(R.string.password_too_short));
            return;
        }
        
        // Validar que las contraseñas coincidan
        if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.passwords_not_match));
            return;
        }
        
        // Crear cuenta
        registerUser(name, email, password);
    }
    
    private void registerUser(String name, String email, String password) {
        // Mostrar diálogo de carga
        AlertDialog loadingDialog = createLoadingDialog();
        loadingDialog.show();
        
        // Crear objeto Usuario
        Usuario usuario = new Usuario(name, email, password);
        
        // Registrar usuario en la base de datos
        usuarioRepository.registrarUsuario(usuario, result -> {
            // Cerrar diálogo de carga
            loadingDialog.dismiss();
            
            runOnUiThread(() -> {
                if (result > 0) {
                    // Registro exitoso
                    Toast.makeText(RegisterActivity.this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                    
                    // Guardar sesión del usuario
                    saveUserSession(result.intValue(), email);
                    
                    // Volver a la actividad principal
                    finish();
                } else if (result == -1) {
                    // El email ya está en uso
                    emailInputLayout.setError(getString(R.string.email_exists));
                } else {
                    // Otro error
                    Toast.makeText(RegisterActivity.this, getString(R.string.register_error), Toast.LENGTH_SHORT).show();
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
        return getString(R.string.register);
    }
}