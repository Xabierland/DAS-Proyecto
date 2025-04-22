package com.xabierland.librebook.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.xabierland.librebook.R;
import com.xabierland.librebook.activities.MainActivity;

public class LibreBookMessagingService extends FirebaseMessagingService {

    private static final String TAG = "LibreBookFCM";
    private static final String CHANNEL_ID = "fcm_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "Mensaje recibido desde: " + remoteMessage.getFrom());

        // Verificar si el mensaje contiene datos
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Datos del mensaje: " + remoteMessage.getData());
        }

        // Verificar si el mensaje contiene una notificación
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Cuerpo de la notificación: " + remoteMessage.getNotification().getBody());
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Nuevo token FCM: " + token);
        // Enviar el token al servidor
        sendRegistrationToServer(token);
    }

    /**
     * Envía el token al servidor
     * En una implementación real, aquí se enviaría el token al servidor backend
     */
    private void sendRegistrationToServer(String token) {
        // Implementar si es necesario enviar el token a un servidor
        Log.d(TAG, "Token enviado al servidor: " + token);
    }

    /**
     * Crear y mostrar una notificación
     */
    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // Configurar el sonido de la notificación
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Crear el canal de notificación para Android 8.0 y superior
        createNotificationChannel();

        // Construir la notificación
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        // Obtener el NotificationManager
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Mostrar la notificación
        notificationManager.notify(0, notificationBuilder.build());
    }

    /**
     * Crear el canal de notificación (requerido para Android 8.0 y superior)
     */
    private void createNotificationChannel() {
        // Crear el canal de notificación solo en Android 8.0 y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            // Registrar el canal con el sistema
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}