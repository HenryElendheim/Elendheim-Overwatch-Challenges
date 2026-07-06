package com.elendheim.overwatchchallenges.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.overwatchchallenges.data.Challenge
import com.elendheim.overwatchchallenges.data.PoolMode
import com.elendheim.overwatchchallenges.data.Role
import com.elendheim.overwatchchallenges.data.Roster
import com.elendheim.overwatchchallenges.engine.RollResult
import com.elendheim.overwatchchallenges.ui.theme.Ash
import com.elendheim.overwatchchallenges.ui.theme.DamageRed
import com.elendheim.overwatchchallenges.ui.theme.SupportGreen
import com.elendheim.overwatchchallenges.ui.theme.TankBlue
import kotlin.random.Random
import kotlinx.coroutines.delay

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

            AnimatedContent(
                targetState = state.result,
                contentKey = { it != null },
                transitionSpec = {
                    (fadeIn(tween(450, delayMillis = 120)) +
                        scaleIn(tween(450, delayMillis = 120), initialScale = 0.92f))
                        .togetherWith(fadeOut(tween(160)))
                },
                label = "screen",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { result ->
                if (result == null) {
                    StartContent(onRoll = viewModel::roll)
                } else {
                    RolledContent(state = state, result = result, viewModel = viewModel)
                }
            }
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

/** First launch: nothing but the big button. Everything else arrives after the first roll. */
@Composable
private fun StartContent(onRoll: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
            label = "pulseScale",
        )
        Button(
            onClick = onRoll,
            shape = CircleShape,
            modifier = Modifier
                .size(210.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
        ) {
            Text(
                text = "Roll",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Get a hero. Get a constraint. Survive it.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ash,
        )
    }
}

@Composable
private fun RolledContent(state: RollUiState, result: RollResult, viewModel: RollViewModel) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))
        RoleFilterRow(selected = state.roleFilter, onSelect = viewModel::setRoleFilter)

        Spacer(Modifier.height(10.dp))
        PoolModeRow(selected = state.poolMode, onSelect = viewModel::setPoolMode)

        StakesRow(enabled = state.stakes, onToggle = viewModel::toggleStakes)

        Spacer(Modifier.height(6.dp))
        ResultCard(
            state = state,
            result = result,
            onRemove = viewModel::removeChallenge,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = viewModel::mutate, modifier = Modifier.weight(1f)) {
                Text("Mutate")
            }
            OutlinedButton(onClick = viewModel::escalate, modifier = Modifier.weight(1f)) {
                Text("Escalate")
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = viewModel::roll,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
        ) {
            Text("Roll again", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Mutations this roll: ${state.mutations}",
            style = MaterialTheme.typography.labelMedium,
            color = Ash,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(10.dp))
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
private fun ResultCard(
    state: RollUiState,
    result: RollResult,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shownHero by remember { mutableStateOf(result.hero) }
    var revealed by remember { mutableStateOf(false) }

    // the slot-machine pass: flick through the roster, slow down, land on the
    // rolled hero, then let the constraints in
    LaunchedEffect(state.rollId) {
        revealed = false
        val pool = state.roleFilter?.let(Roster::byRole) ?: Roster.heroes
        val spinRandom = Random(state.rollId.toLong())
        var stepMs = 45L
        while (stepMs < 330L) {
            shownHero = pool.random(spinRandom)
            delay(stepMs)
            stepMs = (stepMs * 5 / 4) + 8
        }
        shownHero = result.hero
        delay(140)
        revealed = true
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = shownHero.role.label.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = shownHero.role.tint,
            )
            AnimatedContent(
                targetState = shownHero,
                transitionSpec = {
                    (slideInVertically(tween(90)) { it / 2 } + fadeIn(tween(90)))
                        .togetherWith(slideOutVertically(tween(90)) { -it / 2 } + fadeOut(tween(90)))
                },
                label = "heroSpin",
            ) { hero ->
                Text(
                    text = hero.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
            ) {
                Column(Modifier.animateContentSize()) {
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
                        ChallengeItem(
                            challenge = challenge,
                            index = index,
                            removable = index > 0,
                            onRemove = { onRemove(index) },
                        )
                    }

                    AnimatedVisibility(
                        visible = state.stakes,
                        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 4 },
                        exit = fadeOut(tween(200)),
                    ) {
                        Column {
                            Spacer(Modifier.height(22.dp))
                            Text(
                                text = "IF YOU FAIL",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = result.punishment,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeItem(
    challenge: Challenge,
    index: Int,
    removable: Boolean,
    onRemove: () -> Unit,
) {
    // key on the text so a mutate or escalate composes a fresh item that
    // plays its own entrance
    androidx.compose.runtime.key(challenge.text) {
        val appear = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = appear,
            enter = fadeIn(tween(300, delayMillis = index * 70)) +
                slideInVertically(tween(300, delayMillis = index * 70)) { it / 3 },
        ) {
            Column {
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 14.dp))
                if (removable) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) onRemove()
                            true
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Removed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    ) {
                        ChallengeBlock(challenge)
                    }
                } else {
                    ChallengeBlock(challenge)
                }
            }
        }
    }
}

@Composable
private fun ChallengeBlock(challenge: Challenge) {
    Column(Modifier.fillMaxWidth()) {
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
