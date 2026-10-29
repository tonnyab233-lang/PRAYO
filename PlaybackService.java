package com.prayo.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class PlaybackService extends Service {
    private static final String CHANNEL = "prayo_playback";
    private static final int NOTIFICATION_ID = 72;
    private static MediaPlayer player;
    private static String songTitle = "PRAYO", songArtist = "Tu música";

    public static void play(Context context, Uri uri, String title, String artist) {
        Intent i = new Intent(context, PlaybackService.class).setAction("PLAY").setData(uri)
                .putExtra("title", title).putExtra("artist", artist);
        start(context, i);
    }
    public static void toggle(Context context) { start(context, new Intent(context, PlaybackService.class).setAction("TOGGLE")); }
    public static boolean isPlaying() { return player != null && player.isPlaying(); }
    private static void start(Context c, Intent i) {
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    @Override public void onCreate() { super.onCreate(); createChannel(); }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "PLAY".equals(intent.getAction())) {
            songTitle = intent.getStringExtra("title"); songArtist = intent.getStringExtra("artist");
            startForeground(NOTIFICATION_ID, notification(true));
            try {
                if (player != null) { player.reset(); player.release(); }
                player = new MediaPlayer(); player.setDataSource(this, intent.getData());
                player.setOnCompletionListener(mp -> { stopForeground(true); stopSelf(); });
                player.setOnPreparedListener(mp -> { mp.start(); updateNotification(true); });
                player.prepareAsync();
            } catch (Exception e) { stopForeground(true); stopSelf(); }
        } else if (intent != null && "TOGGLE".equals(intent.getAction())) {
            if (player != null) {
                if (player.isPlaying()) player.pause(); else { try { player.start(); } catch (Exception ignored) {} }
                updateNotification(isPlaying());
            }
        }
        return START_NOT_STICKY;
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Reproducción de PRAYO", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Controles de música en segundo plano");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
    private Notification notification(boolean playing) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Intent toggle = new Intent(this, PlaybackService.class).setAction("TOGGLE");
        PendingIntent action = PendingIntent.getService(this, 2, toggle, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        return new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(songTitle == null ? "PRAYO" : songTitle).setContentText(songArtist == null ? "" : songArtist)
                .setContentIntent(content).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOngoing(playing)
                .setOnlyAlertOnce(true).addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "Pausar" : "Reproducir", action).build();
    }
    private void updateNotification(boolean playing) {
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification(playing));
    }
    @Override public void onTaskRemoved(Intent rootIntent) { /* El servicio continúa reproduciendo al cerrar la pantalla */ }
    @Override public void onDestroy() { if (player != null) { player.release(); player = null; } super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
