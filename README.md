# Project is Unmaintained
I don't have the time or interest to continue working on ControlDLNA. If you want to take over the project, please contact me (or just fork it).

## Description

ControlDLNA is a DLNA and UPnP control point app for your phone.

It lets you play audio and video from any DLNA or UPnP compatible  server in the local network to a DLNA or UPnP renderer.

Additionally, other apps can utilize the MediaRouter API to play their media on a remote device.

Android 2.2 (Gingerbread) or higher is required.

[![Get it on Google Play](https://developer.android.com/images/brand/en_generic_rgb_wo_60.png)](https://play.google.com/store/apps/details?id=com.github.nutomic.controldlna) [![Get it on F-Droid](https://f-droid.org/wiki/images/0/06/F-Droid-button_get-it-on.png)](http://f-droid.org/repository/browse/?fdid=com.github.nutomic.controldlna)

Permissions: READ_PHONE_STATE is required to pause playback on phone call. All other permissions are required for UPnP functionality. ControlDLNA does not access the internet.

## Building

To build run `./gradlew assembleDebug` or `./gradlew assembleRelease`.
Windows users can use `gradlew.bat` rather than `./gradlew`.

## Icons

All icons are taken from AOSP.

## License

[BSD 3-Clause License](LICENSE.md)
