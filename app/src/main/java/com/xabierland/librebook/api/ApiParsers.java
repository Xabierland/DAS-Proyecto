package com.xabierland.librebook.api;

import com.xabierland.librebook.data.database.entities.Libro;
import com.xabierland.librebook.data.database.entities.Usuario;
import com.xabierland.librebook.data.models.LibroConEstado;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ApiParsers {

    // Parser para un solo libro
    public static ApiClient.ApiResponseParser<Libro> libroParser() {
        return jsonResponse -> {
            JSONObject json = new JSONObject(jsonResponse);
            Libro libro = new Libro();
            libro.setId(json.getInt("id"));
            libro.setTitulo(json.getString("titulo"));
            libro.setAutor(json.getString("autor"));
            if (!json.isNull("isbn")) libro.setIsbn(json.getString("isbn"));
            if (!json.isNull("descripcion")) libro.setDescripcion(json.getString("descripcion"));
            if (!json.isNull("portada_url")) libro.setPortadaUrl(json.getString("portada_url"));
            if (!json.isNull("anio_publicacion")) libro.setAnioPublicacion(json.getInt("anio_publicacion"));
            if (!json.isNull("editorial")) libro.setEditorial(json.getString("editorial"));
            if (!json.isNull("genero")) libro.setGenero(json.getString("genero"));
            if (!json.isNull("num_paginas")) libro.setNumPaginas(json.getInt("num_paginas"));
            return libro;
        };
    }

    // Parser para una lista de libros
    public static ApiClient.ApiResponseParser<List<Libro>> librosListParser() {
        return jsonResponse -> {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            List<Libro> libros = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Libro libro = new Libro();
                libro.setId(json.getInt("id"));
                libro.setTitulo(json.getString("titulo"));
                libro.setAutor(json.getString("autor"));
                if (!json.isNull("isbn")) libro.setIsbn(json.getString("isbn"));
                if (!json.isNull("descripcion")) libro.setDescripcion(json.getString("descripcion"));
                if (!json.isNull("portada_url")) libro.setPortadaUrl(json.getString("portada_url"));
                if (!json.isNull("anio_publicacion")) libro.setAnioPublicacion(json.getInt("anio_publicacion"));
                if (!json.isNull("editorial")) libro.setEditorial(json.getString("editorial"));
                if (!json.isNull("genero")) libro.setGenero(json.getString("genero"));
                if (!json.isNull("num_paginas")) libro.setNumPaginas(json.getInt("num_paginas"));
                libros.add(libro);
            }
            
            return libros;
        };
    }

    // Parser para un solo usuario
    public static ApiClient.ApiResponseParser<Usuario> usuarioParser() {
        return jsonResponse -> {
            JSONObject json = new JSONObject(jsonResponse);
            Usuario usuario = new Usuario("", "", "");
            usuario.setId(json.getInt("id"));
            usuario.setNombre(json.getString("nombre"));
            usuario.setEmail(json.getString("email"));
            if (!json.isNull("foto_perfil")) usuario.setFotoPerfil(json.getString("foto_perfil"));
            if (!json.isNull("fecha_registro")) usuario.setFechaRegistro(json.getLong("fecha_registro"));
            return usuario;
        };
    }

    // Parser para una lista de usuarios
    public static ApiClient.ApiResponseParser<List<Usuario>> usuariosListParser() {
        return jsonResponse -> {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            List<Usuario> usuarios = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Usuario usuario = new Usuario("", "", "");
                usuario.setId(json.getInt("id"));
                usuario.setNombre(json.getString("nombre"));
                usuario.setEmail(json.getString("email"));
                if (!json.isNull("foto_perfil")) usuario.setFotoPerfil(json.getString("foto_perfil"));
                if (!json.isNull("fecha_registro")) usuario.setFechaRegistro(json.getLong("fecha_registro"));
                usuarios.add(usuario);
            }
            
            return usuarios;
        };
    }

    // Parser para un libro con estado
    public static ApiClient.ApiResponseParser<LibroConEstado> libroConEstadoParser() {
        return jsonResponse -> {
            JSONObject json = new JSONObject(jsonResponse);
            LibroConEstado libroConEstado = new LibroConEstado();
            
            // Atributos del libro
            libroConEstado.setId(json.getInt("id"));
            libroConEstado.setTitulo(json.getString("titulo"));
            libroConEstado.setAutor(json.getString("autor"));
            if (!json.isNull("isbn")) libroConEstado.setIsbn(json.getString("isbn"));
            if (!json.isNull("descripcion")) libroConEstado.setDescripcion(json.getString("descripcion"));
            if (!json.isNull("portada_url")) libroConEstado.setPortadaUrl(json.getString("portada_url"));
            if (!json.isNull("anio_publicacion")) libroConEstado.setAnioPublicacion(json.getInt("anio_publicacion"));
            if (!json.isNull("editorial")) libroConEstado.setEditorial(json.getString("editorial"));
            if (!json.isNull("genero")) libroConEstado.setGenero(json.getString("genero"));
            if (!json.isNull("num_paginas")) libroConEstado.setNumPaginas(json.getInt("num_paginas"));
            
            // Atributos de la relación
            if (!json.isNull("estado_lectura")) libroConEstado.setEstadoLectura(json.getString("estado_lectura"));
            if (!json.isNull("es_favorito")) libroConEstado.setEsFavorito(json.getInt("es_favorito") == 1);
            if (!json.isNull("calificacion")) libroConEstado.setCalificacion((float) json.getDouble("calificacion"));
            if (!json.isNull("pagina_actual")) libroConEstado.setPaginaActual(json.getInt("pagina_actual"));
            if (!json.isNull("notas")) libroConEstado.setNotas(json.getString("notas"));
            
            return libroConEstado;
        };
    }

    // Parser para una lista de libros con estado
    public static ApiClient.ApiResponseParser<List<LibroConEstado>> librosConEstadoListParser() {
        return jsonResponse -> {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            List<LibroConEstado> libros = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                LibroConEstado libroConEstado = new LibroConEstado();
                
                // Atributos del libro
                libroConEstado.setId(json.getInt("id"));
                libroConEstado.setTitulo(json.getString("titulo"));
                libroConEstado.setAutor(json.getString("autor"));
                if (!json.isNull("isbn")) libroConEstado.setIsbn(json.getString("isbn"));
                if (!json.isNull("descripcion")) libroConEstado.setDescripcion(json.getString("descripcion"));
                if (!json.isNull("portada_url")) libroConEstado.setPortadaUrl(json.getString("portada_url"));
                if (!json.isNull("anio_publicacion")) libroConEstado.setAnioPublicacion(json.getInt("anio_publicacion"));
                if (!json.isNull("editorial")) libroConEstado.setEditorial(json.getString("editorial"));
                if (!json.isNull("genero")) libroConEstado.setGenero(json.getString("genero"));
                if (!json.isNull("num_paginas")) libroConEstado.setNumPaginas(json.getInt("num_paginas"));
                
                // Atributos de la relación
                if (!json.isNull("estado_lectura")) libroConEstado.setEstadoLectura(json.getString("estado_lectura"));
                if (!json.isNull("es_favorito")) libroConEstado.setEsFavorito(json.getInt("es_favorito") == 1);
                if (!json.isNull("calificacion")) libroConEstado.setCalificacion((float) json.getDouble("calificacion"));
                if (!json.isNull("pagina_actual")) libroConEstado.setPaginaActual(json.getInt("pagina_actual"));
                if (!json.isNull("notas")) libroConEstado.setNotas(json.getString("notas"));
                
                libros.add(libroConEstado);
            }
            
            return libros;
        };
    }

    // Parser genérico para respuestas de ID
    public static ApiClient.ApiResponseParser<Long> idResponseParser() {
        return jsonResponse -> {
            JSONObject json = new JSONObject(jsonResponse);
            return json.getLong("id");
        };
    }

    // Parser genérico para respuestas de número entero
    public static ApiClient.ApiResponseParser<Integer> integerResponseParser() {
        return jsonResponse -> {
            JSONObject json = new JSONObject(jsonResponse);
            return json.getInt("resultado");
        };
    }
}