package by.onton.relay;

import static by.onton.relay.Config.*;

import android.telephony.SmsMessage;

import org.json.JSONException;
import org.json.JSONObject;

final class Contact {
    final String prefix;
    final long id;
    final boolean blocked, showRemainingMsgs;
    final int messagesPerMinute, messagesPerHour, messagesPerDay, messagesPerMonth;
    boolean informedOfBlock = false;

    Contact (JSONObject contact) throws JSONException {
        this.prefix = contact.getString(PREFIX);

        if (prefix.length() > 14)
            throw new JSONException(context.getString(R.string.prefix_too_long));

        if (prefix.charAt(0) == '/')
            throw new JSONException(context.getString(R.string.prefix_slash));

        if (prefixLength != SmsMessage.calculateLength(this.prefix, false)[1])
            throw new JSONException(context.getString(R.string.prefix_length));

        this.id = contact.getLong(ID);
        this.blocked = contact.optBoolean(BLOCKED, Config.Defaults.blocked);
        this.messagesPerMinute = contact.optInt(MESSAGES_PER_MINUTE, Config.Defaults.messagesPerMinute);
        this.messagesPerHour = contact.optInt(MESSAGES_PER_HOUR, Config.Defaults.messagesPerHour);
        this.messagesPerDay = contact.optInt(MESSAGES_PER_DAY, Config.Defaults.messagesPerDay);
        this.messagesPerMonth = contact.optInt(MESSAGES_PER_MONTH, Config.Defaults.messagesPerMonth);
        this.showRemainingMsgs = contact.optBoolean(SHOW_REMAINING, Config.Defaults.showRemainingMsgs);
    }
}
