/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017 Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */

package com.jmstudios.redmoon.filter

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

import com.jmstudios.redmoon.util.*


enum class Command {
    ON {
        override fun activate(service: FilterService) {
            Log.i("ON")
            service.startForeground(NOTIFICATION_ID, getNotification(true))
            service.start(DURATION_LONG)
        }
    },
    OFF {
        override fun activate(service: FilterService) {
            Log.i("OFF")
            service.stopForeground(true)
            service.pause(DURATION_LONG) {
                service.stopSelf()
            }
        }
    },
    PAUSE {
        override fun activate(service: FilterService) {
            Log.i("PAUSE")
            service.stopForeground(false)
            notify(service, getNotification(false))
            service.pause(DURATION_SHORT)
        }
    },
    SUSPEND {
        override fun activate(service: FilterService) {
            service.stopForeground(false)
            notify(service, getNotification(false))
            service.suspend(DURATION_SHORT)
        }
    },
    RESUME {
        override fun activate(service: FilterService) {
            Log.i("RESUME")
            service.startForeground(NOTIFICATION_ID, getNotification(true))
            service.start(DURATION_SHORT)
        }
    },
    SHOW_PREVIEW {
        override fun activate(service: FilterService) {
            Log.i("SHOW_PREVIEW: filterIsOn: $filterIsOn")
            if (filterWasOn == null) {
                filterWasOn = filterIsOn
                Log.i("SHOW_PREVIEW: filterWasOn set to: $filterWasOn")
            }
            service.startForeground(NOTIFICATION_ID, getNotification(true))
            service.start(DURATION_INSTANT)
        }
    },
    HIDE_PREVIEW {
        override fun activate(service: FilterService) {
            Log.i("HIDE_PREVIEW: filterWasOn: $filterWasOn")
            if (filterWasOn != true) {
                service.stopForeground(true)
                service.pause(DURATION_INSTANT) {
                    service.stopSelf()
                }
            }
            filterWasOn = null
        }
    };

    val intent: Intent
        get() = intent(FilterService::class).putExtra(EXTRA_COMMAND, ordinal)

    fun send(): ComponentName = appContext.startService(intent)

    abstract fun activate(service: FilterService)

    //override fun toString(): String = javaClass.simpleName

    companion object : Logger() {
        private const val EXTRA_COMMAND = "jmstudios.bundle.key.command"
        private const val COMMAND_MISSING = -1

        private const val NOTIFICATION_ID = 1

        private const val DURATION_LONG = 1000 // One second
        private const val DURATION_SHORT = 250
        private const val DURATION_INSTANT = 0

        private var filterWasOn: Boolean? = null
        
        fun handle(intent: Intent, svc: FilterService) {
            val flag = intent.getIntExtra(EXTRA_COMMAND, COMMAND_MISSING)
            Log.i("Recieved flag: $flag")
            if (flag != COMMAND_MISSING) {
                Command.values()[flag].activate(svc)
            } else {
                Log.w("Unknown intent recieved")
            }
        }

        fun toggle(on: Boolean) {
            if (on) ON.send() else OFF.send()
        }

        fun preview(on: Boolean) {
            if (on) SHOW_PREVIEW.send() else HIDE_PREVIEW.send()
        }

        private fun notify(context: Context, notification: Notification) {
            val nMan = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nMan.notify(NOTIFICATION_ID, notification)
        }

    }
}
