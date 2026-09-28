package com.example.perutours

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.perutours.navigation.NavigationWrapper
import com.example.perutours.ui.theme.PeruToursTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val auth=Firebase.auth

        setContent {
            val navHostController= rememberNavController()
            PeruToursTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color= MaterialTheme.colorScheme.background
                ){
                    NavigationWrapper(navHostController,auth)
                }
            }
        }
    }
}
