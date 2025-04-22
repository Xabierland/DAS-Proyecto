<?php
/**
 * Clase para manejar la comunicación con Firebase Admin SDK
 */
class FirebaseAdmin {
    private $serviceAccount;
    private $accessToken;
    private $tokenExpiry;
    
    /**
     * Constructor
     * @param string $credentialsFile Ruta al archivo JSON de credenciales de Firebase
     */
    public function __construct($credentialsFile) {
        if (!file_exists($credentialsFile)) {
            throw new Exception("El archivo de credenciales no existe: $credentialsFile");
        }
        
        $this->serviceAccount = json_decode(file_get_contents($credentialsFile), true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception("Error al decodificar el archivo de credenciales: " . json_last_error_msg());
        }
        
        // Inicializar token como null, se generará cuando se necesite
        $this->accessToken = null;
        $this->tokenExpiry = 0;
    }
    
    /**
     * Obtiene un token de acceso OAuth 2.0 para la cuenta de servicio
     * @return string Token de acceso
     */
    public function getAccessToken() {
        // Si ya tenemos un token válido, devolverlo
        if ($this->accessToken !== null && time() < $this->tokenExpiry) {
            return $this->accessToken;
        }
        
        // Generar un nuevo token JWT
        $jwtToken = $this->createJwtToken();
        
        // Intercambiar el JWT por un token de acceso
        $response = $this->exchangeJwtForAccessToken($jwtToken);
        
        // Guardar el token y su tiempo de expiración
        $this->accessToken = $response['access_token'];
        $this->tokenExpiry = time() + $response['expires_in'] - 300; // Restar 5 minutos para tener margen
        
        return $this->accessToken;
    }
    
    /**
     * Crea un token JWT para autenticar con Google
     * @return string Token JWT firmado
     */
    private function createJwtToken() {
        $header = [
            'alg' => 'RS256',
            'typ' => 'JWT',
            'kid' => $this->serviceAccount['private_key_id']
        ];
        
        $time = time();
        $payload = [
            'iss' => $this->serviceAccount['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => 'https://oauth2.googleapis.com/token',
            'exp' => $time + 3600,
            'iat' => $time
        ];
        
        $base64Header = $this->base64UrlEncode(json_encode($header));
        $base64Payload = $this->base64UrlEncode(json_encode($payload));
        
        $dataToSign = "$base64Header.$base64Payload";
        $signature = '';
        
        // Firmar con la clave privada
        $privateKey = $this->serviceAccount['private_key'];
        openssl_sign($dataToSign, $signature, $privateKey, 'SHA256');
        
        $base64Signature = $this->base64UrlEncode($signature);
        
        return "$base64Header.$base64Payload.$base64Signature";
    }
    
    /**
     * Intercambia un token JWT por un token de acceso OAuth 2.0
     * @param string $jwtToken Token JWT firmado
     * @return array Respuesta con token de acceso y tiempo de expiración
     */
    private function exchangeJwtForAccessToken($jwtToken) {
        $url = 'https://oauth2.googleapis.com/token';
        $data = [
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwtToken
        ];
        
        $options = [
            'http' => [
                'header' => "Content-type: application/x-www-form-urlencoded\r\n",
                'method' => 'POST',
                'content' => http_build_query($data)
            ]
        ];
        
        $context = stream_context_create($options);
        $result = file_get_contents($url, false, $context);
        
        if ($result === false) {
            throw new Exception("Error al obtener el token de acceso");
        }
        
        $response = json_decode($result, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception("Error al decodificar la respuesta del token: " . json_last_error_msg());
        }
        
        if (!isset($response['access_token'])) {
            throw new Exception("No se encontró el token de acceso en la respuesta: " . $result);
        }
        
        return $response;
    }
    
    /**
     * Envía una notificación FCM a un tema
     * @param string $topic Tema al que enviar la notificación
     * @param array $notification Datos de la notificación (título, cuerpo, etc.)
     * @param array $data Datos adicionales para la notificación
     * @return array Respuesta del servidor FCM
     */
    public function sendToTopic($topic, $notification, $data = []) {
        $accessToken = $this->getAccessToken();
        
        $url = 'https://fcm.googleapis.com/v1/projects/' . $this->serviceAccount['project_id'] . '/messages:send';
        
        $message = [
            'message' => [
                'topic' => $topic,
                'notification' => $notification,
                'data' => $data
            ]
        ];
        
        $options = [
            'http' => [
                'header' => "Content-type: application/json\r\n" .
                            "Authorization: Bearer $accessToken\r\n",
                'method' => 'POST',
                'content' => json_encode($message)
            ]
        ];
        
        $context = stream_context_create($options);
        $result = file_get_contents($url, false, $context);
        
        if ($result === false) {
            throw new Exception("Error al enviar la notificación");
        }
        
        return json_decode($result, true);
    }
    
    /**
     * Codifica una cadena en formato base64url (para JWT)
     * @param string $data Datos a codificar
     * @return string Datos codificados en base64url
     */
    private function base64UrlEncode($data) {
        $base64 = base64_encode($data);
        $base64Url = strtr($base64, '+/', '-_');
        return rtrim($base64Url, '=');
    }
}
?>