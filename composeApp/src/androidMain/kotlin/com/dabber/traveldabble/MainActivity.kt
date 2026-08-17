package com.dabber.traveldabble

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {}
        super.onCreate(savedInstanceState)

        // Lets the shared UI layer (commonMain) trigger the runtime location
        // permission flow without coupling to android.app.Activity.
        activityReference = this

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activityReference === this) activityReference = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}