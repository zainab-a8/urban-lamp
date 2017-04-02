/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jmstudios.redmoon.helper

import android.graphics.Color

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.util.getString

import org.json.JSONObject

/**
 * Color, intensity, and dimLevel range from 0 to 100 inclusive. Regardless of
 *                     value, the filter is guaranteed to never be fully opaque.
 *
 * color: 0 is 500k; 100 is 3500k
 *
 * dim: 0 doesn't darken; 100 is the maximum the system allows.
 *
 * intensity: 0 doesn't color the filter; 100 is the maximum the system allows.
 */
data class Profile(
        val name:            String  = NAME_CUSTOM,
        val color:           Int     = DEFAULT_COLOR,
        val intensity:       Int     = DEFAULT_INTENSITY,
        val dimLevel:        Int     = DEFAULT_DIM_LEVEL,
        val lowerBrightness: Boolean = false) {

    override fun toString() = JSONObject().run {
        put(KEY_NAME,      name     )
        put(KEY_COLOR,     color    )
        put(KEY_INTENSITY, intensity)
        put(KEY_DIM_LEVEL, dimLevel )
        put(KEY_LOWER_BRIGHTNESS, lowerBrightness)
        toString()
    }

    val filterColor: Int
        get() {
            val rgbColor = rgbFromColor(color)
            val intensityColor = Color.argb(floatToColorBits(intensity.toFloat() / 100.0f),
                                            Color.red  (rgbColor),
                                            Color.green(rgbColor),
                                            Color.blue (rgbColor))
            val dimColor = Color.argb(floatToColorBits(dimLevel.toFloat() / 100.0f), 0, 0, 0)
            return addColors(dimColor, intensityColor)
        }

    private fun addColors(color1: Int, color2: Int): Int {
        var alpha1 = colorBitsToFloat(Color.alpha(color1))
        var alpha2 = colorBitsToFloat(Color.alpha(color2))
        val red1 = colorBitsToFloat(Color.red(color1))
        val red2 = colorBitsToFloat(Color.red(color2))
        val green1 = colorBitsToFloat(Color.green(color1))
        val green2 = colorBitsToFloat(Color.green(color2))
        val blue1 = colorBitsToFloat(Color.blue(color1))
        val blue2 = colorBitsToFloat(Color.blue(color2))

        // See: http://stackoverflow.com/a/10782314

        // Alpha changed to allow more control
        val fAlpha = alpha2 * INTENSITY_MAX_ALPHA + (DIM_MAX_ALPHA - alpha2 * INTENSITY_MAX_ALPHA) * alpha1
        alpha1 *= ALPHA_ADD_MULTIPLIER
        alpha2 *= ALPHA_ADD_MULTIPLIER

        val alpha = floatToColorBits(fAlpha)
        val red   = floatToColorBits((red1   * alpha1 + red2   * alpha2 * (1.0f - alpha1)) / fAlpha)
        val green = floatToColorBits((green1 * alpha1 + green2 * alpha2 * (1.0f - alpha1)) / fAlpha)
        val blue  = floatToColorBits((blue1  * alpha1 + blue2  * alpha2 * (1.0f - alpha1)) / fAlpha)

        return Color.argb(alpha, red, green, blue)
    }

    companion object {
        private const val KEY_NAME      = "name"
        private const val KEY_COLOR     = "color"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_DIM_LEVEL = "dim"
        private const val KEY_LOWER_BRIGHTNESS = "lower-brightness"

        private const val MIN_DIM_LEVEL = 0
        private const val MIN_INTENSITY = 0

        private val NAME_CUSTOM = getString(R.string.filter_name_custom)

        const val DEFAULT_DIM_LEVEL = MIN_DIM_LEVEL
        const val DEFAULT_INTENSITY = MIN_INTENSITY
        const val DEFAULT_COLOR     = 10

        const val DIM_MAX_ALPHA = 0.9f
        private const val INTENSITY_MAX_ALPHA  = 0.75f
        private const val ALPHA_ADD_MULTIPLIER = 0.75f

        private fun colorBitsToFloat(bits:  Int): Float = bits.toFloat() / 255.0f
        private fun floatToColorBits(color: Float): Int = (color * 255.0f).toInt()

        internal fun parse(entry: String): Profile = JSONObject(entry).run {
            val name      = optString(KEY_NAME)
            val color     = optInt(KEY_COLOR)
            val intensity = optInt(KEY_INTENSITY)
            val dim       = optInt(KEY_DIM_LEVEL)
            val lowerBrightness = optBoolean(KEY_LOWER_BRIGHTNESS)
            Profile(name, color, intensity, dim, lowerBrightness)
        }

        fun getColorTemperature(color: Int): Int = 500 + color * 30

        fun rgbFromColor(color: Int): Int {
            val colorTemperature = getColorTemperature(color)
            val alpha = 255 // alpha is managed separately

            // After: http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
            val temp = colorTemperature.toDouble() / 100.0f

            var red: Double
            if (temp <= 66)
                red = 255.0
            else {
                red = temp - 60
                red = 329.698727446 * Math.pow(red, -0.1332047592)
                if (red < 0) red = 0.0
                if (red > 255) red = 255.0
            }

            var green: Double
            if (temp <= 66) {
                green = temp
                green = 99.4708025861 * Math.log(green) - 161.1195681661
                if (green < 0) green = 0.0
                if (green > 255) green = 255.0
            } else {
                green = temp - 60
                green = 288.1221695283 * Math.pow(green, -0.0755148492)
                if (green < 0) green = 0.0
                if (green > 255) green = 255.0
            }

            var blue: Double
            if (temp >= 66)
                blue = 255.0
            else {
                if (temp < 19)
                    blue = 0.0
                else {
                    blue = temp - 10
                    blue = 138.5177312231 * Math.log(blue) - 305.0447927307
                    if (blue < 0) blue = 0.0
                    if (blue > 255) blue = 255.0
                }
            }

            return Color.argb(alpha, red.toInt(), green.toInt(), blue.toInt())
        }
    }
}
