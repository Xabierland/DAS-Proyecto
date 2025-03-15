package com.xabierland.librebook.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.xabierland.librebook.R;
import com.xabierland.librebook.adapters.LibroAdapter;
import com.xabierland.librebook.adapters.UsuarioAdapter;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.repositories.LibroRepository;
import com.xabierland.librebook.data.repositories.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity {

    private EditText editTextSearch;
    private RecyclerView recyclerViewResults;
    private ProgressBar progressBarSearch;
    private TextView textViewNoResults;
    private SwitchMaterial switchSearchType;
    
    private LibroRepository libroRepository;
    private UsuarioRepository usuarioRepository;
    
    private LibroAdapter libroAdapter;
    private UsuarioAdapter usuarioAdapter;
    
    private List<Libro> libros = new ArrayList<>();
    private List<Usuario> usuarios = new ArrayList<>();
    
    private boolean isSearchingUsers = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        // Inicializar repositorios
        libroRepository = new LibroRepository(getApplication());
        usuarioRepository = new UsuarioRepository(getApplication());
        
        // Inicializar vistas
        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        textViewNoResults = findViewById(R.id.textViewNoResults);
        switchSearchType = findViewById(R.id.switchSearchType);
        
        editTextSearch.requestFocus();

        // Inicializar adaptadores
        libroAdapter = new LibroAdapter(libros);
        usuarioAdapter = new UsuarioAdapter(usuarios);
        
        // Configurar RecyclerView inicialmente para libros
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewResults.setAdapter(libroAdapter);
        
        // Configurar switch de tipo de búsqueda
        switchSearchType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Limpiar resultados actuales
            clearResults();
            
            isSearchingUsers = isChecked;
            
            // Cambiar el hint del campo de búsqueda según el tipo
            if (isSearchingUsers) {
                editTextSearch.setHint(getString(R.string.search_users_hint));
                recyclerViewResults.setAdapter(usuarioAdapter);
            } else {
                editTextSearch.setHint(getString(R.string.search_hint));
                recyclerViewResults.setAdapter(libroAdapter);
            }
            
            // Si hay texto en el campo de búsqueda, realizar la búsqueda con el nuevo tipo
            String searchText = editTextSearch.getText().toString().trim();
            if (searchText.length() > 2) {
                performSearch(searchText);
            } else {
                showEmptyResults();
            }
        });
        
        // Configurar listener para el campo de búsqueda
        setupSearchListener();
    }
    
    private void clearResults() {
        libros.clear();
        usuarios.clear();
        libroAdapter.notifyDataSetChanged();
        usuarioAdapter.notifyDataSetChanged();
    }
    
    private void setupSearchListener() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No se requiere implementación
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No se requiere implementación
            }

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = s.toString().trim();
                
                // Realizar búsqueda después de un breve retraso para evitar búsquedas excesivas
                if (searchText.length() > 2) {
                    // Mostrar el indicador de progreso
                    showLoading(true);
                    
                    // Realizar la búsqueda
                    performSearch(searchText);
                } else if (searchText.isEmpty()) {
                    // Si el campo está vacío, limpiar resultados
                    clearResults();
                    showEmptyResults();
                }
            }
        });
    }
    
    private void performSearch(String query) {
        if (isSearchingUsers) {
            searchUsers(query);
        } else {
            searchBooks(query);
        }
    }
    
    private void searchBooks(String query) {
        libroRepository.buscarLibros(query, result -> {
            runOnUiThread(() -> {
                // Ocultar el indicador de progreso
                showLoading(false);
                
                // Actualizar la lista de libros
                libros.clear();
                if (result != null && !result.isEmpty()) {
                    libros.addAll(result);
                    libroAdapter.notifyDataSetChanged();
                    showResults();
                } else {
                    showEmptyResults();
                }
            });
        });
    }
    
    private void searchUsers(String query) {
        usuarioRepository.buscarUsuarios(query, result -> {
            runOnUiThread(() -> {
                // Ocultar el indicador de progreso
                showLoading(false);
                
                // Actualizar la lista de usuarios
                usuarios.clear();
                if (result != null && !result.isEmpty()) {
                    usuarios.addAll(result);
                    usuarioAdapter.notifyDataSetChanged();
                    showResults();
                } else {
                    showEmptyResults();
                }
            });
        });
    }
    
    private void showLoading(boolean isLoading) {
        progressBarSearch.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            textViewNoResults.setVisibility(View.GONE);
        }
    }
    
    private void showResults() {
        recyclerViewResults.setVisibility(View.VISIBLE);
        textViewNoResults.setVisibility(View.GONE);
    }
    
    private void showEmptyResults() {
        recyclerViewResults.setVisibility(View.GONE);
        textViewNoResults.setVisibility(View.VISIBLE);
        
        // Actualizar mensaje de no resultados según el tipo de búsqueda
        if (isSearchingUsers) {
            textViewNoResults.setText(getString(R.string.no_users_found));
        } else {
            textViewNoResults.setText(getString(R.string.no_results));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.search);
    }
}