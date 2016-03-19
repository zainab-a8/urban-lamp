# Red Moon

Red Moon is a screen filter app for night time phone use. It helps you
sleep after using your phone, by providing a red and dimming filter
that is easy on the eyes. It has seperate color temperature, intensity
and dim level settings.

[![F-Droid](https://f-droid.org/wiki/images/0/06/F-Droid-button_get-it-on.png)](https://f-droid.org/repository/browse/?fdid=com.jmstudios.redmoon)

## Features
* Free and open source (MIT License)
* Separate color temperature, intensity and dim level settings for
complete control
* Persistent notification with pause and stop action
* Automatic startup feature
* Material design
* Smooth transitions when turning the filter on or off

## Planned feature
* Standard profiles
* Custom profiles
* Real-time color, intensity and dim level indicators
* Automatic turn on and off times

## License
The source code of Red Moon is distributed under the MIT License (see the
LICENSE file in the root folder of the project).

All used artwork is released into the public domain. Some of the icons use
cliparts from [openclipart.org](https://openclipart.org/), which are all
released in the public domain, namely:
* https://openclipart.org/detail/121903/full-moon
* https://openclipart.org/detail/213998/nexus-5-flat
* https://openclipart.org/detail/219211/option-button-symbol-minimal-svg-markup

## Building
To build the app on GNU+Linux, clone the repository and run

```
./gradlew build
```

in the root directory.

Use

```
./gradlew installDebug
```

to install the app on a connected device.
