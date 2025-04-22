-- Crear base de datos
CREATE DATABASE IF NOT EXISTS Xxgabina001_librebook_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Xxgabina001_librebook_db;

-- Tabla de libros
CREATE TABLE IF NOT EXISTS libros (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    autor VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) DEFAULT '',
    descripcion TEXT,
    portada_url VARCHAR(255) DEFAULT '',
    anio_publicacion INT DEFAULT 0,
    editorial VARCHAR(255) DEFAULT '',
    genero VARCHAR(100) DEFAULT '',
    num_paginas INT DEFAULT 0,
    INDEX (isbn)
) ENGINE=InnoDB;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    foto_perfil MEDIUMBLOB,
    fecha_registro BIGINT NOT NULL,
    UNIQUE INDEX (email)
) ENGINE=InnoDB;

-- Tabla de relación usuario-libro
CREATE TABLE IF NOT EXISTS usuarios_libros (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    libro_id INT NOT NULL,
    estado_lectura ENUM('por_leer', 'leyendo', 'leido') DEFAULT 'por_leer',
    es_favorito TINYINT(1) DEFAULT 0,
    fecha_adicion BIGINT NOT NULL,
    fecha_inicio_lectura BIGINT NULL,
    fecha_fin_lectura BIGINT NULL,
    calificacion FLOAT NULL,
    pagina_actual INT NULL,
    notas TEXT NULL,
    UNIQUE INDEX (usuario_id, libro_id),
    INDEX (usuario_id),
    INDEX (libro_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (libro_id) REFERENCES libros(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insertar algunos datos de ejemplo (los mismos que tenía la app originalmente)
INSERT INTO libros (titulo, autor, isbn, descripcion, portada_url, anio_publicacion, editorial, genero, num_paginas) VALUES
('Crimen y castigo', 'Fiódor Dostoievski', '9788420674278', 'Relata la historia de Rodión Raskólnikov, un estudiante que vive en una pequeña habitación de San Petersburgo. En plena crisis ideológica y económica planea el asesinato de una vieja usurera para robar sus pertenencias.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1445052614i/27187428.jpg', 1866, 'Alianza Editorial', 'Novela psicológica', 671),
('Los hermanos Karamázov', 'Fiódor Dostoievski', '9788420674285', 'La última novela de Dostoievski narra el conflicto entre padres e hijos. El despótico y sensual Fiódor Karamázov es asesinado y uno de sus hijos, Dmitri, es arrestado por el crimen.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1458131286i/29529737.jpg', 1880, 'Alianza Editorial', 'Novela filosófica', 982),
('El idiota', 'Fiódor Dostoievski', '9788420674292', 'El príncipe Myshkin, un joven de familia aristocrática que regresa a Rusia tras estar varios años en un sanatorio suizo, trata de integrarse en la sociedad de San Petersburgo.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1480301010i/33140351.jpg', 1869, 'Alianza Editorial', 'Novela psicológica', 733),
('Memorias del subsuelo', 'Fiódor Dostoievski', '9788420674308', 'Una de las obras más influyentes de Dostoievski, narrada desde la perspectiva de un funcionario anónimo retirado, que vive aislado y amargado en San Petersburgo.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1719801722i/204454054.jpg', 1864, 'Alianza Editorial', 'Novela existencialista', 171),
('El jugador', 'Fiódor Dostoievski', '9788420674315', 'Basada en las propias experiencias de Dostoievski como jugador, narra la historia de un tutor que trabaja para una familia rusa y desarrolla una obsesión por el juego en un casino.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1660409488i/59736484.jpg', 1866, 'Alianza Editorial', 'Novela', 193),
('Los demonios', 'Fiódor Dostoievski', '9788420674322', 'Novela que refleja el clima político y social de la Rusia de mediados del siglo XIX, centrada en un grupo de revolucionarios nihilistas que se proponen subvertir el orden establecido.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1558320244i/45889115.jpg', 1872, 'Alianza Editorial', 'Novela política', 763),
('Humillados y ofendidos', 'Fiódor Dostoievski', '9788420674339', 'Primera novela larga de Dostoievski tras su regreso del exilio en Siberia, cuenta la historia de un joven escritor y su relación con una familia noble caída en desgracia.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1410963705i/23244795.jpg', 1861, 'Alianza Editorial', 'Novela social', 438),
('El eterno marido', 'Fiódor Dostoievski', '9788420678184', 'Novela que explora la relación entre un hombre y su difunto amigo, el marido eterno, y cómo la presencia de este último afecta a su vida y a su matrimonio.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1705677120i/33285812.jpg', 1870, 'Alianza Editorial', 'Novela psicológica', 232),
('Noches blancas', 'Fiódor Dostoievski', '9788420674353', 'Relato corto y sentimental que narra la historia de un soñador solitario que conoce a una joven durante las noches blancas de verano en San Petersburgo.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1436307004i/25868547.jpg', 1848, 'Alianza Editorial', 'Novela corta romántica', 96),
('El doble', 'Fiódor Dostoievski', '9788420674360', 'Obra que explora la dualidad psicológica a través de la historia de un funcionario del gobierno que se encuentra con su doble exacto, desencadenando una espiral de locura.', 'https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1420981383i/15719498.jpg', 1846, 'Alianza Editorial', 'Novela psicológica', 177);

-- Insertar usuarios predeterminados
INSERT INTO usuarios (nombre, email, password, fecha_registro) VALUES
('Administrador', 'admin@xabierland.com', 'admin', UNIX_TIMESTAMP() * 1000),
('Xabier Gabiña', 'xabierland@gmail.com', '123456', UNIX_TIMESTAMP() * 1000);

-- Insertar relaciones de biblioteca para Xabier
SET @xabier_id = (SELECT id FROM usuarios WHERE email = 'xabierland@gmail.com');

-- Añadir "Crimen y castigo" como leído
INSERT INTO usuarios_libros (usuario_id, libro_id, estado_lectura, es_favorito, fecha_adicion, calificacion, notas) VALUES
(@xabier_id, 1, 'leido', 0, UNIX_TIMESTAMP() * 1000, 8.0, 'Una obra maestra de la literatura rusa que explora la culpa y la redención.');

-- Añadir "El idiota" como leyendo actualmente
INSERT INTO usuarios_libros (usuario_id, libro_id, estado_lectura, es_favorito, fecha_adicion, pagina_actual, fecha_inicio_lectura) VALUES
(@xabier_id, 3, 'leyendo', 0, UNIX_TIMESTAMP() * 1000, 250, UNIX_TIMESTAMP() * 1000);

-- Añadir "Los demonios" como pendiente por leer
INSERT INTO usuarios_libros (usuario_id, libro_id, estado_lectura, es_favorito, fecha_adicion) VALUES
(@xabier_id, 6, 'por_leer', 1, UNIX_TIMESTAMP() * 1000);

-- Añadir "Noches blancas" como leído
INSERT INTO usuarios_libros (usuario_id, libro_id, estado_lectura, es_favorito, fecha_adicion, calificacion, fecha_fin_lectura, notas) VALUES
(@xabier_id, 9, 'leido', 1, UNIX_TIMESTAMP() * 1000, 10.0, UNIX_TIMESTAMP() * 1000, 'Mi obra favorita de Dostoievski. Una historia preciosa sobre el amor y la soledad.');