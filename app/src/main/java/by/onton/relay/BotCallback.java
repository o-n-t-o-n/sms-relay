package by.onton.relay;

import org.json.JSONObject;

interface BotCallback<T> {
    void onSuccess(JSONObject response);
    void onError(Exception e);
}
