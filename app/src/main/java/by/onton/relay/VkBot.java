package by.onton.relay;

import static by.onton.relay.Http.request;
import static by.onton.relay.MainActivity.TAG;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

final class VkBot {
    private static final String
            API_VERSION = "5.199",
            API_URL = "https://api.vk.ru/method/%s?v=" + API_VERSION,
            SEND_MESSAGE_PARAMS = "&peer_id=%d&message=%s",
            GET_LP_SERVER_PARAMS = "&group_id=%d",
            LP_PARAMS = "?act=a_check&wait=0&ts=%d&key=%s",
            LP_ACCEPT = "&message_new=1&message_reply=0&message_allow=0&message_deny=0&photo_new=0"
    + "&audio_new=0&video_new=0&wall_reply_new=0&wall_reply_edit=0&wall_reply_delete=0&wall_post_new=0"
    + "&wall_repost=0&board_post_new=0&board_post_edit=0&board_post_delete=0&board_post_restore=0"
    + "&photo_comment_new=0&photo_comment_edit=0&photo_comment_delete=0&photo_comment_restore=0"
    + "&video_comment_new=0&video_comment_edit=0&video_comment_delete=0&video_comment_restore=0"
    + "&market_comment_new=0&market_comment_edit=0&market_comment_delete=0&market_comment_restore=0"
    + "&poll_vote_new=0&group_join=0&group_leave=0&user_block=0&user_unblock=0&group_change_settings=0"
    + "&group_change_photo=0&group_officers_edit=0&donut_subscription_create=0&donut_subscription_prolonged=0"
    + "&donut_subscription_expired=0&donut_subscription_cancelled=0&subscription_price_changed=0"
    + "&donut_money_withdraw=0&donut_money_withdraw_error=0",
            RESPONSE="response";

    static final long
            CLIENT_ERROR = -1,
            LP_TS_TOO_LOW = 1,
            LP_REFRESH_KEY = 2,
            LP_REFRESH_ALL = 3;

    private final JSONObject sendMessagesParams = new JSONObject();
    private final String token;
    private final URL getByIdUrl, setLongPollSettingsUrl, sendMessageUrl;
    private String key, lpApiUrl;
    private URL lpUrl, getLongPollServerUrl;
    private long groupId, ownerId, ts;

    private String vkMethod(String method) {
        return String.format(Locale.ROOT, API_URL, method);
    }

    private String lpParams() {
        return String.format(Locale.ROOT, LP_PARAMS, ts, key);
    }

    private String sendMessageParams(long peerId, String message) {
        return String.format(Locale.ROOT, LP_PARAMS, peerId, message);
    }

    private String getLpServerParams() {
        return String.format(Locale.ROOT, GET_LP_SERVER_PARAMS, groupId);
    }

    VkBot() throws MalformedURLException {
        this.token = Config.vk.token;

        getByIdUrl = new URL(vkMethod("groups.getById"));
        setLongPollSettingsUrl = new URL(vkMethod("groups.setLongPollSettings") + LP_ACCEPT);
        sendMessageUrl = new URL(vkMethod("messages.send"));
    }

    boolean init() throws IOException, JSONException {
        JSONObject response = request(getByIdUrl, token).optJSONObject(RESPONSE);
        if (response == null)
            return false;

        groupId = response.getJSONArray("groups").getJSONObject(0).getLong("id");

        request(setLongPollSettingsUrl, token);

        getLongPollServerUrl = new URL(vkMethod("groups.getLongPollServer") + getLpServerParams());

        return getLongPollServer();
    }

    JSONObject poll() throws IOException {
        return request(lpUrl);
    }

    JSONObject sendMessage(long chatId, String text, long reply) throws JSONException, IOException {
        sendMessagesParams.put("peer_id", chatId);
        sendMessagesParams.put("message", text);
        sendMessagesParams.put("random_id", System.nanoTime());
        sendMessagesParams.put("reply_to", reply);

        return request(sendMessageUrl, token, sendMessagesParams);
    }

    JSONObject sendMessage(long chatId, String text) throws JSONException, IOException {
        return sendMessage(chatId, text, 0);
    }

    private boolean getLongPollServer() throws IOException {
        ts = -1;
        key = "";
        lpApiUrl = "";
        JSONObject response = request(getLongPollServerUrl, token).optJSONObject("response");

        if (response == null)
            return false;

        ts = response.optLong("ts", ts);
        key = response.optString("key", key);
        lpApiUrl = response.optString("server", lpApiUrl);

        if (ts == -1 || key.isEmpty() || lpApiUrl.isEmpty())
            return false;

        updateLpUrl(ts);

        return true;
    }

    void updateLpUrl(long ts) throws MalformedURLException {
        this.ts = ts;
        lpUrl = new URL(lpApiUrl + lpParams());
    }
}
