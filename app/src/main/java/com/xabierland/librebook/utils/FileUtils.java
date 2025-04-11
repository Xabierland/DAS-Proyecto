package com.xabierland.librebook.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Clase de utilidades para manejo de archivos
 */
public class FileUtils {
    
    private static final String TAG = "FileUtils";
    
    /**
     * Crea un archivo temporal para almacenar una imagen
     */
    public static File createTempImageFile(Context context) throws IOException {
        // Crear nombre único para el archivo
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefijo */
                ".jpg",         /* sufijo */
                storageDir      /* directorio */
        );
    }
    
    /**
     * Guarda una imagen en el almacenamiento
     */
    public static File saveBitmapToFile(Context context, Bitmap bitmap, String fileName) throws IOException {
        File directory = context.getFilesDir();
        File file = new File(directory, fileName);
        
        FileOutputStream outputStream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        outputStream.flush();
        outputStream.close();
        
        return file;
    }

    /**
     * Convierte un bitmap a una cadena Base64
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    /**
     * Obtiene un bitmap desde una Uri
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }
    
    /**
     * Obtiene y corrige la orientación de una imagen capturada desde la cámara
     * @param context Contexto de la aplicación
     * @param photoUri Uri de la foto capturada
     * @return Bitmap con la orientación corregida
     */
    public static Bitmap getAndFixImageOrientation(Context context, Uri photoUri) {
        try {
            // Obtener el bitmap original
            InputStream inputStream = context.getContentResolver().openInputStream(photoUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            
            if (bitmap == null) {
                Log.e(TAG, "No se pudo cargar el bitmap desde la Uri: " + photoUri);
                return null;
            }
            
            // Obtener la orientación de la imagen desde los metadatos EXIF
            inputStream = context.getContentResolver().openInputStream(photoUri);
            ExifInterface exif = null;
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    if (inputStream != null) {
                        exif = new ExifInterface(inputStream);
                    }
                } else {
                    // Para versiones anteriores a Android N, necesitamos la ruta del archivo
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    android.database.Cursor cursor = context.getContentResolver().query(
                            photoUri, filePathColumn, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        cursor.close();
                        if (filePath != null) {
                            exif = new ExifInterface(filePath);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error al obtener información EXIF", e);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            
            // Si no se pudo obtener la información EXIF, devolver el bitmap original
            if (exif == null) {
                return bitmap;
            }
            
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            
            // Rotar bitmap según la orientación
            return rotateBitmap(bitmap, orientation);
            
        } catch (Exception e) {
            Log.e(TAG, "Error al corregir la orientación de la imagen", e);
            return null;
        }
    }
    
    /**
     * Rota un bitmap según la orientación EXIF
     * @param bitmap Bitmap original
     * @param orientation Orientación EXIF
     * @return Bitmap rotado
     */
    private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1.0f, -1.0f);
                break;
            default:
                // Si no hay orientación que corregir, devolver el bitmap original
                return bitmap;
        }
        
        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            // Reciclar el bitmap original para liberar memoria
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            // Si hay problemas de memoria, devolver el bitmap original
            Log.e(TAG, "Error de memoria al rotar la imagen", e);
            return bitmap;
        }
    }
}