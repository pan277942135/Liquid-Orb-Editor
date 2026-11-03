package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.aiorb.ui.AIOrbViewModel
import com.example.aiorb.ui.AIOrbVisualLabScreen
import com.example.ui.OrbScreen
import com.example.ui.OrbViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val aiOrbViewModel: AIOrbViewModel by viewModels()
    private val liquidOrbViewModel: OrbViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF070913)
                ) {
                    var currentMode by remember { mutableStateOf("ai_orb_lab") }

                    if (currentMode == "ai_orb_lab") {
                        AIOrbVisualLabScreen(viewModel = aiOrbViewModel)
                    } else {
                        OrbScreen(viewModel = liquidOrbViewModel)
                    }
                }
            }
        }
    }
}
