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

import com.xabierland.librebook.R;
import com.xabierland.librebook.adapters.LibroAdapter;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.repositories.LibroRepository;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity {

    private EditText editTextSearch;
    private RecyclerView recyclerViewResults;
    private ProgressBar progressBarSearch;
    private TextView textViewNoResults;
    
    private LibroRepository libroRepository;
    private LibroAdapter libroAdapter;
    private List<Libro> libros = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        // Inicializar el repositorio
        libroRepository = new LibroRepository(getApplication());
        
        // Inicializar vistas
        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        textViewNoResults = findViewById(R.id.textViewNoResults);
        
        // Configurar RecyclerView
        setupRecyclerView();
        
        // Configurar listener para el campo de búsqueda
        setupSearchListener();
    }
    
    private void setupRecyclerView() {
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        libroAdapter = new LibroAdapter(libros);
        recyclerViewResults.setAdapter(libroAdapter);
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
                // Realizar búsqueda después de un breve retraso para evitar búsquedas excesivas
                if (s.length() > 2) {
                    // Mostrar el indicador de progreso
                    showLoading(true);
                    
                    // Buscar libros
                    searchBooks(s.toString());
                } else if (s.length() == 0) {
                    // Si el campo está vacío, limpiar resultados
                    libros.clear();
                    libroAdapter.notifyDataSetChanged();
                    showEmptyResults();
                }
            }
        });
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