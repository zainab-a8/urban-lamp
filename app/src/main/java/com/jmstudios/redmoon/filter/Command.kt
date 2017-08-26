/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */

package com.jmstudios.redmoon.filter

import android.content.ComponentName
import android.content.Intent

import com.jmstudios.redmoon.util.*

enum class Command {
    ON {
        override fun activate(service: FilterService) {
            service.start()
            state = this
        }
    },
    OFF {
        override fun activate(service: FilterService) {
            service.stop()
            state = this
        }
    },
    PAUSE {
        override fun activate(service: FilterService) {
            service.pause()
            state = this
        }
    },
    /* This command is used when the service is suspended temporarily,
     * because the user is in an excluded app (for example the package
     * installer). It stops the service like in the OffState, but
     * doesn't change the UI, switch or brightness state just like the
     * PreviewState. Like the PreviewState, it logs changes to the
     * state and applies them when the suspend state is deactivated.
    */
    SUSPEND {
        override fun activate(service: FilterService) {
            service.pause()
            state = this
        }
    },
    RESUME {
        override fun activate(service: FilterService) {
            service.start()
            state = this
        }
    },
    FADE_ON {
        override fun activate(service: FilterService) {
            service.start()
            state = this
        }
    },
    FADE_OFF {
        override fun activate(service: FilterService) {
            service.stop()
            state = this
        }
    },
    /* This State is used to present the service to the user when (s)he
     * is holding one of the seekbars to adjust the service. It turns
     * on the service and saves what state it should be when it will be
     * turned off.
     */
    SHOW_PREVIEW {
        var presses: Int = 0
        lateinit var returnState: Command

        override fun activate(service: FilterService) {
            if (state == HIDE_PREVIEW) presses-- else presses++
            Log.d("$presses presses active")

            when (presses) {
                0 -> {
                    Log.i("Moving back to state: $returnState")
                    returnState.activate(service)
                }
                1 -> {
                    returnState = state
                    state = this
                    service.preview()
                }
            }
        }
    },
    HIDE_PREVIEW {
        override fun activate(service: FilterService) {
            state = this
            SHOW_PREVIEW.activate(service)
        }
    };

    val intent: Intent
        get() = intent(FilterService::class).putExtra(BUNDLE_KEY_COMMAND, this.ordinal)

    fun send(): ComponentName = appContext.startService(intent)

    abstract fun activate(service: FilterService)

    //override fun toString(): String = javaClass.simpleName

    companion object : Logger() {
        const val BUNDLE_KEY_COMMAND = "jmstudios.bundle.key.command"

        fun toggle(on: Boolean) {
            if (on) ON.send() else OFF.send()
        }

        fun preview(on: Boolean) {
            if (on) SHOW_PREVIEW.send() else HIDE_PREVIEW.send()
        }

        private var state: Command = OFF
            set(value) {
                Log.i("State changed from ${field.name} to ${value.name}")
                field = value
            }
    }
}

