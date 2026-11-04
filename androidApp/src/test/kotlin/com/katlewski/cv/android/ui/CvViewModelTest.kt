package com.katlewski.cv.android.ui

import androidx.lifecycle.viewModelScope
import com.katlewski.cv.data.CvMode
import com.katlewski.cv.data.CvService
import com.katlewski.cv.data.InMemoryVariantCache
import com.katlewski.cv.data.VariantCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CvViewModelTest {
    private val baseUrl = "https://cv.example.com"
    private val code = "Ab3dEf6hIj9L"

    // Real-time Main dispatcher: the service's 8 s request timeout must not fire on virtual time
    // while MockEngine answers on another thread (same reason as CvApi's CvServiceTest).
    private lateinit var main: ExecutorCoroutineDispatcher
    private val requests = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        Dispatchers.setMain(main)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        main.close()
    }

    private fun cvJson(name: String) = """{"profile":{"fullName":"$name","headline":"h","about":"a"}}"""

    /** [handler] returns the response body for a URL, or null for "offline". */
    private fun viewModel(
        mode: CvMode,
        cache: VariantCache = InMemoryVariantCache(),
        handler: (url: String) -> String?,
    ): CvViewModel {
        val engine = MockEngine { request ->
            val url = request.url.toString()
            synchronized(requests) { requests += url }
            val body = handler(url) ?: throw IllegalStateException("offline")
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return CvViewModel(CvService(HttpClient(engine), baseUrl, mode, cache))
    }

    /** Waits (in real time) until the ViewModel's initial load and any refreshes have finished. */
    private fun CvViewModel.awaitIdle(): CvUiState = runBlocking {
        withTimeout(5_000) {
            val job = viewModelScope.coroutineContext.job
            while (true) {
                val running = job.children.toList()
                if (running.isEmpty()) break
                running.joinAll()
            }
        }
        state.value
    }

    @Test
    fun variantNetworkFailureShowsErrorAndNeverDefault() {
        val vm = viewModel(CvMode.Variant(code)) { url ->
            if (url.endsWith("/default.json")) cvJson("Default Person") else null
        }

        val state = vm.awaitIdle()

        assertTrue(state.loadFailed)
        assertNull(state.cv)
        assertNull(state.message) // the full-screen error explains it; no snackbar
        assertEquals(listOf("$baseUrl/v/$code.json"), synchronized(requests) { requests.toList() })
    }

    @Test
    fun variantRetryFailureStaysOnErrorWithoutDefault() {
        val vm = viewModel(CvMode.Variant(code)) { url ->
            if (url.endsWith("/default.json")) cvJson("Default Person") else null
        }
        vm.awaitIdle()

        vm.onRetry()
        val state = vm.awaitIdle()

        assertTrue(state.loadFailed)
        assertNull(state.cv)
        assertNull(state.message)
        assertEquals(listOf("$baseUrl/v/$code.json", "$baseUrl/v/$code.json"), synchronized(requests) { requests.toList() })
    }

    @Test
    fun variantRetryAfterFailureLoadsVariant() {
        val online = AtomicBoolean(false)
        val vm = viewModel(CvMode.Variant(code)) { url ->
            if (online.get() && url.endsWith("/v/$code.json")) cvJson("Variant Person") else null
        }
        assertTrue(vm.awaitIdle().loadFailed)

        online.set(true)
        vm.onRetry()
        val state = vm.awaitIdle()

        assertEquals("Variant Person", state.cv?.profile?.fullName)
        assertFalse(state.loadFailed)
    }

    @Test
    fun variantWithCacheShowsCachedCopyWhenOffline() {
        val cache = InMemoryVariantCache()
        runBlocking { cache.put(code, cvJson("Cached Person")) }
        val vm = viewModel(CvMode.Variant(code), cache) { url ->
            if (url.endsWith("/default.json")) cvJson("Default Person") else null
        }

        // Background refresh failed: cached content stays, no error, no snackbar.
        val state = vm.awaitIdle()
        assertEquals("Cached Person", state.cv?.profile?.fullName)
        assertFalse(state.loadFailed)
        assertNull(state.message)

        // A user-initiated refresh failure keeps the cache and shows a snackbar.
        vm.onRefresh()
        val afterRefresh = vm.awaitIdle()
        assertEquals("Cached Person", afterRefresh.cv?.profile?.fullName)
        assertFalse(afterRefresh.loadFailed)
        assertNotNull(afterRefresh.message)
        assertTrue(synchronized(requests) { requests.toList() }.none { it.endsWith("/default.json") })
    }

    @Test
    fun variantWithCacheIsReplacedByNetworkCopy() {
        val cache = InMemoryVariantCache()
        runBlocking { cache.put(code, cvJson("Cached Person")) }
        val vm = viewModel(CvMode.Variant(code), cache) { cvJson("Fresh Person") }

        val state = vm.awaitIdle()

        assertEquals("Fresh Person", state.cv?.profile?.fullName)
        assertFalse(state.loadFailed)
        assertEquals(cvJson("Fresh Person"), runBlocking { cache.get(code) })
    }

    @Test
    fun defaultShowsBundledThenNetworkCv() {
        val vm = viewModel(CvMode.Default) { url ->
            if (url.endsWith("/default.json")) cvJson("Default Person") else null
        }

        val state = vm.awaitIdle()

        assertEquals("Default Person", state.cv?.profile?.fullName)
        assertFalse(state.loadFailed)
        assertNull(state.message)
        assertEquals(listOf("$baseUrl/default.json"), synchronized(requests) { requests.toList() })
    }

    @Test
    fun defaultOfflineKeepsBundledCv() {
        val vm = viewModel(CvMode.Default) { null }

        // Bundled sample data (CvApi's sample-cv.json); no error and no snackbar for the background refresh.
        val state = vm.awaitIdle()
        assertEquals("Alex Sample", state.cv?.profile?.fullName)
        assertFalse(state.loadFailed)
        assertNull(state.message)

        vm.onRefresh()
        val afterRefresh = vm.awaitIdle()
        assertEquals("Alex Sample", afterRefresh.cv?.profile?.fullName)
        assertFalse(afterRefresh.loadFailed)
        assertNotNull(afterRefresh.message)
    }
}
