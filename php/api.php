<?php
// api.php - API Gateway para LibreBook

// Opcional: Descomentar para depuración
// file_put_contents('debug.log', 
//     "REQUEST_URI: " . $_SERVER['REQUEST_URI'] . "\n" .
//     "QUERY_STRING: " . $_SERVER['QUERY_STRING'] . "\n" .
//     "GET: " . print_r($_GET, true) . "\n\n", 
//     FILE_APPEND);

header('Content-Type: application/json');

// Configuración de la base de datos
$servername = "localhost";
$username = "Xxgabina001"; // Reemplazar con tu usuario de MySQL
$password = "BPKrbjdfP"; // Reemplazar con tu contraseña
$dbname = "Xxgabina001_librebook_db";

// Crear conexión
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    sendResponse(500, ["error" => "Error de conexión: " . $conn->connect_error]);
    exit;
}

// Establecer charset
$conn->set_charset("utf8");

// Obtener método HTTP y ruta
$method = $_SERVER['REQUEST_METHOD'];
$request = $_SERVER['REQUEST_URI'];

// Determinar endpoint
$endpoint = '';
$params = [];

// Método 1: Comprobar parámetro de consulta 'endpoint'
if (isset($_GET['endpoint'])) {
    $endpoint = $_GET['endpoint'];
    // Preparar $params para uso posterior
    $params = [$endpoint];
    if (isset($_GET['id'])) {
        $params[] = $_GET['id'];
    }
} 
// Método 2: Extraer endpoint de la URL (api.php/usuarios)
else {
    if (preg_match('#/api\.php/([^?]*)#', $request, $matches)) {
        $path = $matches[1];
        $params = explode('/', trim($path, '/'));
        $endpoint = isset($params[0]) && !empty($params[0]) ? $params[0] : '';
    }
}

// Si no hay endpoint, mostrar error
if (empty($endpoint)) {
    sendResponse(404, ["error" => "Endpoint no encontrado"]);
    exit;
}

// Rutas API
switch ($endpoint) {
    case 'libros':
        handleLibros($method, $params, $conn);
        break;
    case 'usuarios':
        handleUsuarios($method, $params, $conn);
        break;
    case 'biblioteca':
        handleBiblioteca($method, $params, $conn);
        break;
    default:
        sendResponse(404, ["error" => "Endpoint no encontrado"]);
        break;
}

