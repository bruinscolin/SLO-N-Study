package dev.csse.cbjl.slo_n_study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel) {

    val darkMode by themeViewModel.darkMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Dark Mode",
                style = MaterialTheme.typography.titleMedium
            )

            Switch(
                checked = darkMode,
                onCheckedChange = {
                    themeViewModel.toggleDarkMode(it)
                }
            )
        }
    }
}