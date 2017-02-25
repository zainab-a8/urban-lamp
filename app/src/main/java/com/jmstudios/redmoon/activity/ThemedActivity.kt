package com.jmstudios.redmoon.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.model.Config

abstract class ThemedAppCompatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeTheme)
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)
    }
}
