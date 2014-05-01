## Description

ControlDLNA is a DLNA and UPnP control point app for your phone.

It lets you play audio and video from any DLNA or UPnP compatible  server in the local network to a DLNA or UPnP renderer.

Additionally, other apps can utilize the MediaRouter API to play their media on a remote device.

Android 2.2 (Gingerbread) or higher is required.

[Download](http://f-droid.org/repository/browse/?fdid=com.github.nutomic.controldlna)

Permissions: READ_PHONE_STATE is required to pause playback on phone call. All other permissions are required for UPnP functionality. ControlDLNA does not access the internet.

## Building

To build, install Gradle (make sure it has support for Android builds), and then simply run `gradle assembleDebug` or `gradle assembleRelease` The apk will be placed in `build/apk`.

## Icons

All icons are taken from AOSP.

## License

[BSD 3-Clause License](LICENSE.md)
