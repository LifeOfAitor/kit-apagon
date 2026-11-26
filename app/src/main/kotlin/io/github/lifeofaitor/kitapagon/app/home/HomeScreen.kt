package io.github.lifeofaitor.kitapagon.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.lifeofaitor.kitapagon.app.R
import io.github.lifeofaitor.kitapagon.core.ui.component.EmergencyNumberNotice
import io.github.lifeofaitor.kitapagon.core.ui.theme.Spacing

@Composable
fun HomeScreen(
    isDialerUnavailable: Boolean,
    onCallEmergencyNumber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            HomeHeader()
            EmergencyNumberNotice(onCallEmergencyNumber = onCallEmergencyNumber)
            if (isDialerUnavailable) {
                Text(
                    text = stringResource(R.string.home_dialer_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = stringResource(R.string.home_description),
        style = MaterialTheme.typography.bodyLarge,
    )
}
