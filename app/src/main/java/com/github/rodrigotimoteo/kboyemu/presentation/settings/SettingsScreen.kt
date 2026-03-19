package com.github.rodrigotimoteo.kboyemu.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.rodrigotimoteo.kboyemu.domain.emulation.EmulationSpeed
import com.github.rodrigotimoteo.kboyemu.presentation.settings.viewmodel.SettingsViewModel
import com.github.rodrigotimoteo.kboyemu.presentation.theme.GBGreen
import com.github.rodrigotimoteo.kboyemu.presentation.theme.GBShell
import org.koin.androidx.compose.koinViewModel

/**
 * Settings screen accessible from the emulator via the gear icon. Provides save/load state,
 * emulation speed control, change ROM, and about information.
 *
 * @param onBack callback invoked when pressing the back arrow (returns to emulator)
 * @param onChangeRom callback invoked when the user wants to change the ROM (navigates to home)
 * @param modifier modifier applied to the root layout
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangeRom: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val currentSpeed by viewModel.speed.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importSaveGame(it.toString())
            onBack()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportSaveGame(it.toString()) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionHeader(title = "Save States")

            SettingsItem(
                icon = Icons.Filled.Save,
                label = "Save State",
                subtitle = "Quick save current progress",
                onClick = {
                    viewModel.saveState()
                    onBack()
                },
            )

            SettingsItem(
                icon = Icons.Filled.Refresh,
                label = "Load State",
                subtitle = "Restore last saved progress",
                onClick = {
                    viewModel.loadState()
                    onBack()
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Emulation Speed")

            SpeedSelector(
                current = currentSpeed,
                onSelect = { viewModel.setSpeed(it) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Save Game")

            SettingsItem(
                icon = Icons.Filled.FileDownload,
                label = "Import .sav",
                subtitle = "Load a save game file from storage",
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )

            SettingsItem(
                icon = Icons.Filled.FileUpload,
                label = "Export .sav",
                subtitle = "Export current save game to a file",
                onClick = { exportLauncher.launch(viewModel.exportFileName) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Game")

            SettingsItem(
                icon = Icons.Filled.RestartAlt,
                label = "Reset",
                subtitle = "Restart the game from the beginning",
                onClick = {
                    viewModel.resetEmulation()
                    onBack()
                },
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Change ROM",
                subtitle = "Stop emulation and pick a different game",
                onClick = {
                    viewModel.stopEmulation()
                    onChangeRom()
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "About")

            SettingsItem(
                icon = Icons.Filled.Info,
                label = "KBoy Emulator",
                subtitle = "v1.0 — Game Boy & Game Boy Color emulator",
                onClick = {},
            )
        }
    }
}

/**
 * Grid of filter chips for selecting emulation speed. Laid out as two rows (3 + 2) so each
 * chip has enough room in portrait and fills the width in landscape.
 */
@Composable
private fun SpeedSelector(
    current: EmulationSpeed,
    onSelect: (EmulationSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = EmulationSpeed.entries
    val firstRow = entries.take(3)
    val secondRow = entries.drop(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            firstRow.forEach { speed ->
                SpeedChip(
                    speed = speed,
                    selected = speed == current,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            secondRow.forEach { speed ->
                SpeedChip(
                    speed = speed,
                    selected = speed == current,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Single speed filter chip that fills the space given by its parent
 */
@Composable
private fun SpeedChip(
    speed: EmulationSpeed,
    selected: Boolean,
    onSelect: (EmulationSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = { onSelect(speed) },
        label = {
            Text(
                text = speed.label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GBGreen,
            selectedLabelColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = GBGreen.copy(alpha = 0.5f),
            selectedBorderColor = GBGreen,
            enabled = true,
            selected = selected,
        ),
        modifier = modifier,
    )
}

/**
 * Section header label used to group related settings items
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = GBGreen,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
    )
}

/**
 * A single row in the settings list with an icon badge, label, and subtitle
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GBShell,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GBGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}




