package com.xabierland.librebook.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Clase de utilidades para manejo de archivos
 */
public class FileUtils {
    
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
     * Obtiene un bitmap desde una Uri
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }
}