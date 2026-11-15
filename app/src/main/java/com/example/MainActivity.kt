package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.GameRepository
import com.example.ui.GameDexApp
import com.example.ui.GameDexViewModel
import com.example.ui.auth.AuthManager
import com.example.ui.auth.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationRoot()
                }
            }
        }
    }
}

@Composable
fun AppNavigationRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val firebaseAuth = remember { runCatching { Firebase.auth }.getOrNull() }
    var currentUser by remember { mutableStateOf(firebaseAuth?.currentUser) }

    DisposableEffect(firebaseAuth) {
        if (firebaseAuth == null) {
            onDispose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            firebaseAuth.addAuthStateListener(listener)
            onDispose {
                firebaseAuth.removeAuthStateListener(listener)
            }
        }
    }

    // Attempt silent auto-sign in on startup
    LaunchedEffect(firebaseAuth) {
        if (firebaseAuth != null && firebaseAuth.currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = firebaseAuth.currentUser },
                onUnauthenticated = { },
                scope = this
            )
        }
    }

    val user = currentUser
    if (user == null) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = firebaseAuth?.currentUser
            }
        )
    } else {
        val userUid = user.uid
        val repository = remember(userUid) { GameRepository(context.applicationContext) }
        val viewModel: GameDexViewModel = viewModel(
            key = "gamedex_$userUid",
            factory = remember(userUid, repository) { GameDexViewModel.factory(repository) }
        )

        GameDexApp(
            viewModel = viewModel,
            onSignOut = {
                AuthManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = { currentUser = null },
                    scope = scope
                )
            }
        )
    }
}
