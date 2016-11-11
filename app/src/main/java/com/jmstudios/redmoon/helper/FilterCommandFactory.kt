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

import android.content.Context
import android.content.Intent

import com.jmstudios.redmoon.service.ScreenFilterService

/**
 * Factory class to construct a valid [Intent] commands that can be sent to
 * [com.jmstudios.redmoon.service.ScreenFilterService].

 *
 * Use [FilterCommandSender] to execute the constructed commands.
 */
class FilterCommandFactory(private val mContext: Context) {

    /**
     * @param commandFlag one of [ScreenFilterService.COMMAND_ON]
     * *        or [ScreenFilterService.COMMAND_PAUSE].
     * *
     * @return an Intent containing a command that can be sent to [ScreenFilterService] via
     * *         [FilterCommandSender.send]; null if
     * *         `screenFilterServiceCommand` is invalid.
     */
    fun createCommand(commandFlag: Int): Intent {
        val command = if (commandFlag in ScreenFilterService.VALID_COMMAND_START..
                                  ScreenFilterService.VALID_COMMAND_END) {
            commandFlag
        } else ScreenFilterService.COMMAND_PAUSE
        
        return Intent(mContext, ScreenFilterService::class.java)
                .putExtra(ScreenFilterService.BUNDLE_KEY_COMMAND, command)
    }
}
