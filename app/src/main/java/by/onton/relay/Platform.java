package by.onton.relay;

import static android.telephony.SmsMessage.ENCODING_7BIT;
import static by.onton.relay.Config.Defaults.helloMsg;
import static by.onton.relay.Config.Defaults.lengthLimitMsg;
import static by.onton.relay.Config.Defaults.onlyTextMsg;
import static by.onton.relay.Config.Defaults.rateLimitLeftMsg;
import static by.onton.relay.Config.Defaults.systemMsg;
import static by.onton.relay.Config.PLATFORM_TG;
import static by.onton.relay.Config.PLATFORM_VK;
import static by.onton.relay.Config.gsmMaxLength;
import static by.onton.relay.Config.ucsMaxLength;
import static by.onton.relay.MainActivity.TAG;

import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

final class Platform {
    private static final byte
            FLAG_HELLO = 1,
            FLAG_ONLY_TEXT = 2,
            FLAG_LENGTH_LIMIT = 4,
            FLAG_RATE_LIMIT = 8,
            FLAG_PREFIX_GSM7 = -128;

    final byte id;
    final String token;
    final Contact[] contacts;
    final Map<Long, Contact> ids;
    final long logId;

    Platform(byte id, String token, Contact[] contacts, Map<Long, Contact> ids, long logId) {
        this.id = id;
        this.token = token;
        this.contacts = contacts;
        this.ids = ids;
        this.logId = logId;
    }

    private void respond(long userId, String text, long msgId) throws JSONException, IOException {
        if (id == PLATFORM_TG)
            TelegramService.bot.sendMessage(userId, text, msgId);
        else if (id == PLATFORM_VK)
            VkService.bot.sendMessage(userId, text, msgId);
    }

    private String systemMessage(byte flags) {
        if (flags == 0)
            return null;

        String message = "";

        if ((flags & FLAG_HELLO) != 0)
            message += helloMsg;
        if ((flags & FLAG_ONLY_TEXT) != 0)
            message += onlyTextMsg;
        Log.d(TAG, "lengthLimitMsg: " + lengthLimitMsg);
        if ((flags & FLAG_LENGTH_LIMIT) != 0)
            message += String.format(Locale.ROOT,
                    lengthLimitMsg + ((flags & FLAG_PREFIX_GSM7) != 0
                            ? "\n- GSM-7: %d"
                            : ""
                    ), ucsMaxLength, gsmMaxLength);
        if ((flags & FLAG_RATE_LIMIT) != 0)
            message += rateLimitLeftMsg;
        message += systemMsg;

        return message;
    }

    void onMessage(Contact contact, String text, long msgId) throws JSONException, IOException {
        if (text == null) {
            respond(contact.id, systemMessage(FLAG_ONLY_TEXT), msgId);
            return;
        }

        if (text.charAt(0) == '/') {
            throw new UnsupportedOperationException("Commands are not yet implemented");
        }

        int[] smsLength = SmsMessage.calculateLength(contact.prefix + text, false);
        boolean prefixIsGsm7 = SmsMessage.calculateLength(contact.prefix, false)[3] == ENCODING_7BIT;

        if (smsLength[0] != 1) {
            respond(contact.id, systemMessage((byte) (FLAG_LENGTH_LIMIT | (prefixIsGsm7
                    ? FLAG_PREFIX_GSM7
                    : 0
            ))), msgId);
            return;
        }

        if (Sms.manager == null)
            Sms.getManager(Config.context);

        Sms.send(contact.prefix + text);
    }
}
