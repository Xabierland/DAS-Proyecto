package com.xabierland.librebook.data.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "usuarios_libros",
        foreignKeys = {
            @ForeignKey(entity = Usuario.class,
                        parentColumns = "id",
                        childColumns = "usuario_id",
                        onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Libro.class,
                        parentColumns = "id",
                        childColumns = "libro_id",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
            @Index(value = {"usuario_id", "libro_id"}, unique = true),
            @Index("usuario_id"),
            @Index("libro_id")
        })
public class UsuarioLibro {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "usuario_id")
    private int usuarioId;
    
    @ColumnInfo(name = "libro_id")
    private int libroId;
    
    // Enum para los estados de lectura
    public static final String ESTADO_POR_LEER = "por_leer";
    public static final String ESTADO_LEYENDO = "leyendo";
    public static final String ESTADO_LEIDO = "leido";
    
    @ColumnInfo(name = "estado_lectura")
    private String estadoLectura;
    
    @ColumnInfo(name = "es_favorito")
    private boolean esFavorito;
    
    @ColumnInfo(name = "fecha_adicion")
    private long fechaAdicion;
    
    @ColumnInfo(name = "fecha_inicio_lectura")
    private Long fechaInicioLectura;
    
    @ColumnInfo(name = "fecha_fin_lectura")
    private Long fechaFinLectura;
    
    @ColumnInfo(name = "calificacion")
    private Float calificacion;  // 0 a 5 estrellas
    
    @ColumnInfo(name = "pagina_actual")
    private Integer paginaActual;
    
    @ColumnInfo(name = "notas")
    private String notas;

    // Constructor
    public UsuarioLibro(int usuarioId, int libroId, String estadoLectura) {
        this.usuarioId = usuarioId;
        this.libroId = libroId;
        this.estadoLectura = estadoLectura;
        this.esFavorito = false;
        this.fechaAdicion = System.currentTimeMillis();
    }
    
    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getLibroId() {
        return libroId;
    }

    public void setLibroId(int libroId) {
        this.libroId = libroId;
    }

    public String getEstadoLectura() {
        return estadoLectura;
    }

    public void setEstadoLectura(String estadoLectura) {
        this.estadoLectura = estadoLectura;
        
        // Actualizar fechas según el estado
        if (ESTADO_LEYENDO.equals(estadoLectura) && fechaInicioLectura == null) {
            fechaInicioLectura = System.currentTimeMillis();
        } else if (ESTADO_LEIDO.equals(estadoLectura) && fechaFinLectura == null) {
            fechaFinLectura = System.currentTimeMillis();
        }
    }

    public boolean isEsFavorito() {
        return esFavorito;
    }

    public void setEsFavorito(boolean esFavorito) {
        this.esFavorito = esFavorito;
    }

    public long getFechaAdicion() {
        return fechaAdicion;
    }

    public void setFechaAdicion(long fechaAdicion) {
        this.fechaAdicion = fechaAdicion;
    }

    public Long getFechaInicioLectura() {
        return fechaInicioLectura;
    }

    public void setFechaInicioLectura(Long fechaInicioLectura) {
        this.fechaInicioLectura = fechaInicioLectura;
    }

    public Long getFechaFinLectura() {
        return fechaFinLectura;
    }

    public void setFechaFinLectura(Long fechaFinLectura) {
        this.fechaFinLectura = fechaFinLectura;
    }

    public Float getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Float calificacion) {
        this.calificacion = calificacion;
    }

    public Integer getPaginaActual() {
        return paginaActual;
    }

    public void setPaginaActual(Integer paginaActual) {
        this.paginaActual = paginaActual;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}