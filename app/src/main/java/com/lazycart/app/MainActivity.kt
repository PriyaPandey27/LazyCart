package com.lazycart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lazycart.app.ui.navigation.LazyCartNavGraph
import com.lazycart.app.ui.theme.LazyCartTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the Compose navigation graph.
 * All navigation and screen composition happens inside [LazyCartNavGraph].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LazyCartTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LazyCartNavGraph()
                }
            }
        }
    }
}
