package fr.julie.mariosprint.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class UiText(@param:StringRes val resource: Int, val args: List<Any> = emptyList())
@Composable internal fun UiText.resolve(): String = stringResource(resource, *args.toTypedArray())
