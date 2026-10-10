package com.example.perutours

import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.perutours.navigation.NavigationWrapper
import com.example.perutours.service.MyFirebaseMessagingService
import com.example.perutours.ui.theme.PeruToursTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


class MainActivity : ComponentActivity() {
    private val pendingQuotationId = mutableStateOf<String?>(null)
    private val pendingNotificationType = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingQuotationId.value = intent.quotationId()
        pendingNotificationType.value = intent.notificationType()

        val auth=Firebase.auth

        setContent {
            val navHostController= rememberNavController()
            PeruToursTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color= MaterialTheme.colorScheme.background
                ){
                    NavigationWrapper(
                        navHostController = navHostController,
                        auth = auth,
                        pendingQuotationId = pendingQuotationId.value,
                        pendingNotificationType = pendingNotificationType.value,
                        onPendingQuotationConsumed = {
                            pendingQuotationId.value = null
                            pendingNotificationType.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingQuotationId.value = intent.quotationId()
        pendingNotificationType.value = intent.notificationType()
    }

    private fun Intent.quotationId(): String? =
        getStringExtra(MyFirebaseMessagingService.EXTRA_QUOTATION_ID)
            ?: getStringExtra("quotationId")

    private fun Intent.notificationType(): String? =
        getStringExtra(MyFirebaseMessagingService.EXTRA_NOTIFICATION_TYPE)
            ?: getStringExtra("type")
}
