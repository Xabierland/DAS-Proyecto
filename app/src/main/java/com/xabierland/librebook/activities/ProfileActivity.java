package com.xabierland.librebook.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xabierland.librebook.R;
import com.xabierland.librebook.adapters.BookCardAdapter;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.models.LibroConEstado;
import com.xabierland.librebook.data.repositories.BibliotecaRepository;
import com.xabierland.librebook.data.repositories.UsuarioRepository;
import com.xabierland.librebook.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 101;

    // Vistas
    private CircleImageView imageViewProfilePic;
    private FloatingActionButton fabChangeProfilePic;
    private TextView textViewUserName;
    private TextView textViewUserEmail;
    private TextView textViewReadCount;
    private TextView textViewReadingCount;
    private TextView textViewToReadCount;
    private RecyclerView recyclerViewReading;
    private RecyclerView recyclerViewToRead;
    private RecyclerView recyclerViewRead;
    private TextView textViewEmptyReading;
    private TextView textViewEmptyToRead;
    private TextView textViewEmptyRead;

    // Adaptadores
    private BookCardAdapter readingAdapter;
    private BookCardAdapter toReadAdapter;
    private BookCardAdapter readAdapter;

    // Repositorios
    private UsuarioRepository usuarioRepository;
    private BibliotecaRepository bibliotecaRepository;

    // Datos
    private int usuarioId;
    private Usuario usuario;
    private List<LibroConEstado> readingBooks = new ArrayList<>();
    private List<LibroConEstado> toReadBooks = new ArrayList<>();
    private List<LibroConEstado> readBooks = new ArrayList<>();

    // Launcher para selección de foto
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Inicializar repositorios
        usuarioRepository = new UsuarioRepository(getApplication());
        bibliotecaRepository = new BibliotecaRepository(getApplication());

        // Inicializar vistas
        initViews();

        // Configurar el launcher para seleccionar imágenes
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            saveProfileImage(bitmap);
                            imageViewProfilePic.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Verificar sesión del usuario
        checkUserSession();
    }

    private void initViews() {
        // Vistas de perfil
        imageViewProfilePic = findViewById(R.id.imageViewProfilePic);
        fabChangeProfilePic = findViewById(R.id.fabChangeProfilePic);
        textViewUserName = findViewById(R.id.textViewUserName);
        textViewUserEmail = findViewById(R.id.textViewUserEmail);
        textViewReadCount = findViewById(R.id.textViewReadCount);
        textViewReadingCount = findViewById(R.id.textViewReadingCount);
        textViewToReadCount = findViewById(R.id.textViewToReadCount);

        // RecyclerViews
        recyclerViewReading = findViewById(R.id.recyclerViewReading);
        recyclerViewToRead = findViewById(R.id.recyclerViewToRead);
        recyclerViewRead = findViewById(R.id.recyclerViewRead);

        // TextViews para estados vacíos
        textViewEmptyReading = findViewById(R.id.textViewEmptyReading);
        textViewEmptyToRead = findViewById(R.id.textViewEmptyToRead);
        textViewEmptyRead = findViewById(R.id.textViewEmptyRead);

        // Configurar RecyclerViews
        setupRecyclerViews();

        // Configurar botón de cambio de foto
        fabChangeProfilePic.setOnClickListener(v -> checkPermissionAndOpenGallery());
    }

    private void setupRecyclerViews() {
        // Configurar RecyclerView para libros leyendo
        recyclerViewReading.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        readingAdapter = new BookCardAdapter(readingBooks, true);
        recyclerViewReading.setAdapter(readingAdapter);

        // Configurar RecyclerView para libros por leer
        recyclerViewToRead.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        toReadAdapter = new BookCardAdapter(toReadBooks, false);
        recyclerViewToRead.setAdapter(toReadAdapter);

        // Configurar RecyclerView para libros leídos
        recyclerViewRead.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        readAdapter = new BookCardAdapter(readBooks, false);
        recyclerViewRead.setAdapter(readAdapter);
    }

    private void checkUserSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        usuarioId = sharedPreferences.getInt("userId", -1);

        if (!isLoggedIn || usuarioId == -1) {
            // Si no hay sesión activa, redirigir al login
            Toast.makeText(this, "Debes iniciar sesión para ver tu perfil", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Cargar datos del usuario
        loadUserData();
    }

    private void loadUserData() {
        usuarioRepository.obtenerUsuarioPorId(usuarioId, usuario -> {
            runOnUiThread(() -> {
                if (usuario != null) {
                    this.usuario = usuario;
                    updateUI();
                } else {
                    Toast.makeText(ProfileActivity.this, "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void updateUI() {
        // Actualizar información básica del usuario
        textViewUserName.setText(usuario.getNombre());
        textViewUserEmail.setText(usuario.getEmail());

        // Cargar foto de perfil si existe
        if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
            File imgFile = new File(usuario.getFotoPerfil());
            if (imgFile.exists()) {
                imageViewProfilePic.setImageURI(Uri.fromFile(imgFile));
            }
        }

        // Cargar listas de libros
        loadBooks();
    }

    private void loadBooks() {
        // Cargar libros leyendo
        bibliotecaRepository.obtenerLibrosLeyendo(usuarioId, books -> {
            runOnUiThread(() -> {
                readingBooks.clear();
                if (books != null && !books.isEmpty()) {
                    readingBooks.addAll(books);
                    readingAdapter.notifyDataSetChanged();
                    recyclerViewReading.setVisibility(View.VISIBLE);
                    textViewEmptyReading.setVisibility(View.GONE);
                } else {
                    recyclerViewReading.setVisibility(View.GONE);
                    textViewEmptyReading.setVisibility(View.VISIBLE);
                }
                textViewReadingCount.setText(String.valueOf(readingBooks.size()));
            });
        });

        // Cargar libros por leer
        bibliotecaRepository.obtenerLibrosPorLeer(usuarioId, books -> {
            runOnUiThread(() -> {
                toReadBooks.clear();
                if (books != null && !books.isEmpty()) {
                    toReadBooks.addAll(books);
                    toReadAdapter.notifyDataSetChanged();
                    recyclerViewToRead.setVisibility(View.VISIBLE);
                    textViewEmptyToRead.setVisibility(View.GONE);
                } else {
                    recyclerViewToRead.setVisibility(View.GONE);
                    textViewEmptyToRead.setVisibility(View.VISIBLE);
                }
                textViewToReadCount.setText(String.valueOf(toReadBooks.size()));
            });
        });

        // Cargar libros leídos
        bibliotecaRepository.obtenerLibrosLeidos(usuarioId, books -> {
            runOnUiThread(() -> {
                readBooks.clear();
                if (books != null && !books.isEmpty()) {
                    readBooks.addAll(books);
                    readAdapter.notifyDataSetChanged();
                    recyclerViewRead.setVisibility(View.VISIBLE);
                    textViewEmptyRead.setVisibility(View.GONE);
                } else {
                    recyclerViewRead.setVisibility(View.GONE);
                    textViewEmptyRead.setVisibility(View.VISIBLE);
                }
                textViewReadCount.setText(String.valueOf(readBooks.size()));
            });
        });
    }

    private void checkPermissionAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void saveProfileImage(Bitmap bitmap) {
        if (usuario == null) return;

        try {
            // Crear directorio para imágenes si no existe
            File directory = new File(getFilesDir(), "profile_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Crear archivo para la imagen
            String fileName = "profile_" + usuario.getId() + ".jpg";
            File file = new File(directory, fileName);

            // Guardar imagen en el archivo
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            // Actualizar ruta de la imagen en el usuario
            String filePath = file.getAbsolutePath();
            usuario.setFotoPerfil(filePath);

            // Actualizar usuario en la base de datos
            usuarioRepository.actualizarUsuario(usuario, result -> {
                runOnUiThread(() -> {
                    if (result > 0) {
                        Toast.makeText(ProfileActivity.this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error al actualizar foto de perfil", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permiso denegado para acceder a la galería", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected String getActivityTitle() {
        return "Mi Perfil";
    }
}