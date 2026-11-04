package com.katlewski.cv.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katlewski.cv.data.Cv
import com.katlewski.cv.data.CvMode
import com.katlewski.cv.data.CvService
import com.katlewski.cv.data.VariantCache
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CvUiState(
    val cv: Cv? = null,
    val isRefreshing: Boolean = false,
    /** Nothing to show and loading failed (a variant with no cached copy). The screen offers a retry. */
    val loadFailed: Boolean = false,
    /** One-off message for a snackbar; clear it with [CvViewModel.onMessageShown]. */
    val message: String? = null,
)

/**
 * Offline-first: shows [CvService.initialCv] immediately (bundled CV for the default, cached copy for a variant),
 * then refreshes from the network. A failed refresh keeps the current content and only reports a message.
 * A variant with nothing cached shows [CvUiState.loadFailed] instead - never the default CV.
 *
 * One instance per [CvMode]; it owns the service and closes it when cleared.
 */
class CvViewModel(private val service: CvService) : ViewModel(service) {

    private val _state = MutableStateFlow(CvUiState())
    val state: StateFlow<CvUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = try {
                service.initialCv()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _state.update { it.copy(cv = initial) }
            refresh(userInitiated = false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch { refresh(userInitiated = true) }
    }

    /** Retry from the full-screen error state. */
    fun onRetry() = onRefresh()

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    private suspend fun refresh(userInitiated: Boolean) {
        if (_state.value.isRefreshing) return
        // Leaving the error state shows the progress indicator again while retrying.
        _state.update { it.copy(isRefreshing = userInitiated, loadFailed = false) }
        try {
            val cv = service.latestCv()
            _state.update { it.copy(cv = cv, isRefreshing = false, loadFailed = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                val hasContent = it.cv != null
                it.copy(
                    isRefreshing = false,
                    loadFailed = !hasContent,
                    // Without content the full-screen error already explains it.
                    message = if (userInitiated && hasContent) "Couldn't refresh - showing the saved CV" else null,
                )
            }
        }
    }

    companion object {
        fun create(mode: CvMode, cache: VariantCache): CvViewModel =
            CvViewModel(CvService(HttpClient(), mode = mode, cache = cache))
    }
}
