package by.onton.relay;

import android.content.Context;
import android.telephony.SmsMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

final class Config {
    static Context context;

    static final String
            NUMBER = "number",
            PREFIX = "prefix",
            ID = "id",
            BLOCKED = "blocked",
            MESSAGES_PER_MINUTE = "messagesPerMinute",
            MESSAGES_PER_HOUR = "messagesPerHour",
            MESSAGES_PER_DAY = "messagesPerDay",
            MESSAGES_PER_MONTH = "messagesPerMonth",
            SHOW_REMAINING = "showRemainingMsgs",

            HELLO_MSG = "helloMsg",
            ONLY_TEXT_MSG = "onlyTextMsg",
            LENGTH_LIMIT_MSG = "lengthLimitMsg",
            RATE_LIMIT_REACHED_MSG = "rateLimitReachedMsg",
            RATE_LIMIT_REMAINING_MSG = "rateLimitRemainingMsg",
            BLOCKED_MSG = "blockedMsg",
            UNBLOCKED_MSG = "unblockedMsg",
            SYSTEM_MSG = "systemMsg";

    static final byte PLATFORM_TG = 1, PLATFORM_VK = 2;

    private static final class BuiltinDefaults {
        static final int
                messagesPerMinute = 1,
                messagesPerHour = 5,
                messagesPerDay = 10,
                messagesPerMonth = 100;

        static final boolean
                blocked = false,
                showRemainingMsgs = true;
    }

    static final class Defaults {
        static int
                messagesPerMinute = BuiltinDefaults.messagesPerMinute,
                messagesPerHour = BuiltinDefaults.messagesPerHour,
                messagesPerDay = BuiltinDefaults.messagesPerDay,
                messagesPerMonth = BuiltinDefaults.messagesPerMonth;

        static boolean
                blocked = BuiltinDefaults.blocked,
                showRemainingMsgs = BuiltinDefaults.showRemainingMsgs;

        static String
                helloMsg = context.getString(R.string.hello_msg) + '\n',
                onlyTextMsg = context.getString(R.string.only_text_msg) + '\n',
                lengthLimitMsg = context.getString(R.string.length_limit_msg) + ":\n- UCS-2: %d",
                rateLimitReachedMsg = context.getString(R.string.rate_limit_reached_msg),
                rateLimitLeftMsg = context.getString(R.string.rate_limit_left_msg) + '\n',
                blockedMsg = context.getString(R.string.blocked_msg),
                unblockedMsg = context.getString(R.string.unblocked_msg),
                systemMsg = '\n' + context.getString(R.string.system_msg);
    }

    static boolean autostartTg, autostartVk, prefixIsEmpty;
    static int[] prefixSmsLength;
    static int prefixLength;
    static int gsmMaxLength = 0, ucsMaxLength = 0;
    static String number;
    static Platform tg, vk;
    static Map<String, PlatformContact> prefixes;

    private static Platform setupPlatform(JSONObject config, byte platformId) throws JSONException {
        String token = null;
        JSONArray contactsJson = null;
        Contact[] contacts;
        Map<Long, Contact> ids;
        long logId = 0;
        int contactsLength;

        switch (platformId) {
            case PLATFORM_TG:
                token = config.optString("tgToken", null);
                contactsJson = config.optJSONArray("tgContacts");
                logId = config.optLong("tgLogId");
                break;

            case PLATFORM_VK:
                token = config.optString("vkToken", null);
                contactsJson = config.optJSONArray("vkContacts");
                logId = config.optLong("vkLogId");
                break;
        }
        // noinspection ConstantConditions
        if (token != null
                && contactsJson != null
                && (contactsLength = contactsJson.length()) > 0) {
            contacts = new Contact[contactsLength];

            String prefix = contactsJson.getJSONObject(0).getString(PREFIX);

            if (prefixSmsLength[3] == -1) {
                prefixSmsLength = SmsMessage.calculateLength(prefix, false);
                prefixLength = prefixSmsLength[1];
                gsmMaxLength = 160 - prefixLength;
                ucsMaxLength = 70 - prefixLength;
            }

            ids = new HashMap<>(contactsLength);

            for (int i = 0; i < contactsLength; i++) {
                contacts[i] = new Contact(contactsJson.getJSONObject(i));

                 if (prefixes.containsKey(contacts[i].prefix))
                    throw new JSONException(context.getString(R.string.duplicate_prefix));

                prefixes.put(contacts[i].prefix, new PlatformContact(platformId, contacts[i]));

                if (ids.containsKey(contacts[i].id))
                    throw new JSONException(context.getString(R.string.duplicate_contact_id));

                ids.put(contacts[i].id, contacts[i]);
            }
            return new Platform(platformId, token, contacts, ids, logId);
        }
        return null;
    }

    static void set(JSONObject config) throws JSONException {
        Defaults.messagesPerMinute = config.optInt(MESSAGES_PER_MINUTE, Defaults.messagesPerMinute);
        Defaults.messagesPerHour = config.optInt(MESSAGES_PER_HOUR, Defaults.messagesPerHour);
        Defaults.messagesPerDay = config.optInt(MESSAGES_PER_DAY, Defaults.messagesPerDay);
        Defaults.messagesPerMonth = config.optInt(MESSAGES_PER_MONTH, Defaults.messagesPerMonth);
        Defaults.showRemainingMsgs = config.optBoolean(SHOW_REMAINING, Defaults.showRemainingMsgs);

        Defaults.helloMsg = config.optString(HELLO_MSG, Defaults.helloMsg);
        Defaults.onlyTextMsg = config.optString(ONLY_TEXT_MSG, Defaults.onlyTextMsg);
        Defaults.lengthLimitMsg = config.optString(LENGTH_LIMIT_MSG, Defaults.lengthLimitMsg);
        Defaults.rateLimitReachedMsg = config.optString(RATE_LIMIT_REACHED_MSG, Defaults.rateLimitReachedMsg);
        Defaults.rateLimitLeftMsg = config.optString(RATE_LIMIT_REMAINING_MSG, Defaults.rateLimitLeftMsg);
        Defaults.blockedMsg = config.optString(BLOCKED_MSG, Defaults.blockedMsg);
        Defaults.unblockedMsg = config.optString(UNBLOCKED_MSG, Defaults.unblockedMsg);
        Defaults.systemMsg = config.optString(SYSTEM_MSG, Defaults.systemMsg);

        number = config.getString(NUMBER);

        prefixes = new HashMap<>();
        prefixIsEmpty = false;
        prefixSmsLength = new int[] {0, -1, -1, -1};

        tg = null;
        tg = setupPlatform(config, PLATFORM_TG);
        vk = null;
        vk = setupPlatform(config, PLATFORM_VK);


    }
}
