package com.example.gibdd_ochevidec

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class LicenseActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val contents = assets.open("legal/licenses.txt").bufferedReader().use { it.readText() }
        setContentView(ScrollView(this).apply {
            addView(TextView(this@LicenseActivity).apply { text = contents; textSize = 15f; setPadding(28, 48, 28, 48) })
        })
    }
}
