package com.mosque.prayerclock.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

@Composable
@ReadOnlyComposable
fun localizedStringResource(@StringRes id: Int): String {
    return LocalLocalizedContext.current.getString(id)
}

@Composable
@ReadOnlyComposable
fun localizedStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    return LocalLocalizedContext.current.getString(id, *formatArgs)
}

@Composable
@ReadOnlyComposable
fun localizedStringArrayResource(@StringRes id: Int): Array<String> {
    return LocalLocalizedContext.current.resources.getStringArray(id)
}