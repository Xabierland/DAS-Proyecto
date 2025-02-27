package com.xabierland.librebook;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Configurar el botón de cambio de idioma
        findViewById(R.id.btnChangeLanguage).setOnClickListener(v -> showLanguageDialog());
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.settings);
    }
}