# Red Moon

Red Moon is a screen filter app for night time phone use. It helps you
sleep after using your phone, by providing a red and dimming filter
that is easy on the eyes. It has separate color temperature, intensity
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
* Real-time color, intensity and dim level indicators
* Ability to dim the navigation and the notification bar

## Planned feature
* Standard profiles
* Custom profiles
* Automatic turn on and off times
* Base turn on and off times on sunset and sunrise times at the user's location (issue #5)

## Screenshots
<img src="https://lut.im/3k5INuLva5/lXt4Y2SqMVOrqsOt.png" width="180" height="320" />
<img src="https://lut.im/d2v7Qp5F95/u8gMgjrCN1w9PFiG.png" width="180" height="320" />
<img src="https://lut.im/E6Znl6oGGL/wPJyhFXa8hVmyVUS.png" width="180" height="320" />
<img src="https://lut.im/M2Rbi9d4FX/usbnTueHly6Nb3sO.png" width="180" height="320" />


## License
The source code of Red Moon is distributed under the MIT License (see the
LICENSE file in the root folder of the project).

All used artwork is released into the public domain. Some of the icons use
cliparts from [openclipart.org](https://openclipart.org/), which are all
released in the public domain, namely:
* https://openclipart.org/detail/121903/full-moon
* https://openclipart.org/detail/213998/nexus-5-flat
* https://openclipart.org/detail/219211/option-button-symbol-minimal-svg-markup

Red Moon is based on [Shades](https://github.com/cngu/shades), by Chris Nguyen.

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
