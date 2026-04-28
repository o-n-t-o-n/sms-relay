package by.onton.relay;

import static by.onton.relay.Config.context;
import static by.onton.relay.MainActivity.TAG;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public final class Http {
    static final long CLIENT_ERROR = -1;
    static final String UTF_8 = "UTF-8";

    // Source - https://stackoverflow.com/a/4239019
    // Posted by Alex Jasmin, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-04-27, License - CC BY-SA 4.0

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null
                ? connectivityManager.getActiveNetworkInfo()
                : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    static JSONObject request(URL url, String vkToken, JSONObject data) throws IOException {
        if (data != null) {
            String urlString = url.toString();
            StringBuilder query = new StringBuilder(urlString.contains("?") ? "&" : "?");
            Iterator<String> keys = data.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                query.append(URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(data.opt(key).toString(), "UTF-8"));
                if (keys.hasNext()) query.append("&");
            }
            url = new URL(urlString + query.toString());
        }
        Log.d(TAG, "request: " + url.toString().substring(0, 40) + '…');
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setDoOutput(false);
        con.setRequestProperty("Keep-Alive", "timeout=60");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");

        if (vkToken != null)
            con.setRequestProperty("Authorization", "Bearer " + vkToken);

        int status = con.getResponseCode();

        InputStream input = (status >= 200 && status < 300)
                ? con.getInputStream()
                : con.getErrorStream();

        StringBuilder responseBuilder = new StringBuilder();
        String responseText;
        JSONObject response = new JSONObject();

        char[] buffer = new char[4096];
        int length;

        try (InputStreamReader reader = new InputStreamReader(input, UTF_8)) {
            while ((length = reader.read(buffer)) != -1)
                responseBuilder.append(buffer, 0, length);

            responseText = responseBuilder.toString();
        }

        input.close();

        Log.d(TAG, "response: " + responseText);

        if (responseText.isEmpty())
            return response;

        try {
            response = new JSONObject(responseText);
        }
        catch (JSONException e) {
            try {
                String ERROR_MSG = context.getString(R.string.jsonexception);
                Log.w(TAG, ERROR_MSG);
                response = new JSONObject();
                // Telegram-compatible
                response.put("ok", false);
                response.put("error_code", CLIENT_ERROR);
                response.put("description", ERROR_MSG);

                // VK-compatible
                response.put("failed", CLIENT_ERROR);

                JSONObject error = new JSONObject();
                error.put("error_code", CLIENT_ERROR);
                error.put("error_msg", ERROR_MSG);

                response.put("error", error);
            }
            catch (JSONException je) {
                throw new RuntimeException(context.getString(R.string.double_jsonexception), je);
            }
        }
        return response;
    }

    static JSONObject request(URL url, String vkToken)
            throws IOException {
        return request(url, vkToken, null);
    }

    static JSONObject request(URL url, JSONObject data)
            throws IOException {
        return request(url, null, data);
    }

    static JSONObject request(URL url)
            throws IOException {
        return request(url, null, null);
    }
}
