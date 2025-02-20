package com.xabierland.librebook;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle toggle;
    protected Toolbar toolbar;
    protected NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupToolbar();
        setupDrawer();
    }

    protected void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(getActivityTitle());
        }
    }

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
            (this instanceof SettingsActivity && itemId == R.id.nav_settings)) {
            return;
        }

        // Manejar la navegación
        Intent intent = null;
        
        if (itemId == R.id.nav_home) {
            intent = new Intent(this, MainActivity.class);
            // Limpiar el stack de activities si vamos al home
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } 
        else if (itemId == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
        }
        // Añadir más casos según necesites
        
        if (intent != null) {
            startActivity(intent);
            // Si no es la home, cerrar la activity actual
            if (itemId != R.id.nav_home) {
                finish();
            }
        }
    }

    protected void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("language", "");
        setLocale(language);
    }

    protected void setLocale(String language) {
        if (!language.isEmpty()) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
            
            SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
            editor.putString("language", language);
            editor.apply();
        }
    }

    protected void showLanguageDialog() {
        final String[] languages = {"English", "Español", "Euskera"};
        final String[] languageCodes = {"en", "es", "eu"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language);
        builder.setSingleChoiceItems(languages, -1, (dialog, which) -> {
            setLocale(languageCodes[which]);
            recreate();
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected abstract String getActivityTitle();
}