package com.anish.owee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.anish.owee.navigation.RootNavGraph
import com.anish.owee.ui.theme.OweeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OweeTheme {
                val navController = rememberNavController()
                RootNavGraph(navController = navController)
            }
        }
    }
}
