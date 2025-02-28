package com.xabierland.librebook.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.xabierland.librebook.data.database.daos.LibroDao;
import com.xabierland.librebook.data.database.daos.UsuarioDao;
import com.xabierland.librebook.data.database.daos.UsuarioLibroDao;
import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.database.entities.UsuarioLibro;

@Database(
    entities = {
        Usuario.class,
        Libro.class,
        UsuarioLibro.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static volatile AppDatabase INSTANCE;
    
    // DAOs abstractos
    public abstract UsuarioDao usuarioDao();
    public abstract LibroDao libroDao();
    public abstract UsuarioLibroDao usuarioLibroDao();
    
    // Método para obtener la instancia de la base de datos
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "mibiblioteca_db")
                            .fallbackToDestructiveMigration()
                            .addCallback(DatabaseInitializer.getDatabaseCreationCallback())
                            .build();
                    
                    // Precargar la base de datos con datos iniciales
                    DatabaseInitializer.precargarBaseDeDatos(context);
                }
            }
        }
        return INSTANCE;
    }
}