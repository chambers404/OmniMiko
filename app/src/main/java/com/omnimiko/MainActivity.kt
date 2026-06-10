package com.omnimiko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.omnimiko.ui.OmniMikoRoot
import com.omnimiko.ui.theme.OmniMikoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as OmniMikoApp).container
        setContent {
            OmniMikoTheme {
                OmniMikoRoot(container = container)
            }
        }
    }
}
