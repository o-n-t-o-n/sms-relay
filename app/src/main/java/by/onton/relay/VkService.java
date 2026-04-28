package by.onton.relay;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static by.onton.relay.Config.vk;
import static by.onton.relay.Http.isNetworkAvailable;
import static by.onton.relay.MainActivity.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public final class VkService extends Service {
    private static final byte ID = Config.PLATFORM_VK;
    static VkBot bot;

    private long owner;
    private boolean running = false;

    private PowerManager.WakeLock wakeLock;

    private void createNotificationChannel() {
        if (SDK_INT < Build.VERSION_CODES.O)
            return;

        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                Byte.toString(ID),
                getString(R.string.vk),
                IMPORTANCE_LOW
        ));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (vk == null) {
            try {
                MainActivity.applyConfig(this, true);

            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            owner = vk.logId;
        }

        createNotificationChannel();
        wakeLock = ((PowerManager) getSystemService(POWER_SERVICE))
                .newWakeLock(PARTIAL_WAKE_LOCK, "Relay:VkLock");
        wakeLock.acquire(Long.MAX_VALUE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder builder;
        if (SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, Byte.toString(ID));
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle(getString(R.string.vk) + ' ' + getString(R.string.running))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        if (!running) {
            running = true;

            if (SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                startForeground(1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else
                startForeground(1, notification);

            new Thread(this::botLoop).start();
        }
        return START_STICKY;
    }

    private void log(String text) {
        Log.d(TAG, text);
        if (vk.logId == 0)
            return;

        try {
            bot.sendMessage(vk.logId, text);
        } catch (Exception e) {
            Log.e(TAG, "log() -> vk.bot.sendMessage()", e);
        }
    }

    long duration = 1000, lastMessageTime = System.currentTimeMillis();
    private void botLoop() {
        log(getString(R.string.vk)
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
                JSONObject pollResult = bot.poll();

                bot.updateLpUrl(pollResult.optLong("ts"));

                JSONArray updates = pollResult.getJSONArray("updates");
                int length = updates.length();

                /*
                .updates[0].object.message.peer_id
                .updates[0].object.message.text
                .updates[0].object.message.id
                .updates[0].object.message.conversation_message_id
                */

                if (length > 0) {
                    for (int i = 0; i < length; i++) {
                        JSONObject update = updates.getJSONObject(i);
                        JSONObject message = update.getJSONObject("object").getJSONObject("message");

                        String text = message.optString("text", null);
                        long msgId = message.getLong("id");
                        lastMessageTime = message.getLong("date") * 1000;

                        Contact contact = vk.ids.get(message.getLong("peer_id"));

                        if (contact != null) {
                            log("prefix:" + contact.prefix + "\nid:" + contact.id + "\ntext:" + text);
                            vk.onMessage(contact, text, msgId);
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

        log(getString(R.string.vk) + ' '
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
