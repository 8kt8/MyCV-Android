package com.katlewski.cv.android

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.katlewski.cv.android.data.ActiveVariantStore
import com.katlewski.cv.data.CvMode
import com.katlewski.cv.data.VariantCache
import com.katlewski.cv.data.VariantCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the app shows. */
sealed interface CvRoute {
    data class Content(val mode: CvMode) : CvRoute

    /** Opened from a `/c/` link without a valid code. Never shows the default CV. */
    data object InactiveLink : CvRoute
}

/**
 * Activity-scoped: turns opened links into a [CvRoute] and owns a [ViewModelStore] for the current
 * [CvMode], so switching to another variant clears the old `CvViewModel` (closing its service)
 * while configuration changes keep it.
 */
class AppViewModel(
    private val activeVariant: ActiveVariantStore,
    val variantCache: VariantCache,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _route = MutableStateFlow(currentRoute())
    val route: StateFlow<CvRoute> = _route.asStateFlow()

    private var cvStoreKey: CvMode? = null
    private var cvStore = ViewModelStore()

    /**
     * Handles the data of a VIEW intent. A valid variant link becomes the active variant (also for later
     * normal launches); any other link shows [CvRoute.InactiveLink]. Null (a normal launch) changes nothing.
     */
    fun onLink(link: String?) {
        if (link.isNullOrBlank()) return
        val code = VariantCode.fromLink(link)
        if (code != null) activeVariant.code = code
        savedState[KEY_INACTIVE_LINK] = code == null
        val route = currentRoute()
        _route.value = route
        // Leaving the current mode (another variant, or the inactive-link screen) closes its service right away.
        val current = cvStoreKey
        if (current != null && route != CvRoute.Content(current)) {
            cvStore.clear()
            cvStoreKey = null
        }
    }

    /**
     * Store for the `CvViewModel` of [mode]: the same store for the same mode (so it survives configuration
     * changes), a new one for a different mode, which clears the previous store. Main thread only.
     */
    fun viewModelStoreFor(mode: CvMode): ViewModelStore {
        if (mode != cvStoreKey) {
            cvStore.clear()
            cvStore = ViewModelStore()
            cvStoreKey = mode
        }
        return cvStore
    }

    override fun onCleared() {
        cvStore.clear()
    }

    private fun currentRoute(): CvRoute = when {
        savedState.get<Boolean>(KEY_INACTIVE_LINK) == true -> CvRoute.InactiveLink
        else -> CvRoute.Content(activeVariant.code?.let { CvMode.Variant(it) } ?: CvMode.Default)
    }

    private companion object {
        const val KEY_INACTIVE_LINK = "inactive_link"
    }
}
