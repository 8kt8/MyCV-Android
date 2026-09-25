package com.katlewski.cv.android

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.katlewski.cv.android.ui.CvScreen
import com.katlewski.cv.android.ui.CvViewModel
import com.katlewski.cv.android.ui.theme.CvTheme
import com.katlewski.cv.data.CvService

@Composable
fun CvApp() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).crossfade(true).build()
    }
    CvTheme {
        // The ViewModel owns the service and closes it in onCleared.
        val viewModel = viewModel { CvViewModel(CvService()) }
        CvScreen(viewModel)
    }
}
