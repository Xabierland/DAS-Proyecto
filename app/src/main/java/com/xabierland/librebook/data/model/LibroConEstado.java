package com.xabierland.librebook.data.models;

import androidx.room.ColumnInfo;

// Esta clase no es una entidad Room, es un POJO para resultados de consultas
public class LibroConEstado {
    // Campos del libro
    private int id;
    
    private String titulo;
    
    private String autor;
    
    private String isbn;
    
    private String descripcion;
    
    @ColumnInfo(name = "portada_url")
    private String portadaUrl;
    
    @ColumnInfo(name = "anio_publicacion")
    private int anioPublicacion;
    
    private String editorial;
    
    private String genero;
    
    @ColumnInfo(name = "num_paginas")
    private int numPaginas;
    
    // Campos de la relación usuario-libro
    @ColumnInfo(name = "estado_lectura")
    private String estadoLectura;
    
    @ColumnInfo(name = "es_favorito")
    private boolean esFavorito;
    
    private Float calificacion;
    
    @ColumnInfo(name = "pagina_actual")
    private Integer paginaActual;
    
    // Constructor por defecto necesario para Room
    public LibroConEstado() {}
    
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

    public String getEstadoLectura() {
        return estadoLectura;
    }

    public void setEstadoLectura(String estadoLectura) {
        this.estadoLectura = estadoLectura;
    }

    public boolean isEsFavorito() {
        return esFavorito;
    }

    public void setEsFavorito(boolean esFavorito) {
        this.esFavorito = esFavorito;
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
    
    // Métodos útiles para la aplicación
    public int getProgresoLectura() {
        if (paginaActual == null || numPaginas == 0) {
            return 0;
        }
        return (int) ((paginaActual * 100.0f) / numPaginas);
    }
    
    public boolean estaCompletado() {
        return "leido".equals(estadoLectura);
    }
    
    public boolean estaEnProgreso() {
        return "leyendo".equals(estadoLectura);
    }
    
    public boolean estaPorLeer() {
        return "por_leer".equals(estadoLectura);
    }
}