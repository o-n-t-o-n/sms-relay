package by.onton.relay;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES;

import android.content.Context;
import android.telephony.SmsManager;

final class Sms {
    static SmsManager manager = null;

    static void getManager(Context context) {
        if (SDK_INT >= VERSION_CODES.S)
            manager = context.getSystemService(SmsManager.class);
        else
            manager = SmsManager.getDefault();
    }

    static void send(String text) {
        manager.sendTextMessage(Config.number, null, text, null, null);
    }
}
