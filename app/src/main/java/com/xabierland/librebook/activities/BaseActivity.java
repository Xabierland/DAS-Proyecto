package com.xabierland.librebook.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.xabierland.librebook.R;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle toggle;
    protected Toolbar toolbar;
    protected NavigationView navigationView;
    protected SharedPreferences sharedPreferences; // Add this line to declare the SharedPreferences

    // Constantes para idiomas
    public static final int LANGUAGE_SYSTEM = 0;
    public static final int LANGUAGE_ENGLISH = 1;
    public static final int LANGUAGE_SPANISH = 2;
    public static final int LANGUAGE_BASQUE = 3;

    // Constantes para el tema
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplicar el tema guardado
        loadLocale();
        applyTheme();
        super.onCreate(savedInstanceState);
        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        // Esto hace que todas las actividades de la app tengan el idioma seleccionado
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // Esto hace que el toolbar y el menu lateral se añadan a todas las actividades de la app
        setupToolbar();
        setupDrawer();
    }

    //! ======================== Logica del Toolbar ========================

    protected void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(getActivityTitle());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            if (this instanceof SearchActivity) {
                return true;
            }
            // Abre la actividad de búsqueda
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //! ======================== Logica del Menu ========================
    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (drawerLayout != null && toolbar != null) {
            toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            if (navigationView != null) {
                // Verificar el estado de inicio de sesión para mostrar/ocultar ítems del menú
                updateNavigationMenu();

                navigationView.setNavigationItemSelectedListener(item -> {
                    handleNavigationItemSelected(item.getItemId());
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                });
            }
        }
    }

    protected void handleNavigationItemSelected(int itemId) {
        // Si estamos ya en la actividad seleccionada, no hacemos nada
        if ((this instanceof MainActivity && itemId == R.id.nav_home) ||
                (this instanceof SearchActivity && itemId == R.id.nav_search) ||
                (this instanceof ProfileActivity && itemId == R.id.nav_profile) ||
                (this instanceof SearchActivity && itemId == R.id.action_search) ||
                (this instanceof LoginActivity && itemId == R.id.nav_login) ||
                (this instanceof RegisterActivity && itemId == R.id.nav_register) ||
                (this instanceof SettingsActivity && itemId == R.id.nav_settings))
                {
            return;
        }

        // Manejar la navegación
        Intent intent = null;

        if (itemId == R.id.nav_home) {
            intent = new Intent(this, MainActivity.class);
            // Limpiar el stack de activities si vamos al home
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        else if (itemId == R.id.nav_search) {
            intent = new Intent(this, SearchActivity.class);
        }
        else if (itemId == R.id.nav_profile) {
            intent = new Intent(this, ProfileActivity.class);
        }
        else if (itemId == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
        }
        else if (itemId == R.id.nav_login) {
            intent = new Intent(this, LoginActivity.class);
        }
        else if (itemId == R.id.nav_register) {
            intent = new Intent(this, RegisterActivity.class);
        }
        else if (itemId == R.id.nav_logout) {
            // Mostrar diálogo de confirmación de cierre de sesión
            showLogoutConfirmationDialog();
            return;
        }
        if (intent != null) {
            startActivity(intent);
            // Si es la home, cerrar la activity actual
            if (itemId == R.id.nav_home) {
                finish();
            }
        }
    }

    // Método para mostrar u ocultar ítems del menú basado en el estado de autenticación
    protected void updateNavigationMenu() {
        if (navigationView != null) {
            Menu menu = navigationView.getMenu();

            // Verificar si el usuario ha iniciado sesión
            boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

            // Mostrar/ocultar opciones según el estado de inicio de sesión
            menu.findItem(R.id.nav_profile).setVisible(isLoggedIn);
            menu.findItem(R.id.nav_login).setVisible(!isLoggedIn);
            menu.findItem(R.id.nav_register).setVisible(!isLoggedIn);
            menu.findItem(R.id.nav_logout).setVisible(isLoggedIn);
        }
    }

    //! ======================== Logica de cierre de sesión ========================
    // Método para mostrar diálogo de confirmación de cierre de sesión
    protected void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.logout);
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?");
        builder.setPositiveButton("Sí", (dialog, which) -> {
            logoutUser();
        });
        builder.setNegativeButton("No", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Método para cerrar sesión
    protected void logoutUser() {
        // Eliminar información de sesión
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Mostrar mensaje
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

        // Actualizar el menú de navegación
        updateNavigationMenu();

        // Si estamos en MainActivity, recrearla para actualizar la interfaz
        if (this instanceof MainActivity) {
            recreate();
        } else {
            // Si estamos en otra actividad, volvemos a MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    //! ======================== Logica de idiomas ========================
    protected void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int languageMode = prefs.getInt("language_mode", LANGUAGE_SYSTEM);
        applyLanguage(languageMode, false);
    }

    protected void applyLanguage(int languageMode, boolean recreateActivity) {
        // Obtener el código del idioma basado en el modo
        String languageCode;
        
        switch (languageMode) {
            case LANGUAGE_ENGLISH:
                languageCode = "en";
                break;
            case LANGUAGE_SPANISH:
                languageCode = "es";
                break;
            case LANGUAGE_BASQUE:
                languageCode = "eu";
                break;
            case LANGUAGE_SYSTEM:
            default:
                languageCode = Locale.getDefault().getLanguage();
                break;
        }
        
        // Aplicar el idioma
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        
        // Guardar la configuración
        if (languageMode != LANGUAGE_SYSTEM) {
            SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
            editor.putInt("language_mode", languageMode);
            editor.apply();
        }
        
        // Recrear la actividad si es necesario
        if (recreateActivity) {
            recreate();
        }
    }

    protected void setLanguageMode(int languageMode) {
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putInt("language_mode", languageMode);
        editor.apply();

        // Recrear todas las actividades para aplicar el nuevo idioma
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("LANGUAGE_CHANGED", true);
        startActivity(intent);
        finish();
    }

    protected void showLanguageDialog() {
        final String[] languages = {
                getString(R.string.system_default),
                "English",
                "Español",
                "Euskera"
        };

        // Obtener el idioma actual
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int currentLanguage = prefs.getInt("language_mode", LANGUAGE_SYSTEM);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language);
        builder.setSingleChoiceItems(languages, currentLanguage, (dialog, which) -> {
            if (which != currentLanguage) {
                setLanguageMode(which);
            }
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //! ======================== Lógica de temas ========================
    protected void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", THEME_SYSTEM);

        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                setTheme(R.style.Theme_LibreBook_Light);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                setTheme(R.style.Theme_LibreBook_Dark);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                setTheme(R.style.Theme_LibreBook);
                break;
        }
    }

    protected void setThemeMode(int themeMode) {
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putInt("theme_mode", themeMode);
        editor.apply();

        // Recrear todas las actividades para aplicar el nuevo tema
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("THEME_CHANGED", true);
        startActivity(intent);
        finish();
    }

    protected void showThemeDialog() {
        final String[] themes = {
                getString(R.string.system_default),
                getString(R.string.light_theme),
                getString(R.string.dark_theme)
        };

        // Obtener el tema actual
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int currentTheme = prefs.getInt("theme_mode", THEME_SYSTEM);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_theme);
        builder.setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
            if (which != currentTheme) {
                setThemeMode(which);
            }
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //! ================= Lógica de las actividades =================
    protected abstract String getActivityTitle();

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar menú en cada reanudación para asegurar que refleje el estado actual
        updateNavigationMenu();
    }
}