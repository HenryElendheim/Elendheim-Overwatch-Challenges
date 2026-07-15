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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.elendheim.overwatchchallenges.data.Challenge
import com.elendheim.overwatchchallenges.data.Hero
import com.elendheim.overwatchchallenges.data.PoolMode
import com.elendheim.overwatchchallenges.data.Role
import com.elendheim.overwatchchallenges.data.Roster
import com.elendheim.overwatchchallenges.data.RulePack
import com.elendheim.overwatchchallenges.engine.RollResult
import com.elendheim.overwatchchallenges.ui.theme.DamageRed
import com.elendheim.overwatchchallenges.ui.theme.Ember
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

private val muted
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun RollScreen(viewModel: RollViewModel) {
    val state = viewModel.state
    // null = settings closed, otherwise which settings page is open
    var settingsPage by rememberSaveable { mutableStateOf<String?>(null) }
    var landedRollId by rememberSaveable { mutableIntStateOf(0) }
    // saveable so the intro plays once per launch, not again on rotation
    var introDone by rememberSaveable { mutableStateOf(false) }
    // what the result screen is allowed to show; only advances once the reel
    // has landed, so a fresh roll never leaks through the outgoing screen
    var landedResult by remember { mutableStateOf(state.result) }

    BackHandler(enabled = settingsPage != null) {
        settingsPage = if (settingsPage == "root") null else "root"
    }

    val screenKey = when {
        settingsPage != null -> "settings:$settingsPage"
        state.result == null -> "start"
        state.rollId > landedRollId -> "spin"
        else -> "result"
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold { innerPadding ->
            AnimatedContent(
                targetState = screenKey,
                transitionSpec = {
                    if (targetState == "spin") {
                        // cut to the reel fast, no lingering on the old screen
                        fadeIn(tween(180)).togetherWith(fadeOut(tween(110)))
                    } else {
                        (fadeIn(tween(420, delayMillis = 90)) +
                            scaleIn(tween(420, delayMillis = 90), initialScale = 0.94f))
                            .togetherWith(fadeOut(tween(150)))
                    }
                },
                label = "screen",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) { screen ->
                when (screen) {
                    "settings:root" -> SettingsRoot(
                        state = state,
                        onOpen = { settingsPage = it },
                        onDone = { settingsPage = null },
                    )

                    "settings:squad" -> SquadSettings(state, viewModel) { settingsPage = "root" }
                    "settings:challenges" -> ChallengeSettings(state, viewModel) { settingsPage = "root" }
                    "settings:heroes" -> HeroPoolSettings(state, viewModel) { settingsPage = "root" }
                    "settings:access" -> AccessibilitySettings(state, viewModel) { settingsPage = "root" }

                    "start" -> StartScreen(
                        onRoll = viewModel::roll,
                        onSettings = { settingsPage = "root" },
                    )

                    "spin" -> SpinScreen(
                        state = state,
                        onLanded = {
                            landedResult = state.result
                            landedRollId = state.rollId
                        },
                    )

                    else -> (if (state.rollId > landedRollId) landedResult else state.result)
                        ?.let { result ->
                            ResultScreen(
                                state = state,
                                result = result,
                                viewModel = viewModel,
                                onSettings = { settingsPage = "root" },
                            )
                        }
                }
            }
        }

        if (!introDone) {
            IntroSplash(
                reduceMotion = state.reduceMotion,
                onDone = { introDone = true },
            )
        }
    }
}

/**
 * The maker's mark. Plays once per launch, well under two seconds, then gets
 * out of the way. Reduce motion swaps the animation for a short static card.
 */
