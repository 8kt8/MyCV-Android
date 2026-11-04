package com.katlewski.cv.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.katlewski.cv.android.data.FileVariantCache
import com.katlewski.cv.android.data.SharedPrefsActiveVariantStore

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels {
        viewModelFactory {
            initializer {
                AppViewModel(
                    activeVariant = SharedPrefsActiveVariantStore(applicationContext),
                    variantCache = FileVariantCache(applicationContext),
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only for a fresh start: after recreation the link was already handled and the route is restored.
        if (savedInstanceState == null) appViewModel.onLink(intent?.dataString)
        setContent { CvApp(appViewModel) }
    }

    // launchMode="singleTop": a link opened while the app is in front arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appViewModel.onLink(intent.dataString)
    }
}
