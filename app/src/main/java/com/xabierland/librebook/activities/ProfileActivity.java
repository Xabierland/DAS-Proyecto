package com.xabierland.librebook.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private static final int REQUEST_PERMISSION_PROFILE_PIC = 101;

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_VIEW_ONLY = "view_only";

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

    // URI para las fotos tomadas con la cámara
    private Uri photoURI;

    // Launchers para selección de foto y cámara
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Inicializar repositorios
        usuarioRepository = new UsuarioRepository(getApplication());
        bibliotecaRepository = new BibliotecaRepository(getApplication());

        // Inicializar vistas
        initViews();

        // Verificar si estamos viendo el perfil de otro usuario
        if (getIntent().getBooleanExtra(EXTRA_VIEW_ONLY, false)) {
            // Modo visualización de otro usuario
            int otherUserId = getIntent().getIntExtra(EXTRA_USER_ID, -1);
            if (otherUserId != -1) {
                // Ocultar botón de cambio de foto y otras opciones de edición
                fabChangeProfilePic.setVisibility(View.GONE);
                
                // Cargar datos del usuario específico
                loadOtherUserData(otherUserId);
            } else {
                // ID inválido, mostrar error y volver
                Toast.makeText(this, "Error al cargar perfil de usuario", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // Configurar los launchers para seleccionar imágenes y tomar fotos
            setupLaunchers();
            
            // Verificar sesión del usuario actual
            checkUserSession();
        }
    }

    private void setupLaunchers() {
        // Launcher para seleccionar imágenes de la galería
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        uploadProfileImage(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        
        // Launcher para tomar fotos con la cámara
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        // La imagen se guarda en photoURI
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
                        uploadProfileImage(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        int currentUserId = sharedPreferences.getInt("userId", -1);

        if (isLoggedIn && currentUserId != -1 && currentUserId == usuarioId) {
            // Recargar solo los libros
            loadBooks();
        }
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
        fabChangeProfilePic.setOnClickListener(v -> checkPermissionAndShowOptions());
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
            Toast.makeText(this, getString(R.string.login_required_profile), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ProfileActivity.this, getString(R.string.error_loading_user_data), Toast.LENGTH_SHORT).show();
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
            if (usuario.getFotoPerfil().startsWith("http")) {
                // Es una URL remota
                imageViewProfilePic.setImageURI(null); // Limpiar imagen anterior
                // Usar ImageLoader para cargar desde URL
                com.xabierland.librebook.utils.ImageLoader.loadImage(usuario.getFotoPerfil(), imageViewProfilePic);
            } else {
                // Es un archivo local
                File imgFile = new File(usuario.getFotoPerfil());
                if (imgFile.exists()) {
                    imageViewProfilePic.setImageURI(Uri.fromFile(imgFile));
                }
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

    private void checkPermissionAndShowOptions() {
        List<String> permissions = new ArrayList<>();
        
        // Verificar permiso de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        
        // Verificar permiso de almacenamiento según la versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                    permissions.toArray(new String[0]), 
                    REQUEST_PERMISSION_PROFILE_PIC);
        } else {
            // Mostrar opciones si ya tenemos permisos
            showImageSourceOptions();
        }
    }

    private void showImageSourceOptions() {
        final CharSequence[] options = {"Tomar foto", "Elegir de la galería", "Cancelar"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar foto de perfil");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Tomar foto")) {
                openCamera();
            } else if (options[item].equals("Elegir de la galería")) {
                openGallery();
            } else if (options[item].equals("Cancelar")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Verificar que hay una actividad de cámara disponible
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Crear archivo para la foto
            File photoFile = null;
            try {
                photoFile = FileUtils.createTempImageFile(this);
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear archivo temporal", Toast.LENGTH_SHORT).show();
            }
            
            // Si el archivo se creó correctamente
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.xabierland.librebook.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Bitmap bitmap) {
        // Mostrar diálogo de carga
        AlertDialog loadingDialog = createLoadingDialog();
        loadingDialog.show();
        
        try {
            // Convertir bitmap a base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
            
            // Actualizar la UI con la nueva imagen temporalmente
            imageViewProfilePic.setImageBitmap(bitmap);
            
            // Enviar la imagen al servidor
            usuarioRepository.actualizarUsuario(usuario, base64Image, result -> {
                loadingDialog.dismiss();
                
                if (result > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, getString(R.string.profile_pic_updated), Toast.LENGTH_SHORT).show();
                        
                        // Actualizar header del navigation menu
                        updateNavigationHeader();
                        
                        // Actualizar la información del usuario con la URL recibida desde el servidor
                        loadUserData();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, getString(R.string.error_updating_profile_pic), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            loadingDialog.dismiss();
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_saving_image), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_PROFILE_PIC) {
            if (grantResults.length > 0 && allPermissionsGranted(grantResults)) {
                showImageSourceOptions();
            } else {
                // Mostrar un diálogo preguntando si quiere ir a los ajustes
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission_title))
                    .setMessage(getString(R.string.permission_gallery_message))
                    .setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
                        // Intent implícito para abrir los ajustes de la aplicación
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create()
                    .show();
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean allPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private AlertDialog createLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        return builder.create();
    }

    private void loadOtherUserData(int userId) {
        usuarioRepository.obtenerUsuarioPorId(userId, usuario -> {
            runOnUiThread(() -> {
                if (usuario != null) {
                    this.usuario = usuario;
                    this.usuarioId = usuario.getId();
                    
                    // Actualizar título con el nombre del usuario
                    getSupportActionBar().setTitle(usuario.getNombre());
                    
                    // Actualizar interfaz con datos del usuario
                    updateUI();
                    
                    // Cargar libros del usuario
                    loadBooks();
                } else {
                    Toast.makeText(ProfileActivity.this, "No se encontró el usuario", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    public boolean isViewingOtherProfile() {
        // Si estamos en modo visualización de otro usuario, retornar true
        return getIntent().getBooleanExtra(EXTRA_VIEW_ONLY, false);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.my_profile);
    }
}