@Composable
private fun IntroSplash(reduceMotion: Boolean, onDone: () -> Unit) {
    val title = remember { MutableTransitionState(reduceMotion).apply { targetState = true } }
    var subtitleVisible by remember { mutableStateOf(reduceMotion) }
    var fading by remember { mutableStateOf(false) }
    val overlayAlpha by animateFloatAsState(
        targetValue = if (fading) 0f else 1f,
        animationSpec = tween(if (reduceMotion) 1 else 280),
        label = "introFade",
    )

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            delay(600)
            onDone()
            return@LaunchedEffect
        }
        delay(320)
        subtitleVisible = true
        delay(800)
        fading = true
        delay(300)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
            .background(MaterialTheme.colorScheme.background)
            // swallow taps so nothing underneath gets pressed mid-intro
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                visibleState = title,
                enter = fadeIn(tween(340)) + scaleIn(tween(340), initialScale = 1.12f),
            ) {
                Text(
                    text = "ELENDHEIM",
                    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 6.sp),
                    fontWeight = FontWeight.Bold,
                    color = Ember,
                )
            }
            Spacer(Modifier.height(6.dp))
            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
            ) {
                Text(
                    text = "Overwatch Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
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
                color = muted,
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
    // spin length is user-tuned; now and then the runout drags on for suspense
    val spinMillis = remember(state.rollId) {
        val random = Random(state.rollId * 31L + 5)
        val base = (state.spinSeconds * 1000).toInt()
        val suspense = if (random.nextInt(3) == 0) base / 2 + random.nextInt(600) else 0
        (base + suspense).coerceIn(600, 8000)
    }
    // the target sits two entries before the end so the overshoot has
    // something to scroll past
    val targetIndex = reel.size - 3
    val itemPx = with(LocalDensity.current) { 96.dp.toPx() }
    val offset = remember(state.rollId) { Animatable(0f) }

    LaunchedEffect(state.rollId) {
        if (state.reduceMotion) {
            offset.snapTo(targetIndex * itemPx)
            delay(200)
            onLanded()
            return@LaunchedEffect
        }
        offset.animateTo(
            targetValue = targetIndex * itemPx,
            animationSpec = tween(
                durationMillis = spinMillis,
                easing = CubicBezierEasing(0.16f, 0.84f, 0.28f, 1.04f),
            ),
        )
        delay(340)
        onLanded()
    }

    // a wildcard roll never shows the hero; the reel lands on ??? instead
    val mystery = result.challenges.any { it.overridesHero }

    Box(Modifier.fillMaxSize()) {
        reel.forEachIndexed { index, hero ->
            val isTarget = index == targetIndex
            Text(
                text = if (isTarget && mystery) "???" else hero.name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (isTarget && mystery) Ember else hero.role.tint,
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
        OutlinedButton(
            onClick = viewModel::died,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("I died")
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = viewModel::mutate,
                enabled = result.challenges.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
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
            text = "Mutations: ${state.mutations} · Deaths: ${state.deaths}",
            style = MaterialTheme.typography.labelMedium,
            color = muted,
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
                color = muted,
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
            color = muted,
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
            val mystery = result.challenges.any { it.overridesHero }
            Text(
                text = if (mystery) "WILDCARD" else result.hero.role.label.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = if (mystery) Ember else result.hero.role.tint,
            )
            Text(
                text = if (mystery) "???" else result.hero.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            if (mystery) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No hero this time. The constraint below decides who you play.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
            }

            Spacer(Modifier.height(18.dp))
            if (result.challenges.isEmpty() && result.pendingChallenge != null) {
                Text(
                    text = "No constraint this time. Escalate deals the one you were owed, " +
                        "and that one's locked in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
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
            text = "${challenge.packName ?: challenge.category.label} · " +
                (challenge.customTag ?: challenge.intensity.label),
            style = MaterialTheme.typography.labelMedium,
            color = muted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = challenge.text,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

// settings

@Composable
private fun SettingsHeader(title: String, backLabel: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onBack) { Text(backLabel) }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = muted)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = muted)
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
}

@Composable
private fun SettingsRoot(state: RollUiState, onOpen: (String) -> Unit, onDone: () -> Unit) {
    val activeHeroes = Roster.heroes.count { it.name !in state.disabledHeroes }
    val packsOn = state.rulePacks.count { it.enabled }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Settings", "Done", onDone)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsRow(
                title = "Squad sync",
                subtitle = if (state.squadSeed == null) "Rolling solo" else "Synced on \"${state.squadSeed}\"",
                onClick = { onOpen("squad") },
            )
            SettingsRow(
                title = "Challenges",
                subtitle = buildString {
                    append(if (state.standardEnabled) "Standard pool on" else "Standard pool off")
                    if (state.rulePacks.isNotEmpty()) {
                        append(" · $packsOn of ${state.rulePacks.size} packs on")
                    }
                },
                onClick = { onOpen("challenges") },
            )
            SettingsRow(
                title = "Hero pool",
                subtitle = if (state.showPoolCounts) {
                    "$activeHeroes/${Roster.heroes.size} heroes active"
                } else {
                    "Bans and role switches"
                },
                onClick = { onOpen("heroes") },
            )
            SettingsRow(
                title = "Accessibility",
                subtitle = "Text size, contrast and motion",
                onClick = { onOpen("access") },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SquadSettings(state: RollUiState, viewModel: RollViewModel, onBack: () -> Unit) {
    var seedText by remember { mutableStateOf(state.squadSeed.orEmpty()) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Squad sync", "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Everyone who enters the same word gets the same rolls, as long as " +
                    "filters, hero bans and packs match and you tap in the same order. " +
                    "Setting a word starts a fresh sequence.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
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
                color = muted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ChallengeSettings(state: RollUiState, viewModel: RollViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Challenges", "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = "Standard challenges",
                description = "The built-in pool. Turn it off to run purely on your own packs.",
                checked = state.standardEnabled,
                onToggle = viewModel::toggleStandard,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "??? rolls",
                description = "About one roll in fifty comes up as a wildcard: no hero, just a " +
                    "constraint that decides who you play. Needs the standard pool.",
                checked = state.mysteryEnabled,
                onToggle = viewModel::toggleMystery,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "No Challenge Mode",
                description = "Rolls hand you a hero and nothing else. When you're ready, " +
                    "escalate deals the constraint the roll was holding back, and that one " +
                    "can't be swiped away.",
                checked = state.noChallengeMode,
                onToggle = viewModel::toggleNoChallengeMode,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Rule packs", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Bundle your own constraints into packs and flip them on per session. " +
                    "Tag rules Warmup or Chaos to join those pools, or use any tag you like - " +
                    "custom tags only roll in Mixed.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )

            state.rulePacks.forEach { pack ->
                Spacer(Modifier.height(10.dp))
                PackCard(pack, viewModel)
            }

            Spacer(Modifier.height(14.dp))
            var newPackName by remember { mutableStateOf("") }
            OutlinedTextField(
                value = newPackName,
                onValueChange = { newPackName = it },
                label = { Text("New pack name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.createPack(newPackName)
                    newPackName = ""
                },
                enabled = newPackName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create pack") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PackCard(pack: RulePack, viewModel: RollViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = pack.enabled, onCheckedChange = { viewModel.togglePack(pack.name) })
            }

            pack.rules.forEach { rule ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(rule.text, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.tag, style = MaterialTheme.typography.labelSmall, color = muted)
                    }
                    TextButton(onClick = { viewModel.removeRule(pack.name, rule) }) {
                        Text("Remove")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            var ruleText by remember(pack.name) { mutableStateOf("") }
            var ruleTag by remember(pack.name) { mutableStateOf("") }
            OutlinedTextField(
                value = ruleText,
                onValueChange = { ruleText = it },
                label = { Text("New constraint") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = ruleTag,
                onValueChange = { ruleTag = it },
                label = { Text("Tag: Warmup, Chaos or your own") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.addRule(pack.name, ruleText, ruleTag)
                        ruleText = ""
                        ruleTag = ""
                    },
                    enabled = ruleText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Add rule") }
                TextButton(onClick = { viewModel.deletePack(pack.name) }) {
                    Text("Delete pack")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroPoolSettings(state: RollUiState, viewModel: RollViewModel, onBack: () -> Unit) {
    val counts = state.showPoolCounts
    val activeTotal = Roster.heroes.count { it.name !in state.disabledHeroes }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Hero pool", "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = "Show counts",
                description = "Show how many heroes are active, overall and per role.",
                checked = counts,
                onToggle = viewModel::toggleShowPoolCounts,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (counts) "All heroes · $activeTotal/${Roster.heroes.size}" else "All heroes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = Roster.heroes.none { it.name in state.disabledHeroes },
                    onCheckedChange = { on ->
                        viewModel.setGroupEnabled(Roster.heroes.map { it.name }, on)
                    },
                )
            }
            Text(
                text = "Tap a hero to ban them from the roller, tap again to bring them back. " +
                    "The switches flip a whole role, or everything, at once. If you ban " +
                    "everything the roller can land on, it ignores the bans rather than " +
                    "rolling nothing.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )

            Role.entries.forEach { role ->
                val roleHeroes = Roster.byRole(role)
                val activeInRole = roleHeroes.count { it.name !in state.disabledHeroes }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = role.label.uppercase() +
                            if (counts) " · $activeInRole/${roleHeroes.size}" else "",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                        color = role.tint,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = roleHeroes.none { it.name in state.disabledHeroes },
                        onCheckedChange = { on ->
                            viewModel.setGroupEnabled(roleHeroes.map { it.name }, on)
                        },
                    )
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roleHeroes.forEach { hero ->
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

@Composable
private fun AccessibilitySettings(state: RollUiState, viewModel: RollViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Accessibility", "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "TEXT SIZE",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = muted,
            )
            Spacer(Modifier.height(6.dp))
            TextSize.entries.forEach { size ->
                val selected = state.textSize == size
                Card(
                    onClick = { viewModel.setTextSize(size) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = if (selected) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = size.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "High contrast",
                description = "Brighter text and stronger lines against the dark background.",
                checked = state.highContrast,
                onToggle = viewModel::toggleHighContrast,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "Reduce motion",
                description = "Skips the opening animation and the roll spin.",
                checked = state.reduceMotion,
                onToggle = viewModel::toggleReduceMotion,
            )

            Spacer(Modifier.height(16.dp))
            var sliderValue by remember { mutableFloatStateOf(state.spinSeconds) }
            Text("Spin length", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "About %.1f seconds before it lands.".format(sliderValue),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { viewModel.setSpinSeconds(sliderValue) },
                valueRange = 1f..5f,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
