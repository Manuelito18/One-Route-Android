package com.onerouteandroid.oneroute.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onerouteandroid.oneroute.R
import com.onerouteandroid.oneroute.ui.icons.OneRouteIcons
import com.onerouteandroid.oneroute.ui.theme.OneRouteAndroidTheme
import com.onerouteandroid.oneroute.ui.theme.OneRouteElevation
import com.onerouteandroid.oneroute.ui.theme.OneRouteRadius
import com.onerouteandroid.oneroute.ui.theme.OneRouteSpacing
import com.onerouteandroid.oneroute.ui.theme.OneRouteType
import java.util.Locale
import kotlinx.coroutines.delay

/*
 * =============================================================================
 *  Two-factor verification — "Verifica tu identidad"
 * =============================================================================
 *  Compose port of `specs/ui/verificar_otp.html`, built exclusively on the
 *  tokens declared in `specs/ui/DESIGN.md`.
 *
 *  Scope: presentation and local interaction only. The resend countdown, the
 *  PIN cells and the "Verificar código" feedback loop are driven entirely in
 *  memory — no SMS, no network, no persistence.
 *
 *  Layering:
 *   • [VerifyOtpUiState]       – one immutable snapshot of everything needed to
 *                                draw a frame (the digits, the countdown, the
 *                                flags behind the CTA and both resend affordances).
 *   • [VerifyOtpScreenContent] – stateless and fully hoisted: it takes that
 *                                state plus one callback per user intent, so it
 *                                renders from a @Preview, a test or any other
 *                                destination without extra wiring.
 *   • [VerifyOtpScreen]        – stateful container that owns the in-memory
 *                                state, ticks the countdown and runs the local
 *                                verification micro-interaction.
 * =============================================================================
 */

// ---------------------------------------------------------------------------
// State model
// ---------------------------------------------------------------------------

/** Code length shipped by the mock: six digits. */
private const val DefaultCodeLength = 6

/** Seconds the countdown runs before the code may be resent. */
private const val DefaultResendSeconds = 60

/** Destination the mock ships with, so the copy reads well before wiring. */
private const val DefaultDestination = "sofia.morales@ejemplo.com"

/** How long the local "Verificando…" feedback stays on screen. */
private const val VerifyLoadingMillis = 900L

/** One tick of the resend countdown. */
private const val CountdownTickMillis = 1_000L

/**
 * Everything [VerifyOtpScreenContent] needs in order to render one frame.
 *
 * Free of `Context`, callbacks and coroutine scopes by design: a preview or a
 * unit test can build one from a literal and assert on it directly.
 *
 * [isCodeComplete] is only ever written where the digits are normalised, so the
 * flag can never drift from the code it describes.
 */
@Immutable
data class VerifyOtpUiState(
    // --- the code ---
    val code: String = "",
    val codeLength: Int = DefaultCodeLength,
    val isCodeComplete: Boolean = false,

    // --- primary action ---
    val isLoading: Boolean = false,

    // --- resend control ---
    val timerSeconds: Int = DefaultResendSeconds,
    val canResend: Boolean = false,

    // --- where the code was sent, shown by the descriptive copy ---
    val destination: String = DefaultDestination,
) {
    /** Index of the cell waiting for the next digit, or `-1` when complete. */
    val activeCellIndex: Int
        get() = if (isCodeComplete) -1 else code.length.coerceAtMost(codeLength)

    /** The digit rendered by cell [index], or `null` while the cell is empty. */
    fun digitAt(index: Int): Char? = code.getOrNull(index)
}

/**
 * Returns a copy of the state with [raw] normalised to digits only and capped
 * at [VerifyOtpUiState.codeLength], keeping [VerifyOtpUiState.isCodeComplete]
 * in sync with the result.
 */
private fun VerifyOtpUiState.withCode(raw: String): VerifyOtpUiState {
    val digits = raw.filter(Char::isDigit).take(codeLength)
    return copy(code = digits, isCodeComplete = digits.length == codeLength)
}

/** `mm:ss` countdown label, matching the mock's `00:45` clock. */
private fun countdownText(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60)
}

// ---------------------------------------------------------------------------
// Container
// ---------------------------------------------------------------------------

