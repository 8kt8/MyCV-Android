package com.katlewski.cv.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileVariantCacheTest {
    private val root: File = Files.createTempDirectory("variants-test").toFile()
    private val dir = File(root, FileVariantCache.DIR_NAME)
    private val cache = FileVariantCache(dir, Dispatchers.Unconfined)
    private val code = "Ab3dEf6hIj9L"

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun missingEntryIsNull() = runTest {
        assertNull(cache.get(code))
    }

    @Test
    fun putThenGetRoundTripsAndLeavesNoTempFiles() = runTest {
        cache.put(code, """{"a":1}""")
        cache.put(code, """{"a":2}""")

        assertEquals("""{"a":2}""", cache.get(code))
        assertEquals(listOf("$code.json"), dir.list()!!.toList())
    }

    @Test
    fun invalidCodesAreNeverWrittenOrRead() = runTest {
        cache.put("../../evil", "x")
        cache.put("short", "x")

        assertNull(cache.get("../../evil"))
        assertTrue(!dir.exists() || dir.list()!!.isEmpty())
    }
}
