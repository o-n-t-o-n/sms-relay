package by.onton.relay;

import static by.onton.relay.Config.PLATFORM_TG;
import static by.onton.relay.Config.PLATFORM_VK;
import static by.onton.relay.Config.prefixLength;
import static by.onton.relay.Config.prefixes;
import static by.onton.relay.Http.isNetworkAvailable;
import static by.onton.relay.MainActivity.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public final class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Config.number == null) {
            try {
                MainActivity.applyConfig(context, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))
            return;

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

        for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            new Thread(() -> this.processMessage(context, sms)).start();
        }
    }

    private void processMessage(Context context, SmsMessage sms) {
        String number, text, prefix;
        number = sms.getDisplayOriginatingAddress();

        if (!number.equals(Config.number))
            return;

        text = sms.getDisplayMessageBody();

        if (text.length() <= prefixLength)
            return;

        prefix = text.substring(0, prefixLength);

        if (!prefixes.containsKey(prefix))
            return;

        text = text.substring(prefixLength);

        PlatformContact platformContact = prefixes.get(prefix);
        byte platform = platformContact.platform;
        Contact contact = platformContact.contact;


        for (int i = 0; i < 5; i++) {
            if (!isNetworkAvailable(context)) {
                i = 0;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.i(TAG, "interrupted", e);
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            try {
                if (platform == PLATFORM_TG) {
                    TelegramService.bot.sendMessage(contact.id, text);
                    break;
                } else if (platform == PLATFORM_VK) {
                    VkService.bot.sendMessage(contact.id, text);
                    break;
                }
            }
            catch (JSONException e) {
                throw new RuntimeException(e);
            }
            catch (IOException ignored) {}
        }
    }
}