/**
 * Stateful entry point for the verification screen.
 *
 * Owns the in-memory [VerifyOtpUiState] so the screen is interactive the moment
 * it is shown: the numeric keyboard opens, the cells fill in as digits are
 * typed, the countdown reaches zero and enables both resend affordances, and
 * the primary action plays its local "Verificando…" feedback loop.
 *
 * @param destination e-mail or phone number the code was sent to.
 * @param onBackClick invoked when the user taps the top app bar back button.
 * @param onEditEmailClick invoked when the user taps "Cambiar correo".
 * @param onAccountClick invoked when the user taps the account avatar.
 * @param onMoreOptionsClick invoked when the user taps the overflow menu.
 */
@Composable
fun VerifyOtpScreen(
    modifier: Modifier = Modifier,
    destination: String = DefaultDestination,
    onBackClick: () -> Unit = {},
    onEditEmailClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
) {
    var state by remember(destination) { mutableStateOf(VerifyOtpUiState(destination = destination)) }

    // One tick per second. The effect is keyed on the counter itself, so it
    // restarts on every tick and picks the countdown up again after a resend.
    LaunchedEffect(state.timerSeconds) {
        if (state.timerSeconds <= 0) return@LaunchedEffect
        delay(CountdownTickMillis)
        val remaining = state.timerSeconds - 1
        state = state.copy(timerSeconds = remaining, canResend = remaining == 0)
    }

    // Mirrors the mock's submit micro-interaction: a short local spinner, then
    // back to idle. Wiring the real navigation is a one-liner once a NavHost
    // owns this screen.
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            delay(VerifyLoadingMillis)
            state = state.copy(isLoading = false)
        }
    }

    VerifyOtpScreenContent(
        state = state,
        modifier = modifier,
        onCodeChange = { raw -> state = state.withCode(raw) },
        onVerifyClick = {
            if (state.isCodeComplete && !state.isLoading) {
                state = state.copy(isLoading = true)
            }
        },
        onResendClick = {
            if (state.canResend) {
                state = VerifyOtpUiState(destination = state.destination)
            }
        },
        onBackClick = onBackClick,
        onEditEmailClick = onEditEmailClick,
        onAccountClick = onAccountClick,
        onMoreOptionsClick = onMoreOptionsClick,
    )
}

// ---------------------------------------------------------------------------
// Stateless content
// ---------------------------------------------------------------------------

/**
 * Stateless, fully hoisted verification screen.
 *
 * Renders [state] and reports every user intent through its callbacks. It owns
 * no state beyond transient interaction feedback (the focused field, the
 * blinking caret and press scaling), so a [VerifyOtpUiState] literal is all a
 * preview or a Compose test needs.
 */
@Composable
fun VerifyOtpScreenContent(
    state: VerifyOtpUiState,
    modifier: Modifier = Modifier,
    onCodeChange: (String) -> Unit = {},
    onVerifyClick: () -> Unit = {},
    onResendClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onEditEmailClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            VerifyOtpTopBar(
                title = stringResource(R.string.verify_otp_top_bar_title),
                onBackClick = onBackClick,
                onAccountClick = onAccountClick,
                onMoreOptionsClick = onMoreOptionsClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .navigationBarsPadding()
                // Keeps the CTA reachable while the numeric keypad is up.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OneRouteSpacing.Margin)
                // The mock's `pb-12` gesture strip plus `pb-space-xl`.
                .padding(bottom = 48.dp + OneRouteSpacing.SpaceXl),
        ) {
            VerificationBanner(
                destination = state.destination,
                codeLength = state.codeLength,
                onEditEmailClick = onEditEmailClick,
                modifier = Modifier.padding(top = OneRouteSpacing.SpaceSm), // mt-space-sm
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg)) // mb-space-lg

            OtpCard(
                state = state,
                onCodeChange = onCodeChange,
                onResendClick = onResendClick,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg)) // mb-space-lg

            ResendPrompt(
                canResend = state.canResend,
                onResendClick = onResendClick,
            )

            // The mock spaces the microcopy from the action row by `mt-space-md`.
            Spacer(Modifier.height(OneRouteSpacing.SpaceMd))

            VerifyButton(
                enabled = state.isCodeComplete && !state.isLoading,
                isLoading = state.isLoading,
                onClick = onVerifyClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top app bar
// ---------------------------------------------------------------------------

@Composable
private fun VerifyOtpTopBar(
    title: String,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        // `bg-surface/85 backdrop-blur-xl`. Compose has no stable backdrop
        // blur, so the translucent plane plus a hairline shadow stand in for it.
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = OneRouteElevation.Resting,
    ) {
        Column {
            // The 24dp native status-bar strip; the OS clock and system icons
            // are drawn by the platform, not by the app.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp) // h-16
                    .padding(horizontal = OneRouteSpacing.SpaceXs) // px-space-xs
                    // `pr-space-xs` on the trailing action group.
                    .padding(end = OneRouteSpacing.SpaceXs)
                    .consumeWindowInsets(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp), // w-11 h-11
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = OneRouteIcons.ArrowBack,
                        contentDescription = stringResource(R.string.verify_otp_navigate_back),
                        modifier = Modifier.size(24.dp), // text-[24px]
                    )
                }

                Spacer(Modifier.width(OneRouteSpacing.SpaceXs)) // gap-space-xs

                BrandMark(contentDescription = stringResource(R.string.verify_otp_brand_logo))

                Spacer(Modifier.width(OneRouteSpacing.SpaceSm)) // gap-space-sm

                // `max-w-[200px] truncate`: the mock hard-caps the title so the
                // trailing controls keep their slot. The weighted slot does the
                // same job while staying safe on 360dp-wide screens.
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                AccountButton(onAccountClick = onAccountClick)

                Spacer(Modifier.width(OneRouteSpacing.SpaceXs)) // gap-space-xs

                IconButton(
                    onClick = onMoreOptionsClick,
                    modifier = Modifier.size(44.dp), // w-11 h-11
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        imageVector = OneRouteIcons.MoreVert,
                        contentDescription = stringResource(R.string.verify_otp_more_options),
                        modifier = Modifier.size(24.dp), // text-[24px]
                    )
                }
            }
        }
    }
}

