package com.example.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedOrderElement(
    key: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
): Modifier {
    val sScope = sharedTransitionScope ?: LocalSharedTransitionScope.current
    val aScope = animatedVisibilityScope ?: LocalAnimatedVisibilityScope.current
    if (sScope != null && aScope != null) {
        with(sScope) {
            return this@sharedOrderElement.sharedElement(
                rememberSharedContentState(key = key),
                animatedVisibilityScope = aScope
            )
        }
    }
    return this
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedOrderBounds(
    key: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
): Modifier {
    val sScope = sharedTransitionScope ?: LocalSharedTransitionScope.current
    val aScope = animatedVisibilityScope ?: LocalAnimatedVisibilityScope.current
    if (sScope != null && aScope != null) {
        with(sScope) {
            return this@sharedOrderBounds.sharedBounds(
                rememberSharedContentState(key = key),
                animatedVisibilityScope = aScope
            )
        }
    }
    return this
}

