package by.onton.relay;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.SEND_SMS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static by.onton.relay.Http.UTF_8;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public final class MainActivity extends Activity {
    static final String
            TAG = "relay",
            CONFIG_JSON = "config.json";

    private EditText configEditText;

    //  Detect ==Kotlin==
    private boolean findKotlin() {
        try {
            Class.forName("kotlin.Metadata");
            return true;
        }
        catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//      Crash the app if ==Kotlin== is detected. Considering all other measures this should never fire.
        if (findKotlin())
            throw new RuntimeException(getString(R.string.kotlin_detected));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BuildConfig.IS_DEBUG)
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        configEditText = findViewById(R.id.config);

        ArrayList<String> permissions = new ArrayList<>(4);

        if (SDK_INT >= VERSION_CODES.TIRAMISU)
            if (checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED)
                permissions.add(POST_NOTIFICATIONS);

        if (SDK_INT >= VERSION_CODES.M) {
            if (checkSelfPermission(RECEIVE_SMS) != PERMISSION_GRANTED)
                permissions.add(RECEIVE_SMS);
            if (checkSelfPermission(SEND_SMS) != PERMISSION_GRANTED)
                permissions.add(SEND_SMS);
            if (checkSelfPermission(READ_PHONE_STATE) != PERMISSION_GRANTED)
                permissions.add(READ_PHONE_STATE);

            if (permissions.isEmpty())
                return;

            requestPermissions(permissions.toArray(new String[0]), 1);
        }

        Sms.getManager(this);
    }

    @SuppressLint("UseRequiresApi")
    @TargetApi(VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (!((PowerManager) getSystemService(Context.POWER_SERVICE))
                .isIgnoringBatteryOptimizations(getPackageName())) {
            @SuppressLint("BatteryLife") Intent intent = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "onRequestPermissionsResult", e);
            }
        }
    }

    private void toastAndLog(String message) {
        Log.i(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void exceptionalToastAndLog(Exception e, String methodName) {
        Log.e(TAG, methodName + ": ", e);
        Toast.makeText(this,
                e.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    private void saveConfig(boolean hidden, byte[] bytes) throws IOException {
        File configFile = new File(this.getFilesDir(), (hidden ? '.' : "") + CONFIG_JSON);
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            output.write(bytes);
        }
    }

    public void saveConfig(View __) {
        try {
            saveConfig(false, configEditText.getText().toString().getBytes());
            toastAndLog(getString(R.string.saved));
        }
        catch (IOException e) {
            exceptionalToastAndLog(e, "saveConfig");
        }
    }

    static private String loadConfig(Context context, boolean hidden) throws IOException {
        File configFile = new File(context.getFilesDir(), (hidden ? '.' : "") + CONFIG_JSON);
        if (!configFile.exists())
            return null;

        StringBuilder configBuilder = new StringBuilder();
        char[] buffer = new char[4096];
        int length;

        try (FileReader reader = new FileReader(configFile)) {
            while ((length = reader.read(buffer)) != -1)
                configBuilder.append(buffer, 0, length);
            return configBuilder.toString();
        }
    }

    public void loadConfig(View view) {
        try {
            configEditText.setText(loadConfig(view.getContext(), false));
            toastAndLog(getString(R.string.loaded));
        }
        catch (IOException e) {
            exceptionalToastAndLog(e, "loadConfig");
        }
    }

    static void applyConfig(Context context, boolean hidden) throws JSONException, IOException {
        Config.tg = Config.vk = null;
        Config.context = context;

        Config.set(new JSONObject(loadConfig(context, hidden)));

        String toast = "";

        if (Config.tg != null)
            TelegramService.bot = new TelegramBot();
        if (Config.vk != null) {
            VkService.bot = new VkBot();
            try {
                if (!VkService.bot.init())
                    Config.vk = null;
            } catch (Exception e) {
                Log.e(TAG, "applyConfig", e);
                Config.vk = null;
            }
        }
    }

    public void applyConfig(View view) {
        Button startTg = findViewById(R.id.startTg);
        startTg.setEnabled(false);

        Button startVk = findViewById(R.id.startVk);
        startVk.setEnabled(false);

        String toast = "";

        try {
            String configString = configEditText.getText().toString();

            if (!configString.isEmpty())
                saveConfig(true, new JSONObject(configString).toString().getBytes(UTF_8));

            applyConfig(view.getContext(), true);

            Log.d(TAG, "lengthLimitMsg: " + Config.Defaults.lengthLimitMsg);

            if (Config.tg != null) {
                toast += getString(R.string.telegram) + '\n';
                startTg.setEnabled(true);
            }

            if (Config.vk != null) {
                toast += getString(R.string.vk) + '\n';
                startVk.setEnabled(true);
            }

            if (Config.tg == Config.vk)
                toast = getString(R.string.no_platform);
            else
                toast = toast.substring(0, toast.length() - 1);

            toastAndLog(toast);
        }
        catch (Exception e) {
            exceptionalToastAndLog(e, "applyConfig");
        }
    }

    public void startTg(View __) {
        Intent intent = new Intent(this, TelegramService.class);
        if (SDK_INT >= VERSION_CODES.O) {
            this.startForegroundService(intent);
        } else {
            this.startService(intent);
        }
    }

    public void stopTg(View __) {
        this.stopService(new Intent(this, TelegramService.class));
    }

    public void startVk(View __) {
        Intent intent = new Intent(this, VkService.class);
        if (SDK_INT >= VERSION_CODES.O) {
            this.startForegroundService(intent);
        } else {
            this.startService(intent);
        }
    }

    public void stopVk(View __) {
        this.stopService(new Intent(this, VkService.class));
    }
}