/**
 * Local stand-in for the remote brand mark in the mock: a primary disc with a
 * white route glyph, so the header keeps its visual weight without pulling in
 * an image loader for a static asset.
 */
@Composable
private fun BrandMark(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp) // h-8
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = OneRouteIcons.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 44dp round trigger carrying the 32dp profile picture of the mock. */
@Composable
private fun AccountButton(
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.verify_otp_profile_avatar)

    IconButton(
        onClick = onAccountClick,
        modifier = modifier.size(44.dp), // w-11 h-11
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp) // w-8 h-8
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = OneRouteIcons.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Verification banner
// ---------------------------------------------------------------------------

@Composable
private fun VerificationBanner(
    destination: String,
    codeLength: Int,
    onEditEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(BannerGlyphContainer), // w-16 h-16
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = OneRouteElevation.Resting, // shadow-sm
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = OneRouteIcons.MarkEmailRead,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp), // text-[32px]
                )
            }
        }

        Spacer(Modifier.height(OneRouteSpacing.SpaceMd)) // mb-space-md

        Text(
            text = stringResource(R.string.verify_otp_hero_title),
            style = OneRouteType.HeadlineMedium, // headline-md
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(OneRouteSpacing.SpaceXs)) // mb-space-xs

        val sentTo = stringResource(R.string.verify_otp_hero_body, codeLength)
        Text(
            // "Hemos enviado un código de N dígitos a " + the destination in
            // `font-medium text-on-surface`, exactly like the mock's <span>.
            text = buildAnnotatedString {
                append(sentTo)
                append(' ')
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    append(destination)
                }
            },
            style = MaterialTheme.typography.bodyMedium, // body-md
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = BannerTextMaxWidth), // max-w-[320px]
        )

        Spacer(Modifier.height(OneRouteSpacing.SpaceXs)) // mt-space-xs

        ChangeEmailAction(onEditEmailClick = onEditEmailClick)
    }
}

/** `mt-space-xs inline-flex items-center gap-1` "Cambiar correo" trigger. */
@Composable
private fun ChangeEmailAction(
    onEditEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.verify_otp_change_email)

    Row(
        modifier = modifier.clickable(
            role = Role.Button,
            onClickLabel = label,
            onClick = onEditEmailClick,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge, // label-lg
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = OneRouteIcons.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp), // text-[16px]
        )
    }
}

// ---------------------------------------------------------------------------
// OTP card
// ---------------------------------------------------------------------------