// Manejador de libros
function handleLibros($method, $params, $conn) {
    $id = isset($params[1]) ? $params[1] : null;
    
    switch ($method) {
        case 'GET':
            if ($id) {
                // Obtener libro por ID
                $stmt = $conn->prepare("SELECT * FROM libros WHERE id = ?");
                $stmt->bind_param("i", $id);
                $stmt->execute();
                $result = $stmt->get_result();
                $libro = $result->fetch_assoc();
                
                if ($libro) {
                    sendResponse(200, $libro);
                } else {
                    sendResponse(404, ["error" => "Libro no encontrado"]);
                }
                $stmt->close();
            } else {
                // Filtrar por parámetros si existen
                if (isset($_GET['busqueda'])) {
                    // Buscar libros por título o autor
                    $busqueda = "%" . $_GET['busqueda'] . "%";
                    $stmt = $conn->prepare("SELECT * FROM libros WHERE titulo LIKE ? OR autor LIKE ?");
                    $stmt->bind_param("ss", $busqueda, $busqueda);
                } elseif (isset($_GET['genero'])) {
                    // Buscar libros por género
                    $genero = $_GET['genero'];
                    $stmt = $conn->prepare("SELECT * FROM libros WHERE genero = ?");
                    $stmt->bind_param("s", $genero);
                } elseif (isset($_GET['autor'])) {
                    // Buscar libros por autor
                    $autor = $_GET['autor'];
                    $stmt = $conn->prepare("SELECT * FROM libros WHERE autor = ?");
                    $stmt->bind_param("s", $autor);
                } elseif (isset($_GET['isbn'])) {
                    // Buscar libro por ISBN
                    $isbn = $_GET['isbn'];
                    $stmt = $conn->prepare("SELECT * FROM libros WHERE isbn = ?");
                    $stmt->bind_param("s", $isbn);
                } else {
                    // Obtener todos los libros
                    $stmt = $conn->prepare("SELECT * FROM libros");
                }
                
                $stmt->execute();
                $result = $stmt->get_result();
                $libros = [];
                
                while ($row = $result->fetch_assoc()) {
                    $libros[] = $row;
                }
                
                sendResponse(200, $libros);
                $stmt->close();
            }
            break;
            
        case 'POST':
            // Crear nuevo libro
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['titulo']) || !isset($data['autor'])) {
                sendResponse(400, ["error" => "Faltan campos requeridos"]);
                break;
            }
            
            $stmt = $conn->prepare("INSERT INTO libros (titulo, autor, isbn, descripcion, portada_url, anio_publicacion, editorial, genero, num_paginas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->bind_param("sssssisii", 
                $data['titulo'],
                $data['autor'],
                $data['isbn'] ?? '',
                $data['descripcion'] ?? '',
                $data['portadaUrl'] ?? '',
                $data['anioPublicacion'] ?? 0,
                $data['editorial'] ?? '',
                $data['genero'] ?? '',
                $data['numPaginas'] ?? 0
            );
            
            if ($stmt->execute()) {
                $id = $conn->insert_id;
                sendResponse(201, ["id" => $id, "mensaje" => "Libro creado con éxito"]);
            } else {
                sendResponse(500, ["error" => "Error al crear libro: " . $stmt->error]);
            }
            $stmt->close();
            break;
            
        case 'PUT':
            // Actualizar libro existente
            if (!$id) {
                sendResponse(400, ["error" => "Se requiere ID para actualizar"]);
                break;
            }
            
            $data = json_decode(file_get_contents('php://input'), true);
            
            // Verificar que el libro existe
            $stmt = $conn->prepare("SELECT id FROM libros WHERE id = ?");
            $stmt->bind_param("i", $id);
            $stmt->execute();
            $result = $stmt->get_result();
            
            if ($result->num_rows === 0) {
                sendResponse(404, ["error" => "Libro no encontrado"]);
                $stmt->close();
                break;
            }
            $stmt->close();
            
            // Preparar actualización
            $stmt = $conn->prepare("UPDATE libros SET titulo = ?, autor = ?, isbn = ?, descripcion = ?, portada_url = ?, anio_publicacion = ?, editorial = ?, genero = ?, num_paginas = ? WHERE id = ?");
            $stmt->bind_param("sssssissii", 
                $data['titulo'],
                $data['autor'],
                $data['isbn'] ?? '',
                $data['descripcion'] ?? '',
                $data['portadaUrl'] ?? '',
                $data['anioPublicacion'] ?? 0,
                $data['editorial'] ?? '',
                $data['genero'] ?? '',
                $data['numPaginas'] ?? 0,
                $id
            );
            
            if ($stmt->execute()) {
                sendResponse(200, ["mensaje" => "Libro actualizado con éxito"]);
            } else {
                sendResponse(500, ["error" => "Error al actualizar libro: " . $stmt->error]);
            }
            $stmt->close();
            break;
            
        case 'DELETE':
            // Eliminar libro
            if (!$id) {
                sendResponse(400, ["error" => "Se requiere ID para eliminar"]);
                break;
            }
            
            $stmt = $conn->prepare("DELETE FROM libros WHERE id = ?");
            $stmt->bind_param("i", $id);
            
            if ($stmt->execute()) {
                if ($stmt->affected_rows > 0) {
                    sendResponse(200, ["mensaje" => "Libro eliminado con éxito"]);
                } else {
                    sendResponse(404, ["error" => "Libro no encontrado"]);
                }
            } else {
                sendResponse(500, ["error" => "Error al eliminar libro: " . $stmt->error]);
            }
            $stmt->close();
            break;
            
        default:
            sendResponse(405, ["error" => "Método no permitido"]);
            break;
    }
}

