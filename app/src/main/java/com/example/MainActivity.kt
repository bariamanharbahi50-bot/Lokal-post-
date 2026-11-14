package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.auth.AuthManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GovernmentTermsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.LokalPostTheme
import com.example.ui.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth

enum class AppScreen {
    Splash,
    Auth,
    GovernmentTerms,
    Home
}

class MainActivity : ComponentActivity() {

    private val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager = AuthManager(applicationContext)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Splash) }

            LokalPostTheme {
                when (currentScreen) {
                    AppScreen.Splash -> {
                        SplashScreen(
                            onTimeout = {
                                val firebaseUser = try {
                                    FirebaseAuth.getInstance().currentUser
                                } catch (_: Exception) {
                                    null
                                }
                                val isLoggedIn = authManager.isLoggedIn() || firebaseUser != null
                                val termsAccepted = authManager.areTermsAccepted()

                                // Second time: if isLoggedIn && termsAccepted -> directly open Home
                                // If logged in but terms not yet accepted -> show GovernmentTerms
                                // Otherwise -> show Sign Up (AuthScreen)
                                currentScreen = when {
                                    isLoggedIn && termsAccepted -> AppScreen.Home
                                    isLoggedIn && !termsAccepted -> AppScreen.GovernmentTerms
                                    else -> AppScreen.Auth
                                }
                            }
                        )
                    }
                    AppScreen.Auth -> {
                        AuthScreen(
                            onAuthSuccess = {
                                authManager.setLoggedIn(true)
                                currentScreen = if (authManager.areTermsAccepted()) {
                                    AppScreen.Home
                                } else {
                                    AppScreen.GovernmentTerms
                                }
                            }
                        )
                    }
                    AppScreen.GovernmentTerms -> {
                        GovernmentTermsScreen(
                            onAcceptTerms = {
                                authManager.setLoggedIn(true)
                                authManager.setTermsAccepted(true)
                                currentScreen = AppScreen.Home
                            },
                            onBack = {
                                currentScreen = AppScreen.Auth
                            }
                        )
                    }
                    AppScreen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onLogout = {
                                authManager.logout()
                                currentScreen = AppScreen.Auth
                            }
                        )
                    }
                }
            }
        }
    }
}
