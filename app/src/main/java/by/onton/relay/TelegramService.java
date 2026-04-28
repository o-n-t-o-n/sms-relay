package by.onton.relay;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static by.onton.relay.Config.tg;
import static by.onton.relay.Http.isNetworkAvailable;
import static by.onton.relay.MainActivity.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public final class TelegramService extends Service {
    private static final byte ID = Config.PLATFORM_TG;
    static TelegramBot bot;

    private volatile boolean running = false;
    private long offset = 0;

    private PowerManager.WakeLock wakeLock;

    private void createNotificationChannel() {
        if (SDK_INT < VERSION_CODES.O)
            return;

        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                Byte.toString(ID),
                getString(R.string.telegram),
                IMPORTANCE_LOW
        ));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (tg == null) {
            try {
                MainActivity.applyConfig(this, true);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        createNotificationChannel();
        wakeLock = ((PowerManager) getSystemService(POWER_SERVICE))
                .newWakeLock(PARTIAL_WAKE_LOCK, "Relay:TelegramLock");
        wakeLock.acquire(Long.MAX_VALUE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder builder;
        if (SDK_INT >= VERSION_CODES.O) {
            builder = new Notification.Builder(this, Byte.toString(ID));
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle(getString(R.string.telegram) + ' ' + getString(R.string.running))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        if (!running) {
            running = true;

            if (SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE)
                startForeground(1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else
                startForeground(1, notification);

            new Thread(this::botLoop).start();
        }
        return START_STICKY;
    }

    private void log(String text) {
        Log.d(TAG, text);
        if (tg.logId == 0)
            return;

        try {
            bot.sendMessage(tg.logId, text);
        } catch (Exception e) {
            Log.e(TAG, "log() -> tg.bot.sendMessage()", e);
        }
    }

    long duration = 1000, lastMessageTime = System.currentTimeMillis();
    private void botLoop() {
        log(getString(R.string.telegram)
                + ' ' + getString(R.string.running));

        while (running) {
            try {
                Thread.sleep(duration);
            }
            catch (InterruptedException e) {
                Log.i(TAG, "interrupted", e);
                Thread.currentThread().interrupt();
            }

            if (!isNetworkAvailable(Config.context)) {
                duration = 500;
                continue;
            }

            if (!running)
                break;

            try {
                JSONObject updates = bot.getUpdates(offset);

                if (!updates.getBoolean("ok"))
                    log(updates.toString());

                JSONArray result = updates.getJSONArray("result");
                int length = result.length();

                if (length > 0) {
                    for (int i = 0; i < length; i++) {
                        JSONObject update = result.getJSONObject(i);
                        JSONObject message = update.getJSONObject("message");
                        JSONObject chat = message.getJSONObject("chat");
                        String text = message.optString("text", null);
                        long msgId = message.getLong("message_id");

                        offset = update.getLong("update_id") + 1;
                        lastMessageTime = message.getLong("date") * 1000;

                        Contact contact = tg.ids.get(chat.getLong("id"));

                        if (contact != null) {
                            log("prefix:" + contact.prefix + "\nid:" + contact.id + "\ntext:" + text);
                            tg.onMessage(contact, text, msgId);
                        }
                    }
                }

                duration = (System.currentTimeMillis() - lastMessageTime) / 8;
                duration = Math.clamp(duration, 500, 20_000);
            } catch (Exception e) {
                log(e.getMessage());
                Log.e(TAG, "botLoop", e);
            }
        }

        log(getString(R.string.telegram) + ' '
                + getString(R.string.stopped) + ' '
                + System.currentTimeMillis());
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
