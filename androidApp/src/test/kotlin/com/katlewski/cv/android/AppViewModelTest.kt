package com.katlewski.cv.android

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.katlewski.cv.android.data.ActiveVariantStore
import com.katlewski.cv.data.CvMode
import com.katlewski.cv.data.InMemoryVariantCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppViewModelTest {
    private val code = "Ab3dEf6hIj9L"
    private val otherCode = "Zz9yXx8wVv7U"

    private class FakeStore(override var code: String? = null) : ActiveVariantStore

    private class Probe : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    private fun appViewModel(store: ActiveVariantStore = FakeStore(), saved: SavedStateHandle = SavedStateHandle()) =
        AppViewModel(store, InMemoryVariantCache(), saved)

    private fun AppViewModel.probeFor(mode: CvMode): Probe {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = Probe() as T
        }
        return ViewModelProvider(viewModelStoreFor(mode), factory)[Probe::class.java]
    }

    @Test
    fun normalLaunchShowsDefault() {
        val vm = appViewModel()
        vm.onLink(null)
        assertEquals(CvRoute.Content(CvMode.Default), vm.route.value)
    }

    @Test
    fun normalLaunchReopensActiveVariant() {
        val vm = appViewModel(FakeStore(code))
        vm.onLink(null)
        assertEquals(CvRoute.Content(CvMode.Variant(code)), vm.route.value)
    }

    @Test
    fun validLinkBecomesActiveVariant() {
        val store = FakeStore()
        val vm = appViewModel(store)
        vm.onLink("https://mycv-szk.pages.dev/c/$code")
        assertEquals(CvRoute.Content(CvMode.Variant(code)), vm.route.value)
        assertEquals(code, store.code)
    }

    @Test
    fun invalidLinkShowsInactiveAndKeepsActiveVariant() {
        val store = FakeStore(code)
        val saved = SavedStateHandle()
        val vm = appViewModel(store, saved)
        vm.onLink("https://mycv-szk.pages.dev/c/nope")
        assertEquals(CvRoute.InactiveLink, vm.route.value)
        assertEquals(code, store.code)

        // Restored after process death from the saved state.
        assertEquals(CvRoute.InactiveLink, appViewModel(store, saved).route.value)

        // A later valid link leaves the inactive state.
        vm.onLink("https://mycv-szk.pages.dev/c/$otherCode")
        assertEquals(CvRoute.Content(CvMode.Variant(otherCode)), vm.route.value)
    }

    @Test
    fun sameModeKeepsViewModelAndNewModeClearsIt() {
        val vm = appViewModel()
        val first = vm.probeFor(CvMode.Default)
        assertSame(first, vm.probeFor(CvMode.Default))
        assertFalse(first.cleared)

        vm.onLink("https://mycv-szk.pages.dev/c/$code")
        assertTrue(first.cleared)

        val second = vm.probeFor(CvMode.Variant(code))
        assertNotSame<ViewModel>(first, second)
        vm.onLink("https://mycv-szk.pages.dev/c/$code") // same code again: nothing changes
        assertFalse(second.cleared)

        vm.onLink("https://mycv-szk.pages.dev/c/bad")
        assertTrue(second.cleared)
    }
}