@Composable
private fun OtpCard(
    state: VerifyOtpUiState,
    onCodeChange: (String) -> Unit,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(OneRouteRadius.Card), // rounded-lg
        shadowElevation = OneRouteElevation.Resting, // shadow-sm
    ) {
        Column(modifier = Modifier.padding(OneRouteSpacing.SpaceMd)) { // p-space-md
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.verify_otp_card_label),
                    style = MaterialTheme.typography.labelLarge, // label-lg
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TlsBadge()
            }

            Spacer(Modifier.height(OneRouteSpacing.SpaceSm)) // mb-space-sm

            OtpInput(
                state = state,
                onCodeChange = onCodeChange,
                modifier = Modifier.padding(vertical = OneRouteSpacing.SpaceXs), // my-space-xs
            )

            // `mt-space-md pt-space-xs` on the countdown row.
            Spacer(Modifier.height(OneRouteSpacing.SpaceMd + OneRouteSpacing.SpaceXs))

            ResendRow(
                timerSeconds = state.timerSeconds,
                canResend = state.canResend,
                onResendClick = onResendClick,
            )
        }
    }
}

/** `shield` + "Cifrado TLS" pill in the card header. */
@Composable
private fun TlsBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape) // rounded-full
            .background(MaterialTheme.colorScheme.primaryFixed)
            .padding(horizontal = 8.dp, vertical = 2.dp), // px-2 py-0.5
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
    ) {
        Icon(
            imageVector = OneRouteIcons.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp), // text-[14px]
        )
        Text(
            text = stringResource(R.string.verify_otp_card_tls),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ---------------------------------------------------------------------------
// PIN cells
// ---------------------------------------------------------------------------

/**
 * The cells of the mock plus the transparent field that drives them.
 *
 * A single [BasicTextField] spans the whole grid so typing stays sequential and
 * fluid — digits land in order, backspace removes the last one — while the
 * field itself paints nothing: the cells below are the visible representation
 * of the code, and the field only owns focus, the numeric keyboard and the
 * semantics a screen reader needs.
 */
@Composable
private fun OtpInput(
    state: VerifyOtpUiState,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // The mock opens with one cell already focused, keypad included.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(state.codeLength) { index ->
                OtpCell(
                    digit = state.digitAt(index),
                    isActive = index == state.activeCellIndex,
                )
            }
        }

        BasicTextField(
            value = state.code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester),
            // The cells render the digits, so the field itself stays invisible.
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        )
    }
}

