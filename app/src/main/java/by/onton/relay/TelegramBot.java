package by.onton.relay;

import static by.onton.relay.Http.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


import java.util.Collections;
import java.util.Locale;

final class TelegramBot {
    private static final String API_URL = "https://api.telegram.org/bot%s/%s",
            MESSAGE_ID = "message_id";
    private final String token;
    private final URL getMeUrl, getUpdatesUrl, sendMessageUrl;
    private final JSONObject getUpdatesParams = new JSONObject(),
            sendMessagesParams = new JSONObject(),
            replyParameters = new JSONObject();

    private String tgMethod(String method) {
        return String.format(Locale.ROOT, API_URL, token, method);
    }

    TelegramBot() throws MalformedURLException, JSONException {
        this.token = Config.tg.token;

        getMeUrl = new URL(tgMethod("getMe"));
        getUpdatesUrl = new URL(tgMethod("getUpdates"));
        sendMessageUrl = new URL(tgMethod("sendMessage"));

        getUpdatesParams.put("allowed_updates", new JSONArray(Collections.singletonList("message")));

        replyParameters.put(MESSAGE_ID, 0);
        replyParameters.put("allow_sending_without_reply", true);

        sendMessagesParams.put("reply_parameters", replyParameters);
    }

    JSONObject getMe()
            throws IOException {
        return request(getMeUrl);
    }

    JSONObject getUpdates(long offset) throws JSONException, IOException {
        getUpdatesParams.put("offset", offset);
        return request(getUpdatesUrl, getUpdatesParams);//.optJSONArray("result");
    }

    JSONObject sendMessage(long chatId, String text, long reply) throws JSONException, IOException {
        replyParameters.put(MESSAGE_ID, reply);
        sendMessagesParams.put("chat_id", chatId);
        sendMessagesParams.put("text", text);
        return request(sendMessageUrl, sendMessagesParams);
    }

    JSONObject sendMessage(long chatId, String text) throws JSONException, IOException {
        return sendMessage(chatId, text, 0);
    }
}