// Manejador de usuarios
function handleUsuarios($method, $params, $conn) {
    $id = isset($params[1]) ? $params[1] : null;
    
    switch ($method) {
        case 'GET':
            if ($id) {
                // Obtener usuario por ID
                $stmt = $conn->prepare("SELECT id, nombre, email, foto_perfil, fecha_registro FROM usuarios WHERE id = ?");
                $stmt->bind_param("i", $id);
                $stmt->execute();
                $result = $stmt->get_result();
                $usuario = $result->fetch_assoc();
                
                if ($usuario) {
                    // Convertir BLOB a base64 si existe
                    if ($usuario['foto_perfil'] !== null) {
                        $usuario['foto_perfil'] = base64_encode($usuario['foto_perfil']);
                    }
                    sendResponse(200, $usuario);
                } else {
                    sendResponse(404, ["error" => "Usuario no encontrado"]);
                }
                $stmt->close();
            } else if (isset($_GET['email'])) {
                // Obtener usuario por email
                $email = $_GET['email'];
                $stmt = $conn->prepare("SELECT id, nombre, email, foto_perfil, fecha_registro FROM usuarios WHERE email = ?");
                $stmt->bind_param("s", $email);
                $stmt->execute();
                $result = $stmt->get_result();
                $usuario = $result->fetch_assoc();
                
                if ($usuario) {
                    // Convertir BLOB a base64 si existe
                    if ($usuario['foto_perfil'] !== null) {
                        $usuario['foto_perfil'] = base64_encode($usuario['foto_perfil']);
                    }
                    sendResponse(200, $usuario);
                } else {
                    sendResponse(404, ["error" => "Usuario no encontrado"]);
                }
                $stmt->close();
            } else if (isset($_GET['busqueda'])) {
                // Buscar usuarios por nombre o email
                $busqueda = "%" . $_GET['busqueda'] . "%";
                $stmt = $conn->prepare("SELECT id, nombre, email, foto_perfil, fecha_registro FROM usuarios WHERE nombre LIKE ? OR email LIKE ?");
                $stmt->bind_param("ss", $busqueda, $busqueda);
                $stmt->execute();
                $result = $stmt->get_result();
                $usuarios = [];
                
                while ($row = $result->fetch_assoc()) {
                    // Convertir BLOB a base64 si existe
                    if ($row['foto_perfil'] !== null) {
                        $row['foto_perfil'] = base64_encode($row['foto_perfil']);
                    }
                    $usuarios[] = $row;
                }
                
                sendResponse(200, $usuarios);
                $stmt->close();
            } else {
                // Obtener todos los usuarios
                $stmt = $conn->prepare("SELECT id, nombre, email, foto_perfil, fecha_registro FROM usuarios");
                $stmt->execute();
                $result = $stmt->get_result();
                $usuarios = [];
                
                while ($row = $result->fetch_assoc()) {
                    // Convertir BLOB a base64 si existe
                    if ($row['foto_perfil'] !== null) {
                        $row['foto_perfil'] = base64_encode($row['foto_perfil']);
                    }
                    $usuarios[] = $row;
                }
                
                sendResponse(200, $usuarios);
                $stmt->close();
            }
            break;
            
        case 'POST':
            if (isset($params[1]) && $params[1] == 'autenticar') {
                // Autenticar usuario
                $data = json_decode(file_get_contents('php://input'), true);
                
                if (!isset($data['email']) || !isset($data['password'])) {
                    sendResponse(400, ["error" => "Faltan campos requeridos"]);
                    break;
                }
                
                $stmt = $conn->prepare("SELECT id FROM usuarios WHERE email = ? AND password = ?");
                $stmt->bind_param("ss", $data['email'], $data['password']);
                $stmt->execute();
                $result = $stmt->get_result();
                
                if ($result->num_rows > 0) {
                    $usuario = $result->fetch_assoc();
                    sendResponse(200, ["id" => $usuario['id']]);
                } else {
                    sendResponse(401, ["error" => "Credenciales incorrectas"]);
                }
                $stmt->close();
            } else {
                // Registrar nuevo usuario
                $data = json_decode(file_get_contents('php://input'), true);
                
                if (!isset($data['nombre']) || !isset($data['email']) || !isset($data['password'])) {
                    sendResponse(400, ["error" => "Faltan campos requeridos"]);
                    break;
                }
                
                // Verificar si el email ya existe
                $stmt = $conn->prepare("SELECT id FROM usuarios WHERE email = ?");
                $stmt->bind_param("s", $data['email']);
                $stmt->execute();
                $result = $stmt->get_result();
                
                if ($result->num_rows > 0) {
                    sendResponse(409, ["error" => "El email ya está en uso"]);
                    $stmt->close();
                    break;
                }
                $stmt->close();
                
                // Preparar la imagen si existe
                $fotoPerfilData = null;
                if (isset($data['fotoPerfil']) && !empty($data['fotoPerfil'])) {
                    // Decodificar la imagen base64
                    $fotoPerfilData = base64_decode($data['fotoPerfil']);
                }
                
                // Crear usuario con la imagen BLOB
                $stmt = $conn->prepare("INSERT INTO usuarios (nombre, email, password, foto_perfil, fecha_registro) VALUES (?, ?, ?, ?, ?)");
                $fechaRegistro = time() * 1000; // Convertir a milisegundos como en la app
                $stmt->bind_param("sssbi", 
                    $data['nombre'],
                    $data['email'],
                    $data['password'],
                    $fotoPerfilData,
                    $fechaRegistro
                );
                
                if ($stmt->execute()) {
                    $id = $conn->insert_id;
                    sendResponse(201, ["id" => $id, "mensaje" => "Usuario registrado con éxito"]);
                } else {
                    sendResponse(500, ["error" => "Error al registrar usuario: " . $stmt->error]);
                }
                $stmt->close();
            }
            break;
            
            case 'PUT':
                // Actualizar usuario
                if (!$id) {
                    sendResponse(400, ["error" => "Se requiere ID para actualizar"]);
                    break;
                }
                
                $data = json_decode(file_get_contents('php://input'), true);
                
                // Verificar que el usuario existe
                $stmt = $conn->prepare("SELECT id FROM usuarios WHERE id = ?");
                $stmt->bind_param("i", $id);
                $stmt->execute();
                $result = $stmt->get_result();
                
                if ($result->num_rows === 0) {
                    sendResponse(404, ["error" => "Usuario no encontrado"]);
                    $stmt->close();
                    break;
                }
                $stmt->close();
                
                // Verificar si hay imagen
                $hasImage = isset($data['fotoPerfil']) && !empty($data['fotoPerfil']);
                
                if ($hasImage) {
                    // Decodificar la imagen base64
                    $fotoPerfilData = base64_decode($data['fotoPerfil']);
                    
                    // Convertir a formato hexadecimal para UNHEX
                    $hexData = bin2hex($fotoPerfilData);
                    
                    // Construir consulta con UNHEX para datos binarios
                    $query = "UPDATE usuarios SET nombre = ?, email = ?, foto_perfil = UNHEX(?) WHERE id = ?";
                    $stmt = $conn->prepare($query);
                    $stmt->bind_param("sssi", 
                        $data['nombre'],
                        $data['email'],
                        $hexData,
                        $id
                    );
                } else {
                    // Si no hay nueva imagen, no actualizamos ese campo
                    $stmt = $conn->prepare("UPDATE usuarios SET nombre = ?, email = ? WHERE id = ?");
                    $stmt->bind_param("ssi", 
                        $data['nombre'],
                        $data['email'],
                        $id
                    );
                }
                
                if ($stmt->execute()) {
                    sendResponse(200, ["mensaje" => "Usuario actualizado con éxito"]);
                } else {
                    sendResponse(500, ["error" => "Error al actualizar usuario: " . $stmt->error]);
                }
                $stmt->close();
                break;
            
        case 'DELETE':
            // Eliminar usuario
            if (!$id) {
                sendResponse(400, ["error" => "Se requiere ID para eliminar"]);
                break;
            }
            
            $stmt = $conn->prepare("DELETE FROM usuarios WHERE id = ?");
            $stmt->bind_param("i", $id);
            
            if ($stmt->execute()) {
                if ($stmt->affected_rows > 0) {
                    sendResponse(200, ["mensaje" => "Usuario eliminado con éxito"]);
                } else {
                    sendResponse(404, ["error" => "Usuario no encontrado"]);
                }
            } else {
                sendResponse(500, ["error" => "Error al eliminar usuario: " . $stmt->error]);
            }
            $stmt->close();
            break;
            
        default:
            sendResponse(405, ["error" => "Método no permitido"]);
            break;
    }
}

