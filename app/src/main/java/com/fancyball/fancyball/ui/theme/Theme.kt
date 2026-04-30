package com.fancyball.fancyball.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.fancyball.fancyball.R

@Composable
fun FancyBallTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = colorResource(R.color.fb_gold),
        secondary = colorResource(R.color.fb_cyan),
        tertiary = colorResource(R.color.fb_purple),
        background = colorResource(R.color.fb_bg_deep),
        surface = colorResource(R.color.fb_surface),
        onPrimary = colorResource(R.color.fb_text_gold_dark),
        onSecondary = colorResource(R.color.fb_text_primary),
        onTertiary = colorResource(R.color.fb_text_primary),
        onBackground = colorResource(R.color.fb_text_primary),
        onSurface = colorResource(R.color.fb_text_primary)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
