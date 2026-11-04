package com.katlewski.cv.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.katlewski.cv.android.ui.CvScreen
import com.katlewski.cv.android.ui.CvViewModel
import com.katlewski.cv.android.ui.InactiveLinkScreen
import com.katlewski.cv.android.ui.theme.CvTheme

@Composable
fun CvApp(appViewModel: AppViewModel) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).crossfade(true).build()
    }
    CvTheme {
        val route by appViewModel.route.collectAsStateWithLifecycle()
        when (val current = route) {
            CvRoute.InactiveLink -> InactiveLinkScreen()
            is CvRoute.Content -> key(current.mode) {
                // A new mode gets a fresh store, so a fresh CvViewModel + CvService; the old one is cleared and closed.
                val owner = remember {
                    val store = appViewModel.viewModelStoreFor(current.mode)
                    object : ViewModelStoreOwner {
                        override val viewModelStore: ViewModelStore = store
                    }
                }
                val viewModel = viewModel(viewModelStoreOwner = owner) {
                    CvViewModel.create(current.mode, appViewModel.variantCache)
                }
                CvScreen(viewModel)
            }
        }
    }
}
