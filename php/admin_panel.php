<?php
// Incluir archivo de configuración
require_once 'config.php';

// Definir variables para mensajes y resultados
$message = '';
$success = false;
$notification_history = [];

// Cargar historial de notificaciones si existe
$history_file = 'notification_history.json';
if (file_exists($history_file)) {
    $notification_history = json_decode(file_get_contents($history_file), true) ?: [];
}

// Procesar el formulario cuando se envía
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // Obtener los datos del formulario
    $title = isset($_POST['title']) ? trim($_POST['title']) : '';
    $body = isset($_POST['body']) ? trim($_POST['body']) : '';
    
    // Validar que los campos no estén vacíos
    if (empty($title) || empty($body)) {
        $message = "Por favor, completa todos los campos";
    } else {
        // Datos para la API de FCM
        $serverKey = FCM_SERVER_KEY;
        $topic = FCM_TOPIC;
        
        // Crear payload para FCM
        $notification = [
            'title' => $title,
            'body' => $body,
            'sound' => 'default',
            'badge' => '1'
        ];
        
        $data = [
            'click_action' => 'FLUTTER_NOTIFICATION_CLICK',
            'screen' => 'main',
            'type' => 'general_notification'
        ];
        
        $fields = [
            'to' => '/topics/' . $topic,
            'notification' => $notification,
            'data' => $data,
            'priority' => 'high'
        ];
        
        // Convertir a JSON
        $payload = json_encode($fields);
        
        // URL de la API de FCM
        $url = 'https://fcm.googleapis.com/fcm/send';
        
        // Inicializar cURL
        $curl = curl_init();
        curl_setopt($curl, CURLOPT_URL, $url);
        curl_setopt($curl, CURLOPT_POST, true);
        curl_setopt($curl, CURLOPT_HTTPHEADER, [
            'Authorization: key=' . $serverKey,
            'Content-Type: application/json'
        ]);
        curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($curl, CURLOPT_POSTFIELDS, $payload);
        
        // Ejecutar la solicitud
        $result = curl_exec($curl);
        
        // Verificar errores
        if ($result === false) {
            $message = 'Error en cURL: ' . curl_error($curl);
        } else {
            // Decodificar respuesta
            $response = json_decode($result, true);
            
            // Verificar respuesta
            if (isset($response['message_id']) || (isset($response['success']) && $response['success'] > 0)) {
                $message = "Notificación enviada exitosamente";
                $success = true;
                
                // Guardar en el historial
                $notification_history[] = [
                    'title' => $title,
                    'body' => $body,
                    'date' => date('Y-m-d H:i:s'),
                    'status' => 'success'
                ];
                
                // Guardar historial en archivo
                file_put_contents($history_file, json_encode($notification_history));
            } else {
                $message = "Error al enviar la notificación: " . (isset($response['error']) ? $response['error'] : $result);
                
                // Guardar en el historial (error)
                $notification_history[] = [
                    'title' => $title,
                    'body' => $body,
                    'date' => date('Y-m-d H:i:s'),
                    'status' => 'error',
                    'error_message' => isset($response['error']) ? $response['error'] : $result
                ];
                
                // Guardar historial en archivo
                file_put_contents($history_file, json_encode($notification_history));
            }
        }
        
        // Cerrar cURL
        curl_close($curl);
    }
}

// Limitar el historial a las últimas 10 notificaciones
$notification_history = array_slice($notification_history, -10);
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel de Administración - <?php echo APP_NAME; ?></title>
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
        .header {
            text-align: center;
            margin-bottom: 30px;
        }
        .header img {
            max-width: 200px;
            margin-bottom: 20px;
        }
        h1 {
            color: #6200EE;
            margin-bottom: 10px;
        }
        h2 {
            color: #3700B3;
            margin-top: 40px;
            margin-bottom: 20px;
            border-bottom: 1px solid #eee;
            padding-bottom: 10px;
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
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        th {
            background-color: #f2f2f2;
            font-weight: 600;
        }
        tr:hover {
            background-color: #f9f9f9;
        }
        .status-success {
            color: #28a745;
            font-weight: 600;
        }
        .status-error {
            color: #dc3545;
            font-weight: 600;
        }
        .badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
        }
        .badge-success {
            background-color: #28a745;
            color: white;
        }
        .badge-error {
            background-color: #dc3545;
            color: white;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Panel de Administración de Notificaciones</h1>
            <p>Envía notificaciones push a todos los usuarios de <?php echo APP_NAME; ?></p>
        </div>
        
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
        
        <?php if (!empty($notification_history)): ?>
            <h2>Historial de notificaciones recientes</h2>
            <table>
                <thead>
                    <tr>
                        <th>Fecha</th>
                        <th>Título</th>
                        <th>Mensaje</th>
                        <th>Estado</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach (array_reverse($notification_history) as $notification): ?>
                        <tr>
                            <td><?php echo $notification['date']; ?></td>
                            <td><?php echo htmlspecialchars($notification['title']); ?></td>
                            <td><?php echo htmlspecialchars(substr($notification['body'], 0, 50)) . (strlen($notification['body']) > 50 ? '...' : ''); ?></td>
                            <td>
                                <?php if ($notification['status'] === 'success'): ?>
                                    <span class="badge badge-success">Enviado</span>
                                <?php else: ?>
                                    <span class="badge badge-error">Error</span>
                                <?php endif; ?>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        <?php endif; ?>
        
        <div class="footer">
            <p>Panel de administración de <?php echo APP_NAME; ?> &copy; <?php echo date("Y"); ?></p>
            <p>Contacto: <?php echo ADMIN_EMAIL; ?></p>
        </div>
    </div>
</body>
</html>