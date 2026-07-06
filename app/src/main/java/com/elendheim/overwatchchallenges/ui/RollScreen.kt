package com.elendheim.overwatchchallenges.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.overwatchchallenges.data.Challenge
import com.elendheim.overwatchchallenges.data.Hero
import com.elendheim.overwatchchallenges.data.PoolMode
import com.elendheim.overwatchchallenges.data.Role
import com.elendheim.overwatchchallenges.data.Roster
import com.elendheim.overwatchchallenges.engine.RollResult
import com.elendheim.overwatchchallenges.ui.theme.Ash
import com.elendheim.overwatchchallenges.ui.theme.DamageRed
import com.elendheim.overwatchchallenges.ui.theme.SupportGreen
import com.elendheim.overwatchchallenges.ui.theme.TankBlue
import kotlin.math.abs
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
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var landedRollId by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = showSettings) { showSettings = false }

    val screenKey = when {
        showSettings -> "settings"
        state.result == null -> "start"
        state.rollId > landedRollId -> "spin"
        else -> "result"
    }

    Scaffold { innerPadding ->
        AnimatedContent(
            targetState = screenKey,
            transitionSpec = {
                (fadeIn(tween(420, delayMillis = 90)) +
                    scaleIn(tween(420, delayMillis = 90), initialScale = 0.94f))
                    .togetherWith(fadeOut(tween(150)))
            },
            label = "screen",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { screen ->
            when (screen) {
                "settings" -> SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { showSettings = false },
                )

                "start" -> StartScreen(
                    onRoll = viewModel::roll,
                    onSettings = { showSettings = true },
                )

                "spin" -> SpinScreen(
                    state = state,
                    onLanded = { landedRollId = state.rollId },
                )

                else -> state.result?.let { result ->
                    ResultScreen(
                        state = state,
                        result = result,
                        viewModel = viewModel,
                        onSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun StartScreen(onRoll: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Header(onSettings = onSettings)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
}

/**
 * The full-screen reel. Every hero still in the pool flies past at least
 * once, the scroll decelerates with a slight overshoot, and it settles on
 * the rolled hero before handing over to the result screen.
 */
@Composable
private fun SpinScreen(state: RollUiState, onLanded: () -> Unit) {
    val result = state.result ?: return
    val reel = remember(state.rollId) {
        val base = state.roleFilter?.let(Roster::byRole) ?: Roster.heroes
        val pool = base.filterNot { it.name in state.disabledHeroes }.ifEmpty { base }
        buildReel(pool, result.hero, state.rollId)
    }
    // the target sits two entries before the end so the overshoot has
    // something to scroll past
    val targetIndex = reel.size - 3
    val itemPx = with(LocalDensity.current) { 96.dp.toPx() }
    val offset = remember(state.rollId) { Animatable(0f) }

    LaunchedEffect(state.rollId) {
        offset.animateTo(
            targetValue = targetIndex * itemPx,
            animationSpec = tween(
                durationMillis = (1600 + reel.size * 24).coerceAtMost(3200),
                easing = CubicBezierEasing(0.16f, 0.84f, 0.28f, 1.04f),
            ),
        )
        delay(340)
        onLanded()
    }

    Box(Modifier.fillMaxSize()) {
        reel.forEachIndexed { index, hero ->
            Text(
                text = hero.name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = hero.role.tint,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val dy = (index - offset.value / itemPx) * itemPx
                        translationY = dy
                        val distance = abs(dy) / (itemPx * 3f)
                        alpha = (1f - distance * distance).coerceIn(0f, 1f)
                        val scale = 1f - 0.3f * (abs(dy) / (itemPx * 2f)).coerceAtMost(1f)
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}

private fun buildReel(pool: List<Hero>, target: Hero, rollId: Int): List<Hero> {
    val random = Random(rollId * 6007L + 97)
    val lead = mutableListOf<Hero>()
    lead += pool.shuffled(random)
    while (lead.size < 22) lead += pool.shuffled(random)
    if (lead.last() == target) lead.removeAt(lead.lastIndex)
    return lead + target + List(2) { pool.random(random) }
}

@Composable
private fun ResultScreen(
    state: RollUiState,
    result: RollResult,
    viewModel: RollViewModel,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Header(onSettings = onSettings)

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
private fun Header(onSettings: () -> Unit) {
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
        TextButton(onClick = onSettings) {
            Text("Settings")
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .animateContentSize()
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

@Composable
private fun ChallengeItem(
    challenge: Challenge,
    index: Int,
    removable: Boolean,
    onRemove: () -> Unit,
) {
    // key on the text so a mutate or escalate composes a fresh item that
    // plays its own entrance
    key(challenge.text) {
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
                        backgroundContent = {},
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    state: RollUiState,
    viewModel: RollViewModel,
    onBack: () -> Unit,
) {
    var seedText by remember { mutableStateOf(state.squadSeed.orEmpty()) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text("Done") }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Squad sync", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Everyone who enters the same word gets the same rolls, as long as " +
                    "filters and hero bans match and you tap in the same order. Setting a " +
                    "word starts a fresh sequence.",
                style = MaterialTheme.typography.bodySmall,
                color = Ash,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = seedText,
                onValueChange = { seedText = it },
                label = { Text("Squad word") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.setSquadSeed(seedText) },
                    modifier = Modifier.weight(1f),
                ) { Text("Sync") }
                OutlinedButton(
                    onClick = {
                        seedText = ""
                        viewModel.setSquadSeed(null)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Go solo") }
            }
            Text(
                text = if (state.squadSeed == null) "Rolling solo." else "Synced on \"${state.squadSeed}\".",
                style = MaterialTheme.typography.labelMedium,
                color = Ash,
                modifier = Modifier.padding(top = 6.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 18.dp))

            Text("Hero pool", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap a hero to ban them from the roller, tap again to bring them back. " +
                    "If you ban everything the roller can land on, it ignores the bans " +
                    "rather than rolling nothing.",
                style = MaterialTheme.typography.bodySmall,
                color = Ash,
            )

            Role.entries.forEach { role ->
                Spacer(Modifier.height(14.dp))
                Text(
                    text = role.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = role.tint,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Roster.byRole(role).forEach { hero ->
                        FilterChip(
                            selected = hero.name !in state.disabledHeroes,
                            onClick = { viewModel.toggleHero(hero.name) },
                            label = { Text(hero.name) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
