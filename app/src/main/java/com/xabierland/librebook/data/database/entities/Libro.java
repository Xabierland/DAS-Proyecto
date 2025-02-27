package com.xabierland.librebook.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.room.Ignore;

@Entity(tableName = "libros",
        indices = {@Index(value = {"isbn"}, unique = true)})
public class Libro {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "titulo")
    private String titulo;
    
    @ColumnInfo(name = "autor")
    private String autor;
    
    @ColumnInfo(name = "isbn")
    private String isbn;
    
    @ColumnInfo(name = "descripcion")
    private String descripcion;
    
    @ColumnInfo(name = "portada_url")
    private String portadaUrl;
    
    @ColumnInfo(name = "anio_publicacion")
    private int anioPublicacion;
    
    @ColumnInfo(name = "editorial")
    private String editorial;
    
    @ColumnInfo(name = "genero")
    private String genero;
    
    @ColumnInfo(name = "num_paginas")
    private int numPaginas;
    
    // Constructor vacío para Room
    public Libro() {
        // Constructor vacío para Room
    }
    
    // Constructor básico - marcado con @Ignore para que Room no lo use
    @Ignore
    public Libro(String titulo, String autor) {
        this.titulo = titulo;
        this.autor = autor;
    }
    
    // Constructor completo - marcado con @Ignore para que Room no lo use
    @Ignore
    public Libro(String titulo, String autor, String isbn, String descripcion, 
                String portadaUrl, int anioPublicacion, String editorial, 
                String genero, int numPaginas) {
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.descripcion = descripcion;
        this.portadaUrl = portadaUrl;
        this.anioPublicacion = anioPublicacion;
        this.editorial = editorial;
        this.genero = genero;
        this.numPaginas = numPaginas;
    }
        
    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPortadaUrl() {
        return portadaUrl;
    }

    public void setPortadaUrl(String portadaUrl) {
        this.portadaUrl = portadaUrl;
    }

    public int getAnioPublicacion() {
        return anioPublicacion;
    }

    public void setAnioPublicacion(int anioPublicacion) {
        this.anioPublicacion = anioPublicacion;
    }

    public String getEditorial() {
        return editorial;
    }

    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public int getNumPaginas() {
        return numPaginas;
    }

    public void setNumPaginas(int numPaginas) {
        this.numPaginas = numPaginas;
    }
}