<?php
// Incluir archivo de configuración y clase FirebaseAdmin
require_once 'config.php';
require_once 'FirebaseAdmin.php';

// Definir variables para mensajes y resultados
$message = '';
$success = false;

// Procesar el formulario cuando se envía
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // Obtener los datos del formulario
    $title = isset($_POST['title']) ? trim($_POST['title']) : '';
    $body = isset($_POST['body']) ? trim($_POST['body']) : '';
    
    // Validar que los campos no estén vacíos
    if (empty($title) || empty($body)) {
        $message = "Por favor, completa todos los campos";
    } else {
        try {
            // Inicializar Firebase Admin con el archivo de credenciales
            $firebase = new FirebaseAdmin(FCM_CREDENTIALS_FILE);
            
            // Crear datos de notificación
            $notification = [
                'title' => $title,
                'body' => $body
            ];
            
            // Datos adicionales para la notificación
            $data = [
                'click_action' => 'FLUTTER_NOTIFICATION_CLICK',
                'screen' => 'main',
                'type' => 'general_notification'
            ];
            
            // Enviar notificación al tema
            $response = $firebase->sendToTopic(FCM_TOPIC, $notification, $data);
            
            // Verificar respuesta exitosa (normalmente, si no hay excepción, es exitoso)
            $message = "Notificación enviada exitosamente";
            $success = true;
            
        } catch (Exception $e) {
            $message = "Error al enviar la notificación: " . $e->getMessage();
        }
    }
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel de Notificaciones LibreBook</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f8f9fa;
            color: #333;
        }
        .container {
            max-width: 800px;
            margin: 50px auto;
            padding: 30px;
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
        }
        h1 {
            color: #6200EE;
            text-align: center;
            margin-bottom: 30px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
        }
        input[type="text"],
        textarea {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
            font-size: 16px;
        }
        textarea {
            min-height: 150px;
            resize: vertical;
        }
        button {
            background-color: #6200EE;
            color: white;
            border: none;
            padding: 12px 20px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            display: block;
            width: 100%;
            transition: background-color 0.3s;
        }
        button:hover {
            background-color: #3700B3;
        }
        .alert {
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        .alert-success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .alert-error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .footer {
            text-align: center;
            margin-top: 30px;
            color: #777;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Panel de Notificaciones LibreBook</h1>
        
        <?php if (!empty($message)): ?>
            <div class="alert <?php echo $success ? 'alert-success' : 'alert-error'; ?>">
                <?php echo $message; ?>
            </div>
        <?php endif; ?>
        
        <form method="post" action="<?php echo htmlspecialchars($_SERVER["PHP_SELF"]); ?>">
            <div class="form-group">
                <label for="title">Título de la notificación:</label>
                <input type="text" id="title" name="title" placeholder="Ej: Nueva actualización disponible" required>
            </div>
            
            <div class="form-group">
                <label for="body">Mensaje de la notificación:</label>
                <textarea id="body" name="body" placeholder="Ej: Hemos lanzado una nueva actualización con funciones increíbles. ¡Actualiza ahora!" required></textarea>
            </div>
            
            <button type="submit">Enviar notificación a todos los dispositivos</button>
        </form>
        
        <div class="footer">
            <p>Panel de administración de LibreBook &copy; <?php echo date("Y"); ?></p>
        </div>
    </div>
</body>
</html>