package com.katlewski.cv.android.data

import android.content.Context
import android.content.SharedPreferences
import com.katlewski.cv.data.VariantCode

/** Remembers the variant code from the last opened CV link, so a normal launch reopens that variant. */
interface ActiveVariantStore {
    /** The active variant code, or null for the default CV. Setting an invalid code clears it. */
    var code: String?
}

class SharedPrefsActiveVariantStore(private val prefs: SharedPreferences) : ActiveVariantStore {

    constructor(context: Context) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    override var code: String?
        // Invalid stored values read as null.
        get() = VariantCode.parse(prefs.getString(KEY_CODE, null))
        set(value) {
            val editor = prefs.edit()
            val valid = VariantCode.parse(value)
            if (valid == null) editor.remove(KEY_CODE) else editor.putString(KEY_CODE, valid)
            editor.apply()
        }

    private companion object {
        const val PREFS_NAME = "variant"
        const val KEY_CODE = "active_code"
    }
}
