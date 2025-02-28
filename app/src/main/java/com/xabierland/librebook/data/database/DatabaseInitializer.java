package com.xabierland.librebook.data.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.repositories.LibroRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseInitializer {
    private static final String TAG = "DatabaseInitializer";
    
    /**
     * Método para precargar la base de datos con 10 libros de Dostoievski
     */
    public static void precargarBaseDeDatos(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            
            // Verificar si la base de datos está vacía
            int count = db.libroDao().obtenerTodosLosLibros().size();
            
            if (count == 0) {
                Log.d(TAG, "Base de datos vacía, precargando libros...");
                
                // Crear lista de libros
                List<Libro> libros = new ArrayList<>();
                
                // Libro 1
                Libro libro1 = new Libro();
                libro1.setTitulo("Crimen y castigo");
                libro1.setAutor("Fiódor Dostoievski");
                libro1.setIsbn("9788420674278");
                libro1.setDescripcion("Relata la historia de Rodión Raskólnikov, un estudiante que vive en una pequeña habitación de San Petersburgo. En plena crisis ideológica y económica planea el asesinato de una vieja usurera para robar sus pertenencias.");
                libro1.setPortadaUrl("https://covers.openlibrary.org/b/id/8286347-L.jpg");
                libro1.setAnioPublicacion(1866);
                libro1.setEditorial("Alianza Editorial");
                libro1.setGenero("Novela psicológica");
                libro1.setNumPaginas(671);
                libros.add(libro1);
                
                // Libro 2
                Libro libro2 = new Libro();
                libro2.setTitulo("Los hermanos Karamázov");
                libro2.setAutor("Fiódor Dostoievski");
                libro2.setIsbn("9788420674285");
                libro2.setDescripcion("La última novela de Dostoievski narra el conflicto entre padres e hijos. El despótico y sensual Fiódor Karamázov es asesinado y uno de sus hijos, Dmitri, es arrestado por el crimen.");
                libro2.setPortadaUrl("https://covers.openlibrary.org/b/id/8231990-L.jpg");
                libro2.setAnioPublicacion(1880);
                libro2.setEditorial("Alianza Editorial");
                libro2.setGenero("Novela filosófica");
                libro2.setNumPaginas(982);
                libros.add(libro2);
                
                // Libro 3
                Libro libro3 = new Libro();
                libro3.setTitulo("El idiota");
                libro3.setAutor("Fiódor Dostoievski");
                libro3.setIsbn("9788420674292");
                libro3.setDescripcion("El príncipe Myshkin, un joven de familia aristocrática que regresa a Rusia tras estar varios años en un sanatorio suizo, trata de integrarse en la sociedad de San Petersburgo.");
                libro3.setPortadaUrl("https://covers.openlibrary.org/b/id/12909862-L.jpg");
                libro3.setAnioPublicacion(1869);
                libro3.setEditorial("Alianza Editorial");
                libro3.setGenero("Novela psicológica");
                libro3.setNumPaginas(733);
                libros.add(libro3);
                
                // Libro 4
                Libro libro4 = new Libro();
                libro4.setTitulo("Memorias del subsuelo");
                libro4.setAutor("Fiódor Dostoievski");
                libro4.setIsbn("9788420674308");
                libro4.setDescripcion("Una de las obras más influyentes de Dostoievski, narrada desde la perspectiva de un funcionario anónimo retirado, que vive aislado y amargado en San Petersburgo.");
                libro4.setPortadaUrl("https://covers.openlibrary.org/b/id/9276919-L.jpg");
                libro4.setAnioPublicacion(1864);
                libro4.setEditorial("Alianza Editorial");
                libro4.setGenero("Novela existencialista");
                libro4.setNumPaginas(171);
                libros.add(libro4);
                
                // Libro 5
                Libro libro5 = new Libro();
                libro5.setTitulo("El jugador");
                libro5.setAutor("Fiódor Dostoievski");
                libro5.setIsbn("9788420674315");
                libro5.setDescripcion("Basada en las propias experiencias de Dostoievski como jugador, narra la historia de un tutor que trabaja para una familia rusa y desarrolla una obsesión por el juego en un casino.");
                libro5.setPortadaUrl("https://covers.openlibrary.org/b/id/8420284-L.jpg");
                libro5.setAnioPublicacion(1866);
                libro5.setEditorial("Alianza Editorial");
                libro5.setGenero("Novela");
                libro5.setNumPaginas(193);
                libros.add(libro5);
                
                // Libro 6
                Libro libro6 = new Libro();
                libro6.setTitulo("Los demonios");
                libro6.setAutor("Fiódor Dostoievski");
                libro6.setIsbn("9788420674322");
                libro6.setDescripcion("Novela que refleja el clima político y social de la Rusia de mediados del siglo XIX, centrada en un grupo de revolucionarios nihilistas que se proponen subvertir el orden establecido.");
                libro6.setPortadaUrl("https://covers.openlibrary.org/b/id/8280858-L.jpg");
                libro6.setAnioPublicacion(1872);
                libro6.setEditorial("Alianza Editorial");
                libro6.setGenero("Novela política");
                libro6.setNumPaginas(763);
                libros.add(libro6);
                
                // Libro 7
                Libro libro7 = new Libro();
                libro7.setTitulo("Humillados y ofendidos");
                libro7.setAutor("Fiódor Dostoievski");
                libro7.setIsbn("9788420674339");
                libro7.setDescripcion("Primera novela larga de Dostoievski tras su regreso del exilio en Siberia, cuenta la historia de un joven escritor y su relación con una familia noble caída en desgracia.");
                libro7.setPortadaUrl("https://covers.openlibrary.org/b/id/12571580-L.jpg");
                libro7.setAnioPublicacion(1861);
                libro7.setEditorial("Alianza Editorial");
                libro7.setGenero("Novela social");
                libro7.setNumPaginas(438);
                libros.add(libro7);
                
                // Libro 8
                Libro libro8 = new Libro();
                libro8.setTitulo("El adolescente");
                libro8.setAutor("Fiódor Dostoievski");
                libro8.setIsbn("9788420674346");
                libro8.setDescripcion("Penúltima novela de Dostoievski que narra las experiencias de un joven, hijo ilegítimo, que busca encontrar su lugar en la sociedad mientras intenta reconciliarse con su padre.");
                libro8.setPortadaUrl("https://covers.openlibrary.org/b/id/6902395-L.jpg");
                libro8.setAnioPublicacion(1875);
                libro8.setEditorial("Alianza Editorial");
                libro8.setGenero("Bildungsroman");
                libro8.setNumPaginas(692);
                libros.add(libro8);
                
                // Libro 9
                Libro libro9 = new Libro();
                libro9.setTitulo("Noches blancas");
                libro9.setAutor("Fiódor Dostoievski");
                libro9.setIsbn("9788420674353");
                libro9.setDescripcion("Relato corto y sentimental que narra la historia de un soñador solitario que conoce a una joven durante las noches blancas de verano en San Petersburgo.");
                libro9.setPortadaUrl("https://covers.openlibrary.org/b/id/8244913-L.jpg");
                libro9.setAnioPublicacion(1848);
                libro9.setEditorial("Alianza Editorial");
                libro9.setGenero("Novela corta romántica");
                libro9.setNumPaginas(96);
                libros.add(libro9);
                
                // Libro 10
                Libro libro10 = new Libro();
                libro10.setTitulo("El doble");
                libro10.setAutor("Fiódor Dostoievski");
                libro10.setIsbn("9788420674360");
                libro10.setDescripcion("Obra que explora la dualidad psicológica a través de la historia de un funcionario del gobierno que se encuentra con su doble exacto, desencadenando una espiral de locura.");
                libro10.setPortadaUrl("https://covers.openlibrary.org/b/id/7930196-L.jpg");
                libro10.setAnioPublicacion(1846);
                libro10.setEditorial("Alianza Editorial");
                libro10.setGenero("Novela psicológica");
                libro10.setNumPaginas(177);
                libros.add(libro10);
                
                // Insertar los libros en la base de datos
                db.libroDao().insertarLibros(libros);
                
                Log.d(TAG, "Base de datos precargada con " + libros.size() + " libros.");
            } else {
                Log.d(TAG, "La base de datos ya contiene datos, no es necesario precargar.");
            }
        });
    }
    
    /**
     * Callback para inicializar la base de datos
     */
    public static RoomDatabase.Callback getDatabaseCreationCallback() {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
                Log.d(TAG, "Base de datos creada.");
            }
            
            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                Log.d(TAG, "Base de datos abierta.");
            }
        };
    }
}