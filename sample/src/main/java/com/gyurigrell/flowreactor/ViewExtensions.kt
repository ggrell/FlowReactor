//@file:JvmName("BindingView")
//@file:JvmMultifileClass

package com.gyurigrell.flowreactor

import android.view.View
import androidx.annotation.CheckResult

/**
 * An action which sets the visibility property of `view`.
 *
 * *Warning:* The created observable keeps a strong reference to `view`. Unsubscribe to free this
 * reference.
 *
 * @param visibilityWhenFalse Visibility to set on a `false` value (`View.INVISIBLE` or
 * `View.GONE`).
 */
@CheckResult
//@JvmOverloads
fun View.visibility(visibilityWhenFalse: Int = View.GONE): suspend (Boolean) -> Unit {
    require(visibilityWhenFalse != View.VISIBLE) {
        "Setting visibility to VISIBLE when false would have no effect."
    }
    require(visibilityWhenFalse == View.INVISIBLE || visibilityWhenFalse == View.GONE) {
        "Must set visibility to INVISIBLE or GONE when false."
    }
    return { value -> visibility = if (value) View.VISIBLE else visibilityWhenFalse }
}