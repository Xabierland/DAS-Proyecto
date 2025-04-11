package com.xabierland.librebook.activities;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 101;
    private static final int REQUEST_PERMISSION_CAMERA = 103;

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_VIEW_ONLY = "view_only";

    private Uri photoURI;

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
            cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            // En lugar de decodificar directamente, usamos nuestro método mejorado
                            // que corrige la orientación de la imagen
                            Bitmap bitmap = FileUtils.getAndFixImageOrientation(ProfileActivity.this, photoURI);
                            
                            // Procesar y guardar la imagen
                            if (bitmap != null) {
                                saveProfileImage(bitmap);
                                imageViewProfilePic.setImageBitmap(bitmap);
                            } else {
                                Toast.makeText(
                                    ProfileActivity.this,
                                    getString(R.string.error_loading_image), 
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(
                                ProfileActivity.this,
                                getString(R.string.error_loading_image), 
                                Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });
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
                            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            // Verificar sesión del usuario actual
            checkUserSession();
        }
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
            try {
                // Decodificar la cadena base64 a un bitmap
                byte[] decodedString = Base64.decode(usuario.getFotoPerfil(), Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageViewProfilePic.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                Log.e("ProfileActivity", "Error al decodificar imagen base64", e);
                // Usar imagen por defecto en caso de error
                imageViewProfilePic.setImageResource(R.drawable.default_profile_image);
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
        // Mostrar diálogo de opciones para seleccionar fuente de imagen
        String[] options = {getString(R.string.camera), getString(R.string.gallery)};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_image_source));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Opción de cámara
                checkCameraPermissionAndOpen();
            } else {
                // Opción de galería
                checkGalleryPermissionAndOpen();
            }
        });
        builder.show();
    }

    private void checkGalleryPermissionAndOpen() {
        // Este es tu código actual de verificación de permisos de galería
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Necesitamos READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                
                // Verifica si debemos mostrar una explicación
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, 
                        Manifest.permission.READ_MEDIA_IMAGES)) {
                    // Muestra una explicación al usuario
                    new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_needed))
                        .setMessage(getString(R.string.permission_gallery_explanation))
                        .setPositiveButton(getString(R.string.accept), (dialog, which) -> {
                            // Solicita el permiso después de que el usuario vea la explicación
                            ActivityCompat.requestPermissions(ProfileActivity.this,
                                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .create()
                        .show();
                } else {
                    // No necesita explicación, solicita directamente
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                }
            } else {
                openGallery();
            }
        } else {
            // Android 12 y versiones anteriores: Usamos READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                
                // Verifica si debemos mostrar una explicación
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, 
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Muestra una explicación al usuario
                    new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_needed))
                        .setMessage(getString(R.string.permission_gallery_explanation))
                        .setPositiveButton(getString(R.string.accept), (dialog, which) -> {
                            // Solicita el permiso después de que el usuario vea la explicación
                            ActivityCompat.requestPermissions(ProfileActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .create()
                        .show();
                } else {
                    // No necesita explicación, solicita directamente
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                }
            } else {
                openGallery();
            }
        }
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, 
                    Manifest.permission.CAMERA)) {
                // Mostrar explicación
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission_needed))
                    .setMessage(getString(R.string.permission_camera_explanation))
                    .setPositiveButton(getString(R.string.accept), (dialog, which) -> {
                        ActivityCompat.requestPermissions(ProfileActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                REQUEST_PERMISSION_CAMERA);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create()
                    .show();
            } else {
                // Solicitar permiso directamente
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_PERMISSION_CAMERA);
            }
        } else {
            openCamera();
        }
    }
    
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        try {
            // Crear el archivo donde debería ir la foto
            File photoFile = null;
            try {
                photoFile = FileUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error al crear el archivo
                Toast.makeText(this, getString(R.string.error_camera_file), Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Continuar solo si el archivo se creó correctamente
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.xabierland.librebook.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_camera_app), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveProfileImage(Bitmap bitmap) {
        if (usuario == null) return;
    
        try {
            // Convertir el bitmap a base64
            String base64Image = FileUtils.bitmapToBase64(bitmap);
            
            // Guardar la representación base64 en el usuario
            usuario.setFotoPerfil(base64Image);
            
            // Mostrar un diálogo de carga
            AlertDialog loadingDialog = createLoadingDialog();
            loadingDialog.show();
    
            // Actualizar usuario en la base de datos con la nueva imagen
            usuarioRepository.actualizarUsuario(usuario, result -> {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    
                    if (result > 0) {
                        Toast.makeText(ProfileActivity.this, getString(R.string.profile_pic_updated), Toast.LENGTH_SHORT).show();
                        
                        // Actualizar la UI con la nueva imagen
                        imageViewProfilePic.setImageBitmap(bitmap);
                        
                        // Actualizar también el menú de navegación
                        updateNavigationHeader();
                    } else {
                        Toast.makeText(ProfileActivity.this, getString(R.string.error_updating_profile_pic), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_saving_image), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
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
        } else if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                // Mostrar un diálogo preguntando si quiere ir a los ajustes
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission_title))
                    .setMessage(getString(R.string.permission_camera_message))
                    .setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create()
                    .show();
                Toast.makeText(this, getString(R.string.permission_denied_camera), Toast.LENGTH_SHORT).show();
            }
        }
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

    private AlertDialog createLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        return builder.create();
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