package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NothingRed,
    secondary = NothingButtonGray,
    background = NothingBlack,
    surface = NothingBlack,
    onPrimary = NothingTextWhite,
    onSecondary = NothingTextWhite,
    onBackground = NothingTextWhite,
    onSurface = NothingTextWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NothingRed,
    secondary = NothingButtonGray,
    tertiary = NothingLightGray,
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  // Force Nothing OS dark theme
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
