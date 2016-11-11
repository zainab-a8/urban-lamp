/* Copyright (c) 2015 Chris Nguyen
**
** Permission to use, copy, modify, and/or distribute this software for
** any purpose with or without fee is hereby granted, provided that the
** above copyright notice and this permission notice appear in all copies.
**
** THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
** WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
** WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR
** BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES
** OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
** WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
** ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
** SOFTWARE.
*/
package com.jmstudios.redmoon.helper

import android.content.Intent

import com.jmstudios.redmoon.service.ScreenFilterService

/**
 * Helper class that encapsulates the logic to parse an [Intent] that was created by
 * [FilterCommandFactory] and sent to [ScreenFilterService].
 */
class FilterCommandParser {

    /**
     * Retrieves the command in an intent sent to [ScreenFilterService].

     * @param intent that was constructed by [FilterCommandFactory].
     * *
     * @return one of [ScreenFilterService.COMMAND_OFF], [ScreenFilterService.COMMAND_ON],
     * *         [ScreenFilterService.COMMAND_PAUSE], or -1 if `intent` doesn't contain a
     * *         valid command.
     */
    fun parseCommandFlag(intent: Intent?): Int {
        val errorCode = -1

        if (intent == null) {
            return errorCode
        }

        val commandFlag = intent.getIntExtra(ScreenFilterService.BUNDLE_KEY_COMMAND, errorCode)
        return commandFlag
    }
}
