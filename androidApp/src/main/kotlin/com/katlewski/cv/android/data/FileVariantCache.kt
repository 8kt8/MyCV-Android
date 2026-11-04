package com.katlewski.cv.android.data

import android.content.Context
import com.katlewski.cv.data.VariantCache
import com.katlewski.cv.data.VariantCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Persistent [VariantCache]: one `<code>.json` file per variant in [dir], so a variant opens offline.
 * Writes go to a temp file that is renamed over the target, so a reader never sees a half-written file.
 */
class FileVariantCache(
    private val dir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VariantCache {

    constructor(context: Context) : this(File(context.filesDir, DIR_NAME))

    override suspend fun get(code: String): String? {
        val file = fileFor(code) ?: return null
        return withContext(ioDispatcher) {
            try {
                if (file.isFile) file.readText() else null
            } catch (e: IOException) {
                null
            }
        }
    }

    override suspend fun put(code: String, json: String) {
        val file = fileFor(code) ?: return
        withContext(ioDispatcher) {
            try {
                dir.mkdirs()
                // Unique temp name, so two concurrent writers never share a temp file.
                val temp = File.createTempFile("$code-", ".tmp", dir)
                try {
                    temp.writeText(json)
                    if (!temp.renameTo(file)) throw IOException("Couldn't rename ${temp.name}")
                } finally {
                    temp.delete() // no-op after a successful rename
                }
            } catch (e: IOException) {
                // Caching is best effort: the CV is already loaded, it just won't be available offline.
            }
        }
    }

    /** Only valid codes (`[A-Za-z0-9]{12}`) map to a file, so a code can never escape [dir]. */
    private fun fileFor(code: String): File? = VariantCode.parse(code)?.let { File(dir, "$it.json") }

    companion object {
        const val DIR_NAME = "variants"
    }
}
