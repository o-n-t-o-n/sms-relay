# SMS Relay

A minimalist self-contained **barely working** Android app for forwarding Telegram and VK messages over SMS and vice versa.

[![Unstable](https://img.shields.io/badge/Stability-Unstable-yellow.svg)](http://github.com/badges/stability-badges)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## Installing

Build the app from source or download a pre-built APK from [Releases](https://github.com/o-n-t-o-n/sms-relay/releases). It should be compatible with Android 5.0–17 (API levels 21–37).

## Building

To reproduce `by.onton.relay-unsigned.apk`, build a Release APK and run the following shell commands inside of `sms-relay/app/build/outputs/apk/release` on a POSIX-compatible system:

```shell
rm -r /tmp/by.onton.relay-temp /tmp/by.onton.relay-{temp,unsigned}.apk
unzip app-release-unsigned.apk AndroidManifest.xml resources.arsc classes.dex 'res/*.xml' -d /tmp/by.onton.relay-temp
cd /tmp/by.onton.relay-temp
find . -type f -exec chmod 644 {} \; -exec touch -t 202601010000 {} \;
find . -type d -exec chmod 755 {} \; -exec touch -t 202601010000 {} \;
zip -X -9 ../by.onton.relay-temp.apk AndroidManifest.xml
find res -type f -name '*.xml' | sort | zip -X -9 ../by.onton.relay-temp.apk -@
zip -X -0 ../by.onton.relay-temp.apk classes.dex resources.arsc
cd ..
zipalign 4 by.onton.relay-temp.apk by.onton.relay-unsigned.apk
```

Software used:
- [GNU Coreutils](https://packages.cachyos.org/package/cachyos-core-v3/x86_64_v3/coreutils) `9.10-1.1`
- [GNU Findutils](https://packages.cachyos.org/package/cachyos-core-v3/x86_64_v3/findutils) `4.10.0-3.1`
- [Info-ZIP Zip](https://packages.cachyos.org/package/cachyos-extra-v3/x86_64_v3/zip) `3.0-13.1`
- [Info-ZIP UnZip](https://packages.cachyos.org/package/cachyos-extra-v3/x86_64_v3/unzip) `6.0-23.1`
- [Liberica JDK 21](https://aur.archlinux.org/packages/liberica-jdk-21-full-bin) `21.0.10.u10-1`
- [Android SDK Build Tools](https://developer.android.com/tools/releases/build-tools) [`37.0.0`](https://dl.google.com/android/repository/build-tools_r37_linux.zip)
- [Android SDK Platform](https://developer.android.com/tools/releases/platforms) [`37.0`](https://dl.google.com/android/repository/platform-37.0_r01.zip)
- [Android Studio](https://aur.archlinux.org/packages/android-studio) [`2025.3.3.7-1`](https://dl.google.com/dl/android/studio/ide-zips/2025.3.3.7/android-studio-panda3-patch1-linux.tar.gz)

## Setup

> [!TIP]
> Use Logcat or a similar tool. The app only shows short toast messages that are not always helpful.

### Telegram Bot

1. [Create a bot and obtain its token](https://core.telegram.org/bots#how-do-i-create-a-bot)

### VK Bot
1. [Enable 2-step verification](https://id.vk.com/account/#/otp-settings)
> [!WARNING]
> There is a high chance you will not be able to request a confirmation code (OTP) from a device with Developer options and/or root access, or from the browser version.
> To be safe, move your SIM card to a "clean" device, install VK, log in, and request the code on it.
2. [Create a community](https://vk.com/groups_create)
3. [Obtain a community access token](https://dev.vk.com/en/api/bots/getting-started#Community)

> [!IMPORTANT]
> When issuing a token enable the following rights:
> - [x] Allow access to community management &emsp; _\* required to get the key for long polling_
> - [x] Allow access to community messages

### config.json

Use this template to write your own configuration:

```json
{
    "number": "<Target phone number>",

    "tgToken": "<Telegram bot token>",
    "tgLogId": 0,
    "tgContacts": [
        {
            "prefix": "<SMS prefix>",
            "id": 1,
            "blocked": false,
            "messagesPerMinute": 1,
            "messagesPerHour": 5,
            "messagesPerDay": 10,
            "messagesPerMonth": 100,
            "showRemainingMsgs": true
        }
    ],

    "vkToken": "<VK bot token>",
    "vkLogId": 0,
    "vkContacts": [
        {
            "prefix": "<SMS prefix>",
            "id": 1,
            "blocked": false,
            "messagesPerMinute": 1,
            "messagesPerHour": 5,
            "messagesPerDay": 10,
            "messagesPerMonth": 100,
            "showRemainingMsgs": true
        }
    ],

    "messagesPerMinute": 1,
    "messagesPerHour": 5,
    "messagesPerDay": 10,
    "messagesPerMonth": 100,
    "showRemainingMsgs": true,

    "helloMsg": "Welcome to %s!",
    "onlyTextMsg": "Only text messages are allowed",
    "lengthLimitMsg": "Message length limit:",
    "rateLimitReachedMsg": "Message rate limit reached. Try again later",
    "rateLimitLeftMsg": "Remaining messages per:\n- minute: %d\n- hour: %d\n- day: %d\n- month: %d",
    "blockedMsg": "You've been blocked",
    "unblockedMsg": "You've been unblocked",
    "systemMsg": "This is a system message. Do not respond"
}
```

The **target** is the device SMS Relay will communicate with over SMS, **not** the device running the app.

Optional fields:
- `blocked`, `messagesPer*`, and `showRemainingMsgs` are applied to each contact. The priority is (ascending): built-in defaults (same as in the template above), global values, per-contact values.
- `*Msg` are localized unless you set them manually. See [res](https://github.com/o-n-t-o-n/sms-relay/blob/master/app/src/main/res) for available locales.
- `tgLogId` and `vkLogId` are your own user IDs for logging.

A prefix is a **unique string** of up to 14 characters supported by the [GSM 03.38 standard](https://en.wikipedia.org/wiki/GSM_03.38) (7-bit default alphabet or UCS-2), used to identify contacts.

It **may be empty** only if one platform is used and there is only one contact.

Prefixes **must not** start with a forward slash (`/`).

A prefix is **not separated** from the message itself. Because of this **all prefixes must be of the same length**.

I strongly suggest using short GSM-7 prefixes to not waste the already limited actual message length or break anything.

---

## Notes

This is a **Kotlin-free** app. Several measures, _mostly aggressive_, were taken to ensure it doesn't slip into the app.<br>
It doesn't mean that I will be a d\*\*\* about it and try to forbid Kotlin in your own forks. **I won't**. This is free software, after all.<br>
To remove the anti-Kotlin measures, search for `==Kotlin==` in all files and remove the relevant code under the comments.

The code is garbage, I know. It's also **not stable**. You should definitely check [Don't kill my app!](https://dontkillmyapp.com/) and have someone check on the app if you're away.

You must either only have one SIM card in the device running the app or configure the default SIM for messaging.
