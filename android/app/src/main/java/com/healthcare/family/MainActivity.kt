package com.healthcare.family

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.healthcare.family.data.local.TokenManager
import com.healthcare.family.navigation.AppNavGraph
import com.healthcare.family.ui.theme.HealthCareTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isElderlyMode by tokenManager.isElderlyMode.collectAsState(initial = false)

            HealthCareTheme(isElderlyMode = isElderlyMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn by tokenManager.isLoggedIn.collectAsState(initial = false)

                    AppNavGraph(
                        navController = navController,
                        tokenManager = tokenManager,
                        isLoggedIn = isLoggedIn,
                    )
                }
            }
        }
    }
}
