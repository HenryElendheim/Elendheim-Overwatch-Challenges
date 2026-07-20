package com.elendheim.overwatchchallenges.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.overwatchchallenges.data.Challenge
import com.elendheim.overwatchchallenges.data.ChallengePool
import com.elendheim.overwatchchallenges.data.CustomRule
import com.elendheim.overwatchchallenges.data.Hero
import com.elendheim.overwatchchallenges.data.Intensity
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
import kotlinx.coroutines.launch

private val Role.tint
    get() = when (this) {
        Role.TANK -> TankBlue
        Role.DAMAGE -> DamageRed
        Role.SUPPORT -> SupportGreen
    }

/** The role's color, with any accessibility override applied. */
private fun RollUiState.tintFor(role: Role): Color =
    if (!customRoleColors) role.tint
    else roleColors[role]?.let { Color(it) } ?: role.tint

// distinct enough to cover most colorblindness combinations
private val SwatchPalette = listOf(
    Color(0xFF5FA8FF), Color(0xFF4DD0E1), Color(0xFF26A69A), Color(0xFF5FD98F),
    Color(0xFFD4E157), Color(0xFFFFD54F), Color(0xFFFF9E2C), Color(0xFFFF7A66),
    Color(0xFFEF5350), Color(0xFFF48FB1), Color(0xFFB388FF), Color(0xFFFFFFFF),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchRow(
    selected: Int?,
    onPick: (Int?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SwatchPalette.forEach { color ->
            val argb = color.toArgb()
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected == argb) 3.dp else 1.dp,
                        color = if (selected == argb) Ember else Color(0x33FFFFFF),
                        shape = CircleShape,
                    )
                    .clickable { onPick(argb) },
            )
        }
    }
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
        settingsPage = when {
            settingsPage?.startsWith("pack:") == true -> "challenges"
            settingsPage == "standardpack" -> "challenges"
            settingsPage == "root" -> null
            else -> "root"
        }
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
                when {
                    screen == "settings:root" -> SettingsRoot(
                        state = state,
                        onOpen = { settingsPage = it },
                        onDone = { settingsPage = null },
                    )

                    screen.startsWith("settings:pack:") -> PackDetailSettings(
                        state = state,
                        viewModel = viewModel,
                        packName = screen.removePrefix("settings:pack:"),
                        onBack = { settingsPage = "challenges" },
                    )

                    screen == "settings:standardpack" -> StandardPackSettings(
                        state = state,
                        viewModel = viewModel,
                        onBack = { settingsPage = "challenges" },
                    )

                    screen == "settings:squad" -> SquadSettings(state, viewModel) { settingsPage = "root" }
                    screen == "settings:challenges" -> ChallengeSettings(
                        state = state,
                        viewModel = viewModel,
                        onOpenPack = { settingsPage = "pack:$it" },
                        onOpenStandard = { settingsPage = "standardpack" },
                        onBack = { settingsPage = "root" },
                    )
                    screen == "settings:heroes" -> HeroPoolSettings(state, viewModel) { settingsPage = "root" }
                    screen == "settings:spinstyle" -> SpinSettings(state, viewModel) { settingsPage = "root" }
                    screen == "settings:access" -> AccessibilitySettings(state, viewModel) { settingsPage = "root" }

                    screen == "start" -> StartScreen(
                        onRoll = viewModel::roll,
                        onSettings = { settingsPage = "root" },
                    )

                    screen == "spin" -> SpinScreen(
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
        val suspense = if (state.suspenseEnabled && random.nextInt(3) == 0) {
            base / 2 + random.nextInt(600)
        } else {
            0
        }
        (base + suspense).coerceIn(600, 8000)
    }
    // the target sits two entries before the end so the overshoot has
    // something to scroll past
    val targetIndex = reel.size - 3
    val itemPx = with(LocalDensity.current) { state.reelDensity.itemHeight.dp.toPx() }
    val offset = remember(state.rollId) { Animatable(0f) }
    // the landing pop: the winner bounces up while the rest clear the stage
    val pop = remember(state.rollId) { Animatable(1f) }
    val othersAlpha = remember(state.rollId) { Animatable(1f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.rollId) {
        if (state.reduceMotion) {
            offset.snapTo(targetIndex * itemPx)
            if (state.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(200)
            onLanded()
            return@LaunchedEffect
        }
        offset.animateTo(
            targetValue = targetIndex * itemPx,
            animationSpec = tween(
                durationMillis = spinMillis,
                easing = if (state.overshootEnabled) {
                    CubicBezierEasing(0.16f, 0.84f, 0.28f, 1.04f)
                } else {
                    CubicBezierEasing(0.16f, 0.84f, 0.3f, 1f)
                },
            ),
        )
        if (state.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { othersAlpha.animateTo(0f, tween(240)) }
        pop.animateTo(
            targetValue = 1.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
        delay(220)
        onLanded()
    }

    // a wildcard roll never shows the hero; the reel lands on ??? instead
    val mystery = result.challenges.any { it.overridesHero }
    val plainColor = when (state.reelColor) {
        ReelColor.ROLE -> null
        ReelColor.EMBER -> Ember
        ReelColor.WHITE -> MaterialTheme.colorScheme.onBackground
    }

    Box(Modifier.fillMaxSize()) {
        reel.forEachIndexed { index, hero ->
            val isTarget = index == targetIndex
            Text(
                text = if (isTarget && mystery) "???" else hero.name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (isTarget && mystery) Ember else plainColor ?: state.tintFor(hero.role),
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val dy = (index - offset.value / itemPx) * itemPx
                        translationY = dy
                        val distance = abs(dy) / (itemPx * 3f)
                        alpha = (1f - distance * distance).coerceIn(0f, 1f) *
                            (if (isTarget) 1f else othersAlpha.value)
                        val scale = (1f - 0.3f * (abs(dy) / (itemPx * 2f)).coerceAtMost(1f)) *
                            (if (isTarget) pop.value else 1f)
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }

        // the landing line markers sit still while the reel flies past them
        if (state.landingArrow) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(state.arrowSize.sizeDp.dp),
            ) {
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height / 2f)
                    close()
                }
                drawPath(path, state.arrowColor?.let { Color(it) } ?: Color.White)
            }
        }
        if (state.landingUnderline) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 30.dp)
                    .size(width = 190.dp, height = 3.dp)
                    .background(Color.White, RoundedCornerShape(2.dp)),
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
        val haptic = LocalHapticFeedback.current
        OutlinedButton(
            onClick = {
                if (state.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.died()
            },
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
                color = if (mystery) Ember else state.tintFor(result.hero.role),
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
                subtitle = "Experimental · " +
                    if (state.squadSeed == null) "Rolling solo" else "Synced on \"${state.squadSeed}\"",
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
                title = "Spin",
                subtitle = "Length, suspense, reel style and colors",
                onClick = { onOpen("spinstyle") },
            )
            SettingsRow(
                title = "Accessibility",
                subtitle = "Text size, contrast and motion",
                onClick = { onOpen("access") },
            )

            Spacer(Modifier.height(24.dp))
            val context = LocalContext.current
            val versionName = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull()
            }
            Text(
                text = "Elendheim Overwatch Challenges" +
                    (versionName?.let { " v$it" } ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
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
                .imePadding()
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "EXPERIMENTAL",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = Ember,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "This hasn't been battle-tested with a real squad yet. If rolls drift " +
                    "apart between phones, reroll from a fresh word.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            Spacer(Modifier.height(10.dp))
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
private fun ChallengeSettings(
    state: RollUiState,
    viewModel: RollViewModel,
    onOpenPack: (String) -> Unit,
    onOpenStandard: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Challenges", "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(Modifier.height(8.dp))
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
                    "custom tags only roll in Mixed. Every pack has a code that can bring " +
                    "it back through import.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )

            Spacer(Modifier.height(10.dp))
            Card(
                onClick = onOpenStandard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Standard challenges", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${ChallengePool.all.size} rules built in",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                        )
                    }
                    Switch(
                        checked = state.standardEnabled,
                        onCheckedChange = { viewModel.toggleStandard() },
                    )
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.headlineSmall,
                        color = muted,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            state.rulePacks.forEach { pack ->
                Spacer(Modifier.height(10.dp))
                Card(
                    onClick = { onOpenPack(pack.name) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pack.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = pack.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Ember,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            Text(
                                text = "${pack.rules.size} rules",
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                            )
                        }
                        Switch(
                            checked = pack.enabled,
                            onCheckedChange = { viewModel.togglePack(pack.name) },
                        )
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.headlineSmall,
                            color = muted,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
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

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Import a pack", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Type a short pack code (like XQCW42), paste a shared code, or pick " +
                    "a saved JSON or text file. Short codes bring back any pack this phone " +
                    "has ever had, even deleted ones.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            Spacer(Modifier.height(8.dp))
            var importCode by remember { mutableStateOf("") }
            var importFailed by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val openPackFile = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    val content = runCatching {
                        context.contentResolver.openInputStream(uri)
                            ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }.getOrNull()
                    importFailed = content == null || !viewModel.importPack(content)
                }
            }
            OutlinedTextField(
                value = importCode,
                onValueChange = {
                    importCode = it
                    importFailed = false
                },
                label = { Text("Pack code") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        importFailed = !viewModel.importPack(importCode)
                        if (!importFailed) importCode = ""
                    },
                    enabled = importCode.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Import code") }
                OutlinedButton(
                    onClick = {
                        openPackFile.launch(
                            arrayOf("application/json", "text/*", "application/octet-stream")
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("From file") }
            }
            if (importFailed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Couldn't read that. Try a code or a saved pack file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/**
 * One pack, full page: search it, edit rules in place, multi-select for
 * mass deletion, and share the whole thing as a code.
 */
@Composable
private fun PackDetailSettings(
    state: RollUiState,
    viewModel: RollViewModel,
    packName: String,
    onBack: () -> Unit,
) {
    val pack = state.rulePacks.firstOrNull { it.name == packName }
    if (pack == null) {
        // pack got deleted under us; bail back to the list
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current
    val allTags = remember(state.rulePacks) {
        (listOf(Intensity.WARMUP.label, Intensity.CHAOS.label) +
            state.rulePacks.flatMap { p -> p.rules.map { it.tag } })
            .distinctBy { it.lowercase() }
    }

    var query by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var editingRule by remember { mutableStateOf<String?>(null) }

    val distinctTags = pack.rules.map { it.tag }.distinctBy { it.lowercase() }
    val searchable = pack.rules.size > 3 || distinctTags.size > 3
    val visibleRules = pack.rules.filter {
        query.isBlank() || it.text.contains(query, true) || it.tag.contains(query, true)
    }

    var confirmDeletePack by remember { mutableStateOf(false) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader(pack.name, "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text(
                text = "Code: ${pack.code}",
                style = MaterialTheme.typography.labelLarge,
                color = Ember,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Enabled",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = pack.enabled, onCheckedChange = { viewModel.togglePack(pack.name) })
            }

            Spacer(Modifier.height(8.dp))
            val saveJson = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    viewModel.exportPackJson(pack.name)?.let { content ->
                        context.contentResolver.openOutputStream(uri)
                            ?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                    }
                }
            }
            val saveMarkdown = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/markdown")
            ) { uri ->
                if (uri != null) {
                    viewModel.exportPackMarkdown(pack.name)?.let { content ->
                        context.contentResolver.openOutputStream(uri)
                            ?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportPack(pack.name)?.let { code ->
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, code)
                            }
                            context.startActivity(Intent.createChooser(send, "Share pack"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Share code") }
                OutlinedButton(
                    onClick = {
                        selecting = !selecting
                        selected = emptySet()
                        editingRule = null
                    },
                    enabled = pack.rules.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text(if (selecting) "Cancel" else "Select") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { saveJson.launch("${pack.name}.json") },
                    modifier = Modifier.weight(1f),
                ) { Text("Save as JSON") }
                OutlinedButton(
                    onClick = { saveMarkdown.launch("${pack.name}.md") },
                    modifier = Modifier.weight(1f),
                ) { Text("Save as text") }
            }

            if (selecting) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { confirmDeleteSelected = true },
                    enabled = selected.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete ${selected.size} selected") }
            }

            if (searchable) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search rules and tags") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(6.dp))
            visibleRules.forEach { rule ->
                key(rule.text) {
                    if (editingRule == rule.text) {
                        Spacer(Modifier.height(6.dp))
                        RuleEditor(
                            rule = rule,
                            allTags = allTags,
                            onSave = { text, tag ->
                                viewModel.updateRule(pack.name, rule, text, tag)
                                editingRule = null
                            },
                            onDelete = {
                                viewModel.removeRules(pack.name, listOf(rule))
                                editingRule = null
                            },
                            onCancel = { editingRule = null },
                        )
                        Spacer(Modifier.height(6.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = selecting) {
                                    selected = if (rule.text in selected) {
                                        selected - rule.text
                                    } else {
                                        selected + rule.text
                                    }
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(rule.text, style = MaterialTheme.typography.bodyMedium)
                                Text(rule.tag, style = MaterialTheme.typography.labelSmall, color = muted)
                            }
                            if (selecting) {
                                Text(
                                    text = if (rule.text in selected) "Selected" else "Tap to select",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (rule.text in selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        muted
                                    },
                                )
                            } else {
                                TextButton(onClick = { editingRule = rule.text }) { Text("Edit") }
                            }
                        }
                    }
                }
            }
            if (pack.rules.isNotEmpty() && visibleRules.isEmpty()) {
                Text(
                    text = "Nothing matches \"$query\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            Text("Add a rule", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            var ruleText by remember { mutableStateOf("") }
            var ruleTag by remember { mutableStateOf("") }
            var showTagPicker by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = ruleText,
                onValueChange = { ruleText = it },
                label = { Text("New constraint") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ruleTag,
                    onValueChange = { ruleTag = it },
                    label = { Text("Tag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { showTagPicker = !showTagPicker },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(if (showTagPicker) "×" else "+") }
            }
            if (showTagPicker) {
                Spacer(Modifier.height(6.dp))
                TagPicker(allTags = allTags) { tag ->
                    ruleTag = tag
                    showTagPicker = false
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addRule(pack.name, ruleText, ruleTag)
                    ruleText = ""
                    ruleTag = ""
                },
                enabled = ruleText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add rule") }

            Spacer(Modifier.height(14.dp))
            TextButton(
                onClick = { confirmDeletePack = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Delete pack", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDeleteSelected) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text("Delete ${selected.size} ${if (selected.size == 1) "rule" else "rules"}?") },
            text = { Text("They come off the pack right away.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeRules(pack.name, pack.rules.filter { it.text in selected })
                        selecting = false
                        selected = emptySet()
                        confirmDeleteSelected = false
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSelected = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmDeletePack) {
        AlertDialog(
            onDismissRequest = { confirmDeletePack = false },
            title = { Text("Delete \"${pack.name}\"?") },
            text = {
                Text(
                    "The pack leaves your list, but its code ${pack.code} stays reserved. " +
                        "Typing it under Import brings the pack back."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeletePack = false
                        viewModel.deletePack(pack.name)
                        onBack()
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletePack = false }) { Text("Cancel") }
            },
        )
    }
}

/** The built-in pool, browsable like any other pack. Read-only by design. */
@Composable
private fun StandardPackSettings(
    state: RollUiState,
    viewModel: RollViewModel,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visible = ChallengePool.all.filter {
        query.isBlank() || it.text.contains(query, true) ||
            it.category.label.contains(query, true) || it.intensity.label.contains(query, true)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Standard challenges", "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Enabled",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.standardEnabled,
                    onCheckedChange = { viewModel.toggleStandard() },
                )
            }
            Text(
                text = "The built-in pool, ${ChallengePool.all.size} rules. It can't be " +
                    "edited, only benched, so your packs can run the show.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search rules") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))
            visible.forEach { challenge ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(challenge.text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${challenge.category.label} · ${challenge.intensity.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                    )
                }
            }
            if (visible.isEmpty()) {
                Text(
                    text = "Nothing matches \"$query\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RuleEditor(
    rule: CustomRule,
    allTags: List<String>,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(rule.text) }
    var tag by remember { mutableStateOf(rule.tag) }
    var showTagPicker by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Constraint") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { showTagPicker = !showTagPicker },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(if (showTagPicker) "×" else "+") }
            }
            if (showTagPicker) {
                Spacer(Modifier.height(6.dp))
                TagPicker(allTags = allTags) { picked ->
                    tag = picked
                    showTagPicker = false
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSave(text, tag) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

/** Existing tags, tappable. More than three and you get a search bar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPicker(allTags: List<String>, onPick: (String) -> Unit) {
    var tagQuery by remember { mutableStateOf("") }
    Column {
        if (allTags.size > 3) {
            OutlinedTextField(
                value = tagQuery,
                onValueChange = { tagQuery = it },
                label = { Text("Search tags") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            allTags
                .filter { tagQuery.isBlank() || it.contains(tagQuery, true) }
                .forEach { tag ->
                    FilterChip(
                        selected = false,
                        onClick = { onPick(tag) },
                        label = { Text(tag) },
                    )
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
                        color = state.tintFor(role),
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

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "Haptics",
                description = "A thump when the reel lands, and on the I died button.",
                checked = state.hapticsEnabled,
                onToggle = viewModel::toggleHaptics,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ROLE COLORS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = muted,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.customRoleColors,
                    onCheckedChange = { viewModel.toggleCustomRoleColors() },
                )
            }
            Text(
                text = "Pick your own colors for Tank, Damage and Support text, wherever " +
                    "they appear. Handy for colorblindness or just taste. Switch off to " +
                    "go back to the originals - your picks are kept.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            if (state.customRoleColors) {
                Role.entries.forEach { role ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = role.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = state.tintFor(role),
                    )
                    Spacer(Modifier.height(6.dp))
                    ColorSwatchRow(
                        selected = state.roleColors[role],
                        onPick = { viewModel.setRoleColor(role, it) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SpinSettings(state: RollUiState, viewModel: RollViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        SettingsHeader("Spin", "Back", onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
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

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "Suspense spins",
                description = "Roughly one spin in three runs longer before it lands.",
                checked = state.suspenseEnabled,
                onToggle = viewModel::toggleSuspense,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "Overshoot",
                description = "The reel scrolls slightly past the winner and settles back, " +
                    "slot-machine style.",
                checked = state.overshootEnabled,
                onToggle = viewModel::toggleOvershoot,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "Landing arrow",
                description = "A white arrow at the landing line so you can see exactly " +
                    "where the reel will stop.",
                checked = state.landingArrow,
                onToggle = viewModel::toggleLandingArrow,
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ArrowSize.entries.forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = state.arrowSize == size,
                        onClick = { viewModel.setArrowSize(size) },
                        enabled = state.landingArrow,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ArrowSize.entries.size,
                        ),
                    ) { Text(size.label) }
                }
            }
            if (state.landingArrow) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Arrow color",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                ColorSwatchRow(
                    selected = state.arrowColor,
                    onPick = viewModel::setArrowColor,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            SwitchRow(
                title = "Underline the landing name",
                description = "A thin line under whichever name is crossing the landing " +
                    "line right now.",
                checked = state.landingUnderline,
                onToggle = viewModel::toggleLandingUnderline,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(
                text = "REEL DENSITY",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = muted,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ReelDensity.entries.forEachIndexed { index, density ->
                    SegmentedButton(
                        selected = state.reelDensity == density,
                        onClick = { viewModel.setReelDensity(density) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ReelDensity.entries.size,
                        ),
                    ) { Text(density.label) }
                }
            }
            Text(
                text = "How many names crowd the screen while it spins.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "NAME COLORS",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = muted,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ReelColor.entries.forEachIndexed { index, color ->
                    SegmentedButton(
                        selected = state.reelColor == color,
                        onClick = { viewModel.setReelColor(color) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ReelColor.entries.size,
                        ),
                    ) { Text(color.label) }
                }
            }
            Text(
                text = "Role colors flicker as the reel flies; the flat options keep it calm.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