/** One 48×56dp PIN cell: filled, active or idle, as in the mock. */
@Composable
private fun OtpCell(
    digit: Char?,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OneRouteRadius.Medium) // rounded-2xl → 1rem
    val primary = MaterialTheme.colorScheme.primary

    val container = when {
        digit != null -> MaterialTheme.colorScheme.surfaceContainerHighest
        isActive -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Box(
        modifier = modifier
            .width(OtpCellWidth) // w-12
            .height(OtpCellHeight) // h-14
            // The mock adds `shadow-inner` to the filled cells; Compose has no
            // inner-shadow primitive, so the tonal step carries the depth.
            .clip(shape)
            .background(container)
            .then(if (isActive) Modifier.border(2.dp, primary, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            digit != null -> Text(
                text = digit.toString(),
                style = OneRouteType.HeadlineMedium, // headline-md
                color = primary,
            )

            isActive -> OtpCaret()

            else -> Text(
                text = OtpIdleGlyph, // `·`
                style = OneRouteType.HeadlineMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * The `w-0.5 h-6` blinking bar of the focused cell. The mock pings it; a
 * reversing alpha ramp is the Compose equivalent that survives on API 29.
 */
@Composable
private fun OtpCaret(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "otpCaret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(OtpCaretBlinkMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "otpCaretAlpha",
    )

    Box(
        modifier = modifier
            .width(2.dp) // w-0.5
            .height(24.dp) // h-6
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.primary),
    )
}

// ---------------------------------------------------------------------------
// Countdown & resend
// ---------------------------------------------------------------------------

/** Countdown on the left, "Reenviar código" on the right, as in the mock. */
@Composable
private fun ResendRow(
    timerSeconds: Int,
    canResend: Boolean,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountdownLabel(timerSeconds = timerSeconds)

        ResendAction(
            canResend = canResend,
            onResendClick = onResendClick,
            label = stringResource(R.string.verify_otp_resend_action),
        )
    }
}

/** `timer` glyph plus "Reenviar en **00:45**". */
@Composable
private fun CountdownLabel(timerSeconds: Int, modifier: Modifier = Modifier) {
    val time = countdownText(timerSeconds)
    val template = stringResource(R.string.verify_otp_resend_in, time)
    val emphasis = MaterialTheme.colorScheme.onSurface

    val text = buildAnnotatedString {
        append(template)
        val start = template.indexOf(time)
        if (start >= 0) {
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = emphasis),
                start,
                start + time.length,
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
    ) {
        Icon(
            imageVector = OneRouteIcons.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp), // text-[18px]
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall, // label-sm
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Text-only resend trigger. Disabled it mirrors the mock's
 * `text-outline cursor-not-allowed`; enabled it steps up to `text-primary`.
 */
@Composable
private fun ResendAction(
    canResend: Boolean,
    onResendClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge, // label-lg
        color = if (canResend) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        modifier = modifier.clickable(
            enabled = canResend,
            role = Role.Button,
            onClickLabel = label,
            onClick = onResendClick,
        ),
    )
}

/** "¿No recibiste el código?  Reenviar" — the screen's second resend entry. */
@Composable
private fun ResendPrompt(
    canResend: Boolean,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val link = stringResource(R.string.verify_otp_resend_link)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = OneRouteIcons.Help,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp), // text-[16px]
        )

        Spacer(Modifier.width(4.dp)) // gap-1

        Text(
            text = stringResource(R.string.verify_otp_resend_prompt),
            style = MaterialTheme.typography.labelSmall, // label-sm
            color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(Modifier.width(4.dp)) // gap-1

        Text(
            text = link,
            style = MaterialTheme.typography.labelLarge, // label-lg
            color = if (canResend) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.clickable(
                enabled = canResend,
                role = Role.Button,
                onClickLabel = link,
                onClick = onResendClick,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// Primary action
// ---------------------------------------------------------------------------

/**
 * Full-width 48dp full-pill CTA. Disabled until the six digits are in, playing
 * the local "Verificando…" spinner while [isLoading] holds.
 */
@Composable
private fun VerifyButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f, // active:scale-[0.98]
        label = "verifyScale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // h-12
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = CircleShape, // rounded-full
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = OneRouteElevation.Active, // shadow-md
            pressedElevation = OneRouteElevation.Resting, // active:shadow-sm
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = OneRouteSpacing.SpaceMd),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp), // text-[20px]
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(OneRouteSpacing.SpaceSm)) // gap-2
            Text(
                text = stringResource(R.string.verify_otp_verifying),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        } else {
            Text(
                text = stringResource(R.string.verify_otp_verify),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.width(OneRouteSpacing.SpaceSm)) // gap-2
            Icon(
                imageVector = OneRouteIcons.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp), // text-[20px]
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Geometry
// ---------------------------------------------------------------------------

/** 64dp hero disc (`w-16 h-16`). */
private val BannerGlyphContainer = 64.dp

/** `max-w-[320px]` on the descriptive copy. */
private val BannerTextMaxWidth = 320.dp

/** PIN cell geometry (`w-12 h-14`). */
private val OtpCellWidth = 48.dp
private val OtpCellHeight = 56.dp

/** The `·` drawn by every idle cell. */
private const val OtpIdleGlyph = "·"

/** Half-period of the focused-cell caret. */
private const val OtpCaretBlinkMillis = 500

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Verify OTP · Empty", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun VerifyOtpScreenEmptyPreview() {
    OneRouteAndroidTheme {
        VerifyOtpScreen()
    }
}

@Preview(
    name = "Verify OTP · Typing (mock)",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun VerifyOtpScreenTypingPreview() {
    OneRouteAndroidTheme {
        VerifyOtpScreenContent(state = PreviewTypingState)
    }
}

@Preview(
    name = "Verify OTP · Complete",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun VerifyOtpScreenCompletePreview() {
    OneRouteAndroidTheme {
        VerifyOtpScreenContent(state = PreviewTypingState.withCode("849245"))
    }
}

@Preview(
    name = "Verify OTP · Resend ready",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun VerifyOtpScreenResendPreview() {
    OneRouteAndroidTheme {
        VerifyOtpScreenContent(
            state = PreviewTypingState.copy(timerSeconds = 0, canResend = true),
        )
    }
}

@Preview(
    name = "Verify OTP · Verifying",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun VerifyOtpScreenVerifyingPreview() {
    OneRouteAndroidTheme {
        VerifyOtpScreenContent(
            state = PreviewTypingState.withCode("849245").copy(isLoading = true),
        )
    }
}

/** Four digits in, the fifth cell focused — exactly what the mock shows. */
private val PreviewTypingState = VerifyOtpUiState().withCode("8492")