// Manejador de biblioteca (relación usuario-libro)
function handleBiblioteca($method, $params, $conn) {
    $usuarioId = isset($params[1]) ? $params[1] : null;
    $libroId = isset($params[2]) ? $params[2] : null;
    
    switch ($method) {
        case 'GET':
            if ($usuarioId && $libroId) {
                // Obtener un libro específico de la biblioteca del usuario
                $stmt = $conn->prepare(
                    "SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas
                    FROM libros l
                    INNER JOIN usuarios_libros ul ON l.id = ul.libro_id
                    WHERE ul.usuario_id = ? AND l.id = ?"
                );
                $stmt->bind_param("ii", $usuarioId, $libroId);
                $stmt->execute();
                $result = $stmt->get_result();
                $libro = $result->fetch_assoc();
                
                if ($libro) {
                    sendResponse(200, $libro);
                } else {
                    sendResponse(404, ["error" => "Libro no encontrado en la biblioteca del usuario"]);
                }
                $stmt->close();
            } else if ($usuarioId) {
                // Filtrar por estado si se proporciona
                if (isset($_GET['estado'])) {
                    $estado = $_GET['estado'];
                    $stmt = $conn->prepare(
                        "SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas
                        FROM libros l
                        INNER JOIN usuarios_libros ul ON l.id = ul.libro_id
                        WHERE ul.usuario_id = ? AND ul.estado_lectura = ?"
                    );
                    $stmt->bind_param("is", $usuarioId, $estado);
                } else if (isset($_GET['favoritos']) && $_GET['favoritos'] == 1) {
                    // Obtener libros favoritos
                    $stmt = $conn->prepare(
                        "SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas
                        FROM libros l
                        INNER JOIN usuarios_libros ul ON l.id = ul.libro_id
                        WHERE ul.usuario_id = ? AND ul.es_favorito = 1"
                    );
                    $stmt->bind_param("i", $usuarioId);
                } else {
                    // Obtener todos los libros del usuario
                    $stmt = $conn->prepare(
                        "SELECT l.*, ul.estado_lectura, ul.es_favorito, ul.calificacion, ul.pagina_actual, ul.notas
                        FROM libros l
                        INNER JOIN usuarios_libros ul ON l.id = ul.libro_id
                        WHERE ul.usuario_id = ?"
                    );
                    $stmt->bind_param("i", $usuarioId);
                }
                
                $stmt->execute();
                $result = $stmt->get_result();
                $libros = [];
                
                while ($row = $result->fetch_assoc()) {
                    $libros[] = $row;
                }
                
                sendResponse(200, $libros);
                $stmt->close();
            } else {
                sendResponse(400, ["error" => "Se requiere ID de usuario"]);
            }
            break;
            
        case 'POST':
            // Agregar libro a la biblioteca
            if (!$usuarioId) {
                sendResponse(400, ["error" => "Se requiere ID de usuario"]);
                break;
            }
            
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['libroId'])) {
                sendResponse(400, ["error" => "Se requiere ID de libro"]);
                break;
            }
            
            if (!isset($data['estadoLectura'])) {
                $data['estadoLectura'] = 'por_leer'; // Valor por defecto
            }
            
            // Verificar si ya existe
            $stmt = $conn->prepare("SELECT id FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ?");
            $libroIdPost = $data['libroId'];
            $stmt->bind_param("ii", $usuarioId, $libroIdPost);
            $stmt->execute();
            $result = $stmt->get_result();
            
            if ($result->num_rows > 0) {
                sendResponse(409, ["error" => "El libro ya está en la biblioteca del usuario"]);
                $stmt->close();
                break;
            }
            $stmt->close();
            
            // Agregar a biblioteca
            $stmt = $conn->prepare("INSERT INTO usuarios_libros (usuario_id, libro_id, estado_lectura, es_favorito, fecha_adicion) VALUES (?, ?, ?, ?, ?)");
            $esFavorito = isset($data['esFavorito']) ? $data['esFavorito'] : 0;
            $fechaAdicion = time() * 1000; // Convertir a milisegundos
            $stmt->bind_param("iisii", 
                $usuarioId,
                $data['libroId'],
                $data['estadoLectura'],
                $esFavorito,
                $fechaAdicion
            );
            
            if ($stmt->execute()) {
                $id = $conn->insert_id;
                
                // Si hay calificación, actualizar
                if (isset($data['calificacion']) && $data['calificacion'] > 0) {
                    $stmtUpdate = $conn->prepare("UPDATE usuarios_libros SET calificacion = ? WHERE id = ?");
                    $stmtUpdate->bind_param("di", $data['calificacion'], $id);
                    $stmtUpdate->execute();
                    $stmtUpdate->close();
                }
                
                // Si hay página actual, actualizar
                if (isset($data['paginaActual']) && $data['paginaActual'] > 0) {
                    $stmtUpdate = $conn->prepare("UPDATE usuarios_libros SET pagina_actual = ? WHERE id = ?");
                    $stmtUpdate->bind_param("ii", $data['paginaActual'], $id);
                    $stmtUpdate->execute();
                    $stmtUpdate->close();
                }
                
                // Si hay notas, actualizar
                if (isset($data['notas']) && !empty($data['notas'])) {
                    $stmtUpdate = $conn->prepare("UPDATE usuarios_libros SET notas = ? WHERE id = ?");
                    $stmtUpdate->bind_param("si", $data['notas'], $id);
                    $stmtUpdate->execute();
                    $stmtUpdate->close();
                }
                
                // Actualizar fechas según estado
                if ($data['estadoLectura'] == 'leyendo') {
                    $stmtUpdate = $conn->prepare("UPDATE usuarios_libros SET fecha_inicio_lectura = ? WHERE id = ?");
                    $fechaActual = time() * 1000;
                    $stmtUpdate->bind_param("ii", $fechaActual, $id);
                    $stmtUpdate->execute();
                    $stmtUpdate->close();
                } else if ($data['estadoLectura'] == 'leido') {
                    $stmtUpdate = $conn->prepare("UPDATE usuarios_libros SET fecha_fin_lectura = ? WHERE id = ?");
                    $fechaActual = time() * 1000;
                    $stmtUpdate->bind_param("ii", $fechaActual, $id);
                    $stmtUpdate->execute();
                    $stmtUpdate->close();
                }
                
                sendResponse(201, ["id" => $id, "mensaje" => "Libro agregado a la biblioteca con éxito"]);
            } else {
                sendResponse(500, ["error" => "Error al agregar libro a la biblioteca: " . $stmt->error]);
            }
            $stmt->close();
            break;
            
        case 'PUT':
            // Actualizar libro en biblioteca
            if (!$usuarioId || !$libroId) {
                sendResponse(400, ["error" => "Se requiere ID de usuario y libro"]);
                break;
            }
            
            $data = json_decode(file_get_contents('php://input'), true);
            
            // Verificar que existe
            $stmt = $conn->prepare("SELECT id FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ?");
            $stmt->bind_param("ii", $usuarioId, $libroId);
            $stmt->execute();
            $result = $stmt->get_result();
            
            if ($result->num_rows === 0) {
                sendResponse(404, ["error" => "Libro no encontrado en la biblioteca del usuario"]);
                $stmt->close();
                break;
            }
            $row = $result->fetch_assoc();
            $id = $row['id'];
            $stmt->close();
            
            // Construir actualización dinámica basada en campos proporcionados
            $updates = [];
            $types = "";
            $values = [];
            
            if (isset($data['estadoLectura'])) {
                $updates[] = "estado_lectura = ?";
                $types .= "s";
                $values[] = $data['estadoLectura'];
                
                // Actualizar fechas según estado
                if ($data['estadoLectura'] == 'leyendo') {
                    $updates[] = "fecha_inicio_lectura = ?";
                    $types .= "i";
                    $values[] = time() * 1000;
                } else if ($data['estadoLectura'] == 'leido') {
                    $updates[] = "fecha_fin_lectura = ?";
                    $types .= "i";
                    $values[] = time() * 1000;
                }
            }
            
            if (isset($data['esFavorito'])) {
                $updates[] = "es_favorito = ?";
                $types .= "i";
                $values[] = $data['esFavorito'] ? 1 : 0;
            }
            
            if (isset($data['calificacion'])) {
                $updates[] = "calificacion = ?";
                $types .= "d";
                $values[] = $data['calificacion'];
            }
            
            if (isset($data['paginaActual'])) {
                $updates[] = "pagina_actual = ?";
                $types .= "i";
                $values[] = $data['paginaActual'];
            }
            
            if (isset($data['notas'])) {
                $updates[] = "notas = ?";
                $types .= "s";
                $values[] = $data['notas'];
            }
            
            if (empty($updates)) {
                sendResponse(400, ["error" => "No se proporcionaron campos para actualizar"]);
                break;
            }
            
            // Preparar consulta
            $sql = "UPDATE usuarios_libros SET " . implode(", ", $updates) . " WHERE usuario_id = ? AND libro_id = ?";
            $stmt = $conn->prepare($sql);
            
            // Añadir los parámetros de usuario y libro
            $types .= "ii";
            $values[] = $usuarioId;
            $values[] = $libroId;
            
            // Usar callback para bind_param con array
            $stmt->bind_param($types, ...$values);
            
            if ($stmt->execute()) {
                sendResponse(200, ["mensaje" => "Biblioteca actualizada con éxito"]);
            } else {
                sendResponse(500, ["error" => "Error al actualizar biblioteca: " . $stmt->error]);
            }
            $stmt->close();
            break;
            
        case 'DELETE':
            // Eliminar libro de la biblioteca
            if (!$usuarioId || !$libroId) {
                sendResponse(400, ["error" => "Se requiere ID de usuario y libro"]);
                break;
            }
            
            $stmt = $conn->prepare("DELETE FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ?");
            $stmt->bind_param("ii", $usuarioId, $libroId);
            
            if ($stmt->execute()) {
                if ($stmt->affected_rows > 0) {
                    sendResponse(200, ["mensaje" => "Libro eliminado de la biblioteca con éxito"]);
                } else {
                    sendResponse(404, ["error" => "Libro no encontrado en la biblioteca del usuario"]);
                }
            } else {
                sendResponse(500, ["error" => "Error al eliminar libro de la biblioteca: " . $stmt->error]);
            }
            $stmt->close();
            break;
            
        default:
            sendResponse(405, ["error" => "Método no permitido"]);
            break;
    }
}

// Función para enviar respuestas JSON con código de estado
function sendResponse($statusCode, $data) {
    http_response_code($statusCode);
    echo json_encode($data);
    exit;
}

// Cerrar conexión
$conn->close();
?>