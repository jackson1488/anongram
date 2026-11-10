package com.anongram.app.features.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anongram.app.features.auth.ui.welcome.WelcomeScreen as WelcomeScreenImpl

/**
 * Главный экран приветствия AnonGram.
 *
 * Делегирует рендеринг в [WelcomeScreenImpl] в подпапке welcome.
 */
@Composable
fun WelcomeScreen(
    onCreateIdentity: () -> Unit,
    onRestoreAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    WelcomeScreenImpl(
        onCreateIdentity = onCreateIdentity,
        onRestoreAccount = onRestoreAccount,
        modifier = modifier
    )
}
