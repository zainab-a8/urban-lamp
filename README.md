<img align="right" height="256" src="https://lut.im/3IqLwsAZWH/piFLRMOgNLWmiqB8.png">
# Red Moon

Red Moon is a screen filter app for night time phone use. When the
lowest brightness setting of your phone won't do the trick, Red Moon
makes your screen even darker. With Red Moon, using your phone in the
dark won't hurt your eyes and thanks to it's red filter feature, you
will have a healthier sleep.

[![F-Droid](https://f-droid.org/wiki/images/0/06/F-Droid-button_get-it-on.png)](https://f-droid.org/repository/browse/?fdid=com.jmstudios.redmoon)

## Features

* Control the filter with separate color, intensity and dim settings
* Use the default profiles or create your own to quickly choose the
right filter settings
* Schedule automatic turn on and off times, so you don't have to worry
about turning the filter on at night
* Use the persistent notification to pause or stop the filter without
leaving the application you're using
* Control the filter with a widget or a shortcut, right from your
homescreen

## Planned feature

* Base turn on and off times on sunset and sunrise times at the user's
  location (issue #5)

## Screenshots

<img src="https://lut.im/vmFN3vnhn7/anZettuas7khW5l9.png" width="180" height="320" />
<img src="https://lut.im/oymEd1HoVK/YWEpkIPVNOzPfm0O.png" width="180" height="320" />
<img src="https://lut.im/XDgAt3mSOx/WZO1rmwVaM1gS4Qk.png" width="180" height="320" />
<img src="https://lut.im/mBVVEtCZj6/tUNoPKUPoXOc29es.png" width="180" height="320" />
<img src="https://lut.im/EmFrykMlFy/ZIdJvWfV9w7PuVb4.png" width="180" height="320" />
<img src="https://lut.im/WrQySHuyyB/Z6Hy5x22gw9XZNFn.png" width="180" height="320" />

## License

"Red Moon" is a derivative of
"[Shades](https://github.com/cngu/shades)" by
[Chris Nguyen](https://github.com/cngu) used under the
[MIT License](https://github.com/cngu/shades/blob/e240edc1df3e6dd319cd475a739570ff8367d7f8/LICENSE). "Red
Moon" is licensed under the
[GNU General Public License version 3](https://www.gnu.org/licenses/gpl-3.0.html),
or (at your option) any later version by Marien Raat.

All used artwork is released into the public domain. Some of the icons
use cliparts from [openclipart.org](https://openclipart.org/), which
are all released in the public domain, namely:
* https://openclipart.org/detail/121903/full-moon
* https://openclipart.org/detail/213998/nexus-5-flat
* https://openclipart.org/detail/219211/option-button-symbol-minimal-svg-markup
* https://openclipart.org/detail/20806/wolf-head-howl-1

## Building

To build the app on GNU+Linux, clone the repository and run

``` ./gradlew build ```

in the root directory.

Use

``` ./gradlew installDebug ```

to install the app on a connected device.

## How it works

Red Moon displays a constant transparant overlay to dim and color the
screen when the filter is on. If, for example, you have the intensity
set to 0%, then the overlay will be black with a transparency equal to
the dim level you selected. If you choose a higher intensity, the
color will be saturated with the selected color.

## Contributing

All help is welcome! If you have found a bug or have an idea for a new
feature, just
[open a new issue](https://github.com/raatmarien/red-moon/issues/new). If
you can implement it yourself, simply fork this repository, make your
changes and open a pull request.
