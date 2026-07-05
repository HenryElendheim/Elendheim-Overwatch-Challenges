package com.elendheim.overwatchchallenges.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.overwatchchallenges.data.Challenge
import com.elendheim.overwatchchallenges.data.PoolMode
import com.elendheim.overwatchchallenges.data.Role
import com.elendheim.overwatchchallenges.engine.RollResult
import com.elendheim.overwatchchallenges.ui.theme.Ash
import com.elendheim.overwatchchallenges.ui.theme.DamageRed
import com.elendheim.overwatchchallenges.ui.theme.SupportGreen
import com.elendheim.overwatchchallenges.ui.theme.TankBlue

private val Role.tint
    get() = when (this) {
        Role.TANK -> TankBlue
        Role.DAMAGE -> DamageRed
        Role.SUPPORT -> SupportGreen
    }

@Composable
fun RollScreen(viewModel: RollViewModel = viewModel()) {
    val state = viewModel.state
    var showSquadDialog by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Header(
                squadSeed = state.squadSeed,
                onSquadClick = { showSquadDialog = true },
            )

            Spacer(Modifier.height(16.dp))
            RoleFilterRow(selected = state.roleFilter, onSelect = viewModel::setRoleFilter)

            Spacer(Modifier.height(10.dp))
            PoolModeRow(selected = state.poolMode, onSelect = viewModel::setPoolMode)

            StakesRow(enabled = state.stakes, onToggle = viewModel::toggleStakes)

            Spacer(Modifier.height(6.dp))
            ResultCard(
                result = state.result,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::mutate,
                    enabled = state.result != null,
                    modifier = Modifier.weight(1f),
                ) { Text("Mutate") }
                OutlinedButton(
                    onClick = viewModel::escalate,
                    enabled = state.result != null,
                    modifier = Modifier.weight(1f),
                ) { Text("Escalate") }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = viewModel::roll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
            ) {
                Text(
                    text = if (state.result == null) "Roll" else "Roll again",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSquadDialog) {
        SquadSeedDialog(
            current = state.squadSeed,
            onDismiss = { showSquadDialog = false },
            onConfirm = { seed ->
                viewModel.setSquadSeed(seed)
                showSquadDialog = false
            },
        )
    }
}

@Composable
private fun Header(squadSeed: String?, onSquadClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "ELENDHEIM",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                color = Ash,
            )
            Text(
                text = "Overwatch Challenges",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        TextButton(onClick = onSquadClick) {
            Text(if (squadSeed == null) "Squad" else "Squad: $squadSeed")
        }
    }
}

@Composable
private fun RoleFilterRow(selected: Role?, onSelect: (Role?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("Any role") },
        )
        Role.entries.forEach { role ->
            FilterChip(
                selected = selected == role,
                onClick = { onSelect(role) },
                label = { Text(role.label) },
            )
        }
    }
}

@Composable
private fun PoolModeRow(selected: PoolMode, onSelect: (PoolMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        PoolMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = PoolMode.entries.size),
            ) { Text(mode.label) }
        }
    }
}

@Composable
private fun StakesRow(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Stakes: roll a punishment for failing",
            style = MaterialTheme.typography.bodyMedium,
            color = Ash,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ResultCard(result: RollResult?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        if (result == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Tap roll.\nGet a hero, get a constraint.\nSurvive it.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ash,
                )
            }
            return@Card
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = result.hero.role.label.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = result.hero.role.tint,
            )
            Text(
                text = result.hero.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            if (result.challenges.any { it.overridesHero }) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "One of these constraints picks your hero for you. The roll above is just a suggestion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ash,
                )
            }

            Spacer(Modifier.height(18.dp))
            result.challenges.forEachIndexed { index, challenge ->
                if (index > 0) {
                    HorizontalDivider(Modifier.padding(vertical = 14.dp))
                }
                ChallengeBlock(challenge = challenge)
            }

            result.punishment?.let { punishment ->
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "IF YOU FAIL",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = punishment,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun ChallengeBlock(challenge: Challenge) {
    Column {
        Text(
            text = "${challenge.category.label} · ${challenge.intensity.label}",
            style = MaterialTheme.typography.labelMedium,
            color = Ash,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = challenge.text,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun SquadSeedDialog(
    current: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var seedText by remember { mutableStateOf(current.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Squad sync") },
        text = {
            Column {
                Text(
                    "Everyone who enters the same word gets the same rolls, " +
                        "as long as your filters match and you tap in the same order. " +
                        "One chaos theme for the whole group."
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = seedText,
                    onValueChange = { seedText = it },
                    label = { Text("Squad word") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seedText) }) { Text("Sync") }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) { Text("Go solo") }
        },
    )
}
