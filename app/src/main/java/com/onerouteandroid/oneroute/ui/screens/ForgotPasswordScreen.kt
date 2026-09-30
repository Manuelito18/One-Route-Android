package com.onerouteandroid.oneroute.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onerouteandroid.oneroute.R
import com.onerouteandroid.oneroute.ui.icons.OneRouteIcons
import com.onerouteandroid.oneroute.ui.theme.OneRouteAndroidTheme
import com.onerouteandroid.oneroute.ui.theme.OneRouteElevation
import com.onerouteandroid.oneroute.ui.theme.OneRoutePrimaryFixedDim
import com.onerouteandroid.oneroute.ui.theme.OneRouteRadius
import com.onerouteandroid.oneroute.ui.theme.OneRouteSecondaryFixed
import com.onerouteandroid.oneroute.ui.theme.OneRouteSpacing
import com.onerouteandroid.oneroute.ui.theme.OneRouteType
import kotlinx.coroutines.delay

/*
 * =============================================================================
 *  Password recovery — "¿Olvidaste tu contraseña?"
 * =============================================================================
 *  Compose port of `specs/ui/recuperar_contrasena_screen.html`, built exclusively
 *  on the tokens declared in `specs/ui/DESIGN.md`.
 *
 *  Scope: presentation and local interaction only. Nothing here talks to a
 *  backend, persists a draft or sends an e-mail — the "Enviar código OTP" tap
 *  only drives the button's loading → confirmation micro-interaction that the
 *  mock implements with a 900 ms `setTimeout`.
 *
 *  Layering:
 *   • [ForgotPasswordUiState]  – one immutable snapshot of everything needed to
 *                                draw a frame: the contact value for the active
 *                                method, focus, validation feedback and the
 *                                status of the primary action.
 *   • [ForgotPasswordScreenContent] – stateless and fully hoisted. It takes that
 *                                state plus one callback per user intent, so it
 *                                renders from a @Preview, a test or any other
 *                                destination without extra wiring.
 *   • [ForgotPasswordScreen]   – stateful container that owns the in-memory
 *                                state, runs the field validation and drives the
 *                                "Enviar código OTP → Enviando… → ¡Código
 *                                enviado!" feedback loop.
 * =============================================================================
 */

// ---------------------------------------------------------------------------
// State model
// ---------------------------------------------------------------------------

/** The two channels through which a registered user can receive a code. */
enum class RecoveryMethod { Email, Sms }

/** Minimum digits accepted in the phone field, ignoring spacing characters. */
private const val MinPhoneDigits = 10

/**
 * Character count after which the mock reveals the trailing confirmation tick
 * (`val.trim().length >= 5` in `validateInput`).
 */
private const val MinContactLength = 5

/** Length in characters accepted in the e-mail field. */
private val EmailRegex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

/**
 * Everything [ForgotPasswordScreenContent] needs in order to render one frame.
 *
 * Free of `Context`, callbacks and coroutine scopes by design: a preview or a
 * unit test can build one from a literal and assert on it directly.
 */
@Immutable
data class ForgotPasswordUiState(
    // --- text fields ---
    val email: String = "",
    val phone: String = "",

    // --- flags ---
    val method: RecoveryMethod = RecoveryMethod.Email,
    val isFieldFocused: Boolean = false,

    // --- primary action ---
    val isSubmitted: Boolean = false,
    val isLoading: Boolean = false,

    // --- validation feedback, rendered in the helper line of the field ---
    val emailError: String? = null,
    val phoneError: String? = null,
) {
    /** The value of whichever field the active [method] is bound to. */
    val contactValue: String
        get() = when (method) {
            RecoveryMethod.Email -> email
            RecoveryMethod.Sms -> phone
        }

    /** The error belonging to the active [method], if any. */
    val contactError: String?
        get() = when (method) {
            RecoveryMethod.Email -> emailError
            RecoveryMethod.Sms -> phoneError
        }

    val isEmailValid: Boolean
        get() = EmailRegex.matches(email.trim())

    /** Phone digits, ignoring the spaces typed for legibility. */
    val isPhoneValid: Boolean
        get() = phone.count(Char::isDigit) >= MinPhoneDigits

    val isContactValid: Boolean
        get() = when (method) {
            RecoveryMethod.Email -> isEmailValid
            RecoveryMethod.Sms -> isPhoneValid
        }

    /** The trailing confirmation glyph appears once the value is long enough. */
    val isContactLongEnough: Boolean
        get() = contactValue.trim().length >= MinContactLength

    val isSubmitEnabled: Boolean
        get() = isContactValid && !isLoading

    /** True while the primary action must ignore further taps. */
    val isBusy: Boolean
        get() = isLoading
}

/** Resolved copy for the validation messages, so validation stays pure. */
@Immutable
private data class ForgotPasswordErrorCopy(
    val email: String,
    val phone: String,
)

/**
 * Validates only the field the active method is bound to, mirroring the mock,
 * which only ever surfaces the error of the field on screen.
 *
 * Pure: no `Context`, no coroutines, directly unit-testable.
 */
private fun validateContact(
    state: ForgotPasswordUiState,
    copy: ForgotPasswordErrorCopy,
): Pair<ForgotPasswordUiState, Boolean> {
    val emailError = state.emailError?.takeIf { !state.isEmailValid }
        ?: copy.email.takeIf { !state.isEmailValid }
    val phoneError = state.phoneError?.takeIf { !state.isPhoneValid }
        ?: copy.phone.takeIf { !state.isPhoneValid }

    val validated = when (state.method) {
        RecoveryMethod.Email -> state.copy(emailError = emailError, phoneError = null)
        RecoveryMethod.Sms -> state.copy(emailError = null, phoneError = phoneError)
    }

    return validated to (validated.contactError != null)
}

// ---------------------------------------------------------------------------
// Container
// ---------------------------------------------------------------------------

/**
 * Stateful entry point for the password-recovery screen.
 *
 * Owns the in-memory [ForgotPasswordUiState] so the screen is interactive the
 * moment it is shown: the field accepts text, the method selector switches
 * between e-mail and SMS (re-labelling the field, its icon and its helper),
 * the primary action validates before playing the "Enviando código… / ¡Código
 * enviado!" feedback loop, and both the top app bar back button and the
 * "¿Recordaste tu contraseña?" link report their intent through a callback.
 *
 * @param onNavigateBack invoked from the top app bar back button.
 * @param onNavigateToLogin invoked when the user taps "Iniciar sesión".
 */
@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    var state by remember { mutableStateOf(ForgotPasswordUiState()) }

    val errorCopy = ForgotPasswordErrorCopy(
        email = stringResource(R.string.forgot_error_email),
        phone = stringResource(R.string.forgot_error_phone),
    )

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Mirrors the `setTimeout(..., 900)` submit micro-interaction of the mock.
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            delay(SubmitIndicatorDurationMillis)
            state = state.copy(isLoading = false, isSubmitted = true)
        }
    }

    ForgotPasswordScreenContent(
        state = state,
        modifier = modifier,
        onEmailChange = { value ->
            state = state.copy(email = value, emailError = null, isSubmitted = false)
        },
        onPhoneChange = { value ->
            state = state.copy(phone = value, phoneError = null, isSubmitted = false)
        },
        onMethodChange = { method ->
            state = state.copy(
                method = method,
                isSubmitted = false,
                emailError = if (method == RecoveryMethod.Email) state.emailError else null,
                phoneError = if (method == RecoveryMethod.Sms) state.phoneError else null,
            )
        },
        onFieldFocusChange = { focused -> state = state.copy(isFieldFocused = focused) },
        onKeyboardSend = {
            keyboard?.hide()
            focusManager.clearFocus()
        },
        onSubmitClick = {
            if (state.isBusy) return@ForgotPasswordScreenContent
            val (validated, hasError) = validateContact(state, errorCopy)
            state = if (hasError) {
                validated
            } else {
                state.copy(isLoading = true, isSubmitted = false)
            }
        },
        onBackClick = onNavigateBack,
        onNavigateToLogin = onNavigateToLogin,
    )
}

/** How long the "Enviando código…" state stays on screen before success. */
private const val SubmitIndicatorDurationMillis = 900L

// ---------------------------------------------------------------------------
// Stateless content
// ---------------------------------------------------------------------------

/**
 * Stateless, fully hoisted password-recovery screen.
 *
 * Renders [state] and reports every user intent through its callbacks. It owns
 * no state beyond transient interaction feedback (press scaling, animated
 * colours, the ambient pulse and the label float), so a [ForgotPasswordUiState]
 * literal is all a preview or a Compose test needs.
 */
@Composable
fun ForgotPasswordScreenContent(
    state: ForgotPasswordUiState,
    modifier: Modifier = Modifier,
    onEmailChange: (String) -> Unit = {},
    onPhoneChange: (String) -> Unit = {},
    onMethodChange: (RecoveryMethod) -> Unit = {},
    onFieldFocusChange: (Boolean) -> Unit = {},
    onKeyboardSend: () -> Unit = {},
    onSubmitClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            ForgotPasswordTopBar(
                title = stringResource(R.string.forgot_top_bar_title),
                backContentDescription = stringResource(R.string.forgot_navigate_back),
                profileContentDescription = stringResource(R.string.forgot_profile_avatar),
                brandContentDescription = stringResource(R.string.forgot_brand_logo),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .navigationBarsPadding()
                // Keeps the CTA reachable once the keyboard claims the lower half.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OneRouteSpacing.Margin)
                // The mock's `pb-12` on <main> plus `pb-space-xl` on the content.
                .padding(bottom = 48.dp + OneRouteSpacing.SpaceXl),
        ) {
            RecoveryHeroSection(
                method = state.method,
                modifier = Modifier.padding(top = OneRouteSpacing.SpaceSm), // mt-2
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg)) // mb-space-lg

            MethodSelector(
                state = state,
                onMethodChange = onMethodChange,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg))

            ContactField(
                state = state,
                onEmailChange = onEmailChange,
                onPhoneChange = onPhoneChange,
                onFieldFocusChange = onFieldFocusChange,
                onKeyboardSend = onKeyboardSend,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg))

            RecoveryFeedbackCard(
                state = state,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg))

            RecoverySubmitButton(
                isLoading = state.isLoading,
                isSubmitted = state.isSubmitted,
                onClick = onSubmitClick,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceXs)) // pt-space-xs

            LoginRedirection(onNavigateToLogin = onNavigateToLogin)
        }
    }
}

// ---------------------------------------------------------------------------
// Top app bar
// ---------------------------------------------------------------------------

@Composable
private fun ForgotPasswordTopBar(
    title: String,
    backContentDescription: String,
    profileContentDescription: String,
    brandContentDescription: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        // `bg-surface/85 backdrop-blur-xl`. Compose has no stable backdrop blur,
        // so the translucent plane plus a hairline shadow stand in for it.
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
                    .padding(horizontal = OneRouteSpacing.SpaceSm) // px-space-xs
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
                        contentDescription = backContentDescription,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(OneRouteSpacing.SpaceXs))

                BrandMark(contentDescription = brandContentDescription)

                Spacer(Modifier.width(OneRouteSpacing.SpaceXs))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )

                Spacer(Modifier.weight(1f))

                ProfileAvatar(contentDescription = profileContentDescription)
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

@Composable
private fun ProfileAvatar(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp) // w-8 h-8
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .semantics { this.contentDescription = contentDescription },
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

// ---------------------------------------------------------------------------
// Hero: security chip, lock illustration, title and subtitle
// ---------------------------------------------------------------------------

@Composable
private fun RecoveryHeroSection(
    method: RecoveryMethod,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SecurityBadge()

        Spacer(Modifier.height(OneRouteSpacing.SpaceMd)) // mb-space-md

        LockIllustration(method = method)

        Spacer(Modifier.height(OneRouteSpacing.SpaceMd)) // mb-space-md

        Text(
            text = stringResource(R.string.forgot_title),
            style = OneRouteType.HeadlineLargeMobile,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(OneRouteSpacing.SpaceXs)) // mb-space-xs

        Text(
            text = stringResource(R.string.forgot_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // `max-w-[340px]` keeps the copy wrapping as approved.
            modifier = Modifier.width(340.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/** `bg-secondary-container text-on-secondary-container rounded-full` chip. */
@Composable
private fun SecurityBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        shadowElevation = OneRouteElevation.Resting, // shadow-sm
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp, // px-3
                vertical = 4.dp, // py-1
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
        ) {
            Icon(
                imageVector = OneRouteIcons.LockReset,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = stringResource(R.string.forgot_badge),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * 96dp ambient container holding the three concentric discs of the mock, the
 * 28dp shield badge anchored to its lower right, and the hero glyph whose
 * identity — and micro "unlock" wobble — follow the active [method].
 */
@Composable
private fun LockIllustration(
    method: RecoveryMethod,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "heroGlow")
    // `animate-pulse` on the blurred ambient blob.
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroGlowAlpha",
    )

    // The mock nudges the glyph 12° on every method switch. The first
    // composition is skipped so the illustration does not wobble on entry.
    val wobble = remember { Animatable(0f) }
    var hasRenderedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(method) {
        if (hasRenderedOnce) {
            val target = if (method == RecoveryMethod.Sms) 12f else -12f
            wobble.animateTo(target, tween(durationMillis = 120))
            wobble.animateTo(0f, tween(durationMillis = 120))
        } else {
            hasRenderedOnce = true
        }
    }

    val heroDescription = stringResource(R.string.forgot_illustration_description)
    val shieldDescription = stringResource(R.string.forgot_shield_description)

    Box(
        modifier = modifier
            .size(96.dp) // w-24 h-24
            .semantics { contentDescription = heroDescription },
        contentAlignment = Alignment.Center,
    ) {
        // Fallback wash for API levels where `Modifier.blur` is a no-op.
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(OneRoutePrimaryFixedDim.copy(alpha = 0.10f)),
        )

        // `blur-xl` ambient glow blob.
        Box(
            modifier = Modifier
                .size(88.dp)
                .blur(radius = 18.dp)
                .clip(CircleShape)
                .background(OneRoutePrimaryFixedDim.copy(alpha = glowAlpha)),
        )

        // `w-20 h-20 bg-secondary-container` outer disc.
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = OneRouteElevation.Active, // shadow-md
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                // `w-14 h-14 bg-primary-container` inner disc.
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (method) {
                            RecoveryMethod.Email -> OneRouteIcons.LockOpen
                            RecoveryMethod.Sms -> OneRouteIcons.Lock
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(wobble.value),
                    )
                }
            }
        }

        // Floating shield tag at `-bottom-1 -right-1`.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp) // -bottom-1 -right-1
                .size(28.dp) // w-7 h-7
                .semantics { contentDescription = shieldDescription },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = OneRouteElevation.Active,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = OneRouteIcons.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Recovery method selector
// ---------------------------------------------------------------------------

@Composable
private fun MethodSelector(
    state: ForgotPasswordUiState,
    onMethodChange: (RecoveryMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupDescription = stringResource(R.string.forgot_method_group_description)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = groupDescription },
        verticalArrangement = Arrangement.spacedBy(OneRouteSpacing.SpaceSm), // gap-space-sm
    ) {
        RecoveryMethodCard(
            method = RecoveryMethod.Email,
            isSelected = state.method == RecoveryMethod.Email,
            value = state.email,
            title = stringResource(R.string.forgot_method_email_title),
            placeholder = stringResource(R.string.forgot_method_email_placeholder),
            onSelect = { onMethodChange(RecoveryMethod.Email) },
        )

        RecoveryMethodCard(
            method = RecoveryMethod.Sms,
            isSelected = state.method == RecoveryMethod.Sms,
            value = state.phone,
            title = stringResource(R.string.forgot_method_sms_title),
            placeholder = stringResource(R.string.forgot_method_sms_placeholder),
            onSelect = { onMethodChange(RecoveryMethod.Sms) },
        )
    }
}

/**
 * One 16dp-radius option row: a 44dp tinted icon disc, the two-line copy, and
 * the 24dp radio that reflects the selection.
 */
@Composable
private fun RecoveryMethodCard(
    method: RecoveryMethod,
    isSelected: Boolean,
    value: String,
    title: String,
    placeholder: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OneRouteRadius.Medium) // rounded-DEFAULT

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "methodCardColor",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "methodIconContainerColor",
    )
    val iconContentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "methodIconContentColor",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        shape = shape,
        color = containerColor,
        shadowElevation = OneRouteElevation.Resting, // shadow-sm
    ) {
        Row(
            modifier = Modifier.padding(OneRouteSpacing.SpaceMd), // p-space-md
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp) // w-11 h-11
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (method) {
                        RecoveryMethod.Email -> OneRouteIcons.Mail
                        RecoveryMethod.Sms -> OneRouteIcons.Sms
                    },
                    contentDescription = null,
                    tint = iconContentColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(OneRouteSpacing.SpaceMd)) // gap-space-md

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    // The mock shows the live value here, falling back to a
                    // masked sample while the field is still empty.
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.width(OneRouteSpacing.SpaceSm)) // pl-2

            RecoveryRadio(isSelected = isSelected)
        }
    }
}

/** 24dp radio: primary fill + 10dp dot when selected, flat container otherwise. */
@Composable
private fun RecoveryRadio(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "radioTrackColor",
    )
    val dotColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            Color.Transparent
        },
        label = "radioDotColor",
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(160),
        label = "radioDotScale",
    )

    Box(
        modifier = modifier
            .size(24.dp) // w-6 h-6
            .clip(CircleShape)
            .background(trackColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp) // w-2.5 h-2.5
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                }
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

// ---------------------------------------------------------------------------
// Contact field
// ---------------------------------------------------------------------------

/**
 * The single 56dp input of the screen. Its label, leading icon, keyboard type
 * and helper copy all follow the active [ForgotPasswordUiState.method], which
 * is what makes the two option cards feel like they drive the form.
 */
@Composable
private fun ContactField(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onFieldFocusChange: (Boolean) -> Unit,
    onKeyboardSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmail = state.method == RecoveryMethod.Email
    val error = state.contactError

    val label = if (isEmail) {
        stringResource(R.string.forgot_email_label)
    } else {
        stringResource(R.string.forgot_sms_label)
    }
    val placeholder = if (isEmail) {
        stringResource(R.string.forgot_email_placeholder)
    } else {
        stringResource(R.string.forgot_sms_placeholder)
    }
    val helper = error ?: if (isEmail) {
        stringResource(R.string.forgot_email_helper)
    } else {
        stringResource(R.string.forgot_sms_helper)
    }

    RecoveryTextField(
        value = state.contactValue,
        onValueChange = if (isEmail) onEmailChange else onPhoneChange,
        label = label,
        placeholder = placeholder,
        helper = helper,
        isError = error != null,
        isFocused = state.isFieldFocused,
        // The tick only shows once there is no error, so the two never stack.
        showValidTick = state.isContactLongEnough && error == null,
        onFocusChange = onFieldFocusChange,
        leadingIcon = if (isEmail) {
            OneRouteIcons.AlternateEmail
        } else {
            OneRouteIcons.Smartphone
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = if (isEmail) KeyboardType.Email else KeyboardType.Phone,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { onKeyboardSend() }),
        modifier = modifier,
    )
}

/**
 * One 16dp white plane carrying a floating label, a leading icon, an optional
 * trailing confirmation tick and a helper line underneath.
 *
 * Built on [BasicTextField] rather than Material's `TextField` on purpose: the
 * mock asks for a borderless plane whose shadow steps from Level 1 to Level 2
 * on focus, a label that floats *over* the top edge, and a helper line inset
 * 12dp from the field edge rather than from the text column. None of that is
 * reachable through Material's slots, and a `TextField` cannot cast the tinted
 * shadow because it owns its own background.
 */
@Composable
private fun RecoveryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    helper: String,
    isError: Boolean,
    isFocused: Boolean,
    showValidTick: Boolean,
    onFocusChange: (Boolean) -> Unit,
    leadingIcon: ImageVector,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OneRouteRadius.Medium) // rounded-DEFAULT
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    // The label floats as soon as the field holds a value or has focus, exactly
    // like the mock's `peer-placeholder-shown` / `peer-focus` peer selectors.
    val labelFloated = isFocused || value.isNotEmpty()

    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused && !isError) OneRouteElevation.Active else OneRouteElevation.Resting,
        animationSpec = tween(180),
        label = "fieldShadowElevation",
    )
    val shadowAmbient by animateColorAsState(
        targetValue = if (isFocused) OneRouteElevation.ActiveAmbient else OneRouteElevation.RestingAmbient,
        label = "fieldShadowAmbient",
    )
    val shadowSpot by animateColorAsState(
        targetValue = if (isFocused) OneRouteElevation.ActiveSpot else OneRouteElevation.RestingSpot,
        label = "fieldShadowSpot",
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> error
            labelFloated -> primary
            else -> onSurfaceVariant
        },
        label = "fieldLabelColor",
    )
    val labelScale by animateFloatAsState(
        targetValue = if (labelFloated) 0.8f else 1f, // scale-[0.8]
        animationSpec = tween(180),
        label = "fieldLabelScale",
    )
    val fieldInteractionSource = remember { MutableInteractionSource() }

    val helperColor = when {
        isError -> error
        showValidTick -> primary
        else -> onSurfaceVariant
    }
    val helperIcon = when {
        isError -> OneRouteIcons.Close
        showValidTick -> OneRouteIcons.CheckCircle
        else -> OneRouteIcons.Info
    }
    val helperIconDescription = when {
        isError -> null
        showValidTick -> stringResource(R.string.forgot_field_valid)
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ContactFieldHeight) // h-14
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = shadowAmbient,
                    spotColor = shadowSpot,
                )
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .then(
                    if (isError) Modifier.border(1.dp, error, shape) else Modifier,
                )
                .onFocusChanged { onFocusChange(it.isFocused) },
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier
                        .padding(start = LeadingStartInset) // left-4
                        .size(LeadingIconSize), // text-[22px]
                )

                Spacer(Modifier.width(LeadingGap)) // mr-3

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = OneRouteSpacing.SpaceMd)
                            // pt-4 pb-1
                            .padding(top = 16.dp, bottom = 4.dp),
                        enabled = true,
                        readOnly = false,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isError) error else primary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        interactionSource = fieldInteractionSource,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                // The placeholder takes the label's resting slot,
                                // so the two can never overlap.
                                if (!labelFloated) {
                                    Text(
                                        text = placeholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )

                    // Explicitly the top-level overload: the enclosing `Row` also
                    // brings a `RowScope.AnimatedVisibility` into scope, and the
                    // unlabelled form would be ambiguous inside the nested `Box`.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = labelFloated,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(140)),
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer {
                                // The mock scales the label from its own origin.
                                scaleX = labelScale
                                scaleY = labelScale
                                transformOrigin = TransformOrigin(0f, 0f)
                            },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showValidTick,
                    enter = fadeIn(tween(160)) + expandVertically(tween(160)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
                ) {
                    Icon(
                        imageVector = OneRouteIcons.CheckCircle,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier
                            .padding(end = TrailingEndInset) // right-3.5
                            .size(20.dp),
                    )
                }
            }
        }

        FieldHelperLine(
            text = helper,
            color = helperColor,
            icon = helperIcon,
            iconContentDescription = helperIconDescription,
            modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp), // pt-1.5 px-3
        )
    }
}

private val ContactFieldHeight = 56.dp

/** Inset of the leading glyph from the field's left edge (`left-4`). */
private val LeadingStartInset = 16.dp

/** Width of the 22dp leading glyph. */
private val LeadingIconSize = 22.dp

/** Gap between the leading glyph and the text column (`mr-3`). */
private val LeadingGap = 12.dp

/** Inset of the trailing tick from the field's right edge (`right-3.5`). */
private val TrailingEndInset = 14.dp

/** Helper / error line under the field, matching the mock's `pt-1.5 px-3`. */
@Composable
private fun FieldHelperLine(
    text: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconContentDescription: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            tint = color,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

// ---------------------------------------------------------------------------
// Information / confirmation card
// ---------------------------------------------------------------------------

/**
 * The card under the form. It shows the mock's "Vigencia temporal" note while
 * the request is idle and swaps to a local confirmation of the channel the
 * code was "sent" to, so the tap on the CTA always produces visible feedback
 * without any network call.
 */
@Composable
private fun RecoveryFeedbackCard(
    state: ForgotPasswordUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = state.isSubmitted,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(140)),
        ) {
            SuccessCard(
                method = state.method,
                contact = state.contactValue.trim(),
            )
        }

        AnimatedVisibility(
            visible = !state.isSubmitted,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(140)),
        ) {
            ExpiryCard()
        }
    }
}

/** `bg-surface-container-low` "Vigencia temporal" note of the mock. */
@Composable
private fun ExpiryCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(OneRouteRadius.Medium), // rounded-DEFAULT
        shadowElevation = OneRouteElevation.Resting, // shadow-sm
    ) {
        Row(
            modifier = Modifier.padding(OneRouteSpacing.SpaceMd), // p-space-md
            verticalAlignment = Alignment.Top, // items-start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp) // w-8 h-8
                    .clip(CircleShape)
                    .background(OneRouteSecondaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = OneRouteIcons.AvTimer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(12.dp)) // gap-3

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.forgot_expiry_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp)) // mb-0.5
                Text(
                    text = stringResource(R.string.forgot_expiry_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

/**
 * Local confirmation shown once the CTA settles into its success state: a
 * tonal check plus the destination the code would have been delivered to.
 */
@Composable
private fun SuccessCard(
    method: RecoveryMethod,
    contact: String,
    modifier: Modifier = Modifier,
) {
    val title = if (method == RecoveryMethod.Email) {
        stringResource(R.string.forgot_success_title_email)
    } else {
        stringResource(R.string.forgot_success_title_sms)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(OneRouteRadius.Medium), // rounded-DEFAULT
        shadowElevation = OneRouteElevation.Resting,
    ) {
        Row(
            modifier = Modifier.padding(OneRouteSpacing.SpaceMd),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = OneRouteIcons.Check,
                    contentDescription = stringResource(R.string.forgot_success_icon_description),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.forgot_success_body, contact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Primary action
// ---------------------------------------------------------------------------

/**
 * Full-width 48dp full-pill button carrying the three states of the mock's
 * `submit-btn` micro-interaction: "Enviar código OTP" → "Enviando código…" →
 * "¡Código enviado!".
 */
@Composable
private fun RecoverySubmitButton(
    isLoading: Boolean,
    isSubmitted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSubmitted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "submitContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSubmitted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        label = "submitContentColor",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading) 0.99f else 1f, // active:scale-[0.99]
        label = "submitScale",
    )

    // The mock swaps the trailing glyph for a `sync` rotor while in flight.
    val spin = rememberInfiniteTransition(label = "submitSpin")
    val spinAngle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
        ),
        label = "submitSpinAngle",
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
        enabled = !isLoading,
        shape = CircleShape, // rounded-full
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = OneRouteElevation.Active, // shadow-md
            pressedElevation = OneRouteElevation.Resting, // active:shadow-sm
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = OneRouteSpacing.SpaceMd),
    ) {
        when {
            isLoading -> {
                Icon(
                    imageVector = OneRouteIcons.Sync,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(spinAngle),
                )
                Spacer(Modifier.width(OneRouteSpacing.SpaceXs)) // gap-2
                Text(
                    text = stringResource(R.string.forgot_submitting),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            isSubmitted -> SubmitButtonLabel(
                icon = OneRouteIcons.Check,
                text = stringResource(R.string.forgot_success),
            )

            else -> SubmitButtonLabel(
                icon = OneRouteIcons.ArrowForward,
                text = stringResource(R.string.forgot_submit),
            )
        }
    }
}

@Composable
private fun SubmitButtonLabel(icon: ImageVector, text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
    )
    Spacer(Modifier.width(OneRouteSpacing.SpaceXs)) // gap-2
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
}

// ---------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------

/** "¿Recordaste tu contraseña?  Iniciar sesión" */
@Composable
private fun LoginRedirection(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = stringResource(R.string.forgot_login_action)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            // `py-2 px-4 rounded-full` — the pill around the whole tappable area.
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = onNavigateToLogin,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.forgot_login_question),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp)) // ml-1.5
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Forgot password · Empty", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ForgotPasswordScreenEmptyPreview() {
    OneRouteAndroidTheme {
        ForgotPasswordScreen()
    }
}

@Preview(
    name = "Forgot password · Filled (mock)",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun ForgotPasswordScreenFilledPreview() {
    OneRouteAndroidTheme {
        ForgotPasswordScreenContent(state = PreviewFilledState)
    }
}

@Preview(
    name = "Forgot password · SMS method",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun ForgotPasswordScreenSmsPreview() {
    OneRouteAndroidTheme {
        ForgotPasswordScreenContent(
            state = PreviewFilledState.copy(
                method = RecoveryMethod.Sms,
                phone = "+52 55 4912 1047",
            ),
        )
    }
}

@Preview(
    name = "Forgot password · Validation error",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun ForgotPasswordScreenErrorPreview() {
    OneRouteAndroidTheme {
        ForgotPasswordScreenContent(
            state = ForgotPasswordUiState(
                email = "sofia.morales",
                emailError = "Ingresa un correo electrónico válido",
            ),
        )
    }
}

@Preview(
    name = "Forgot password · Code sent",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun ForgotPasswordScreenSentPreview() {
    OneRouteAndroidTheme {
        ForgotPasswordScreenContent(
            state = PreviewFilledState.copy(isSubmitted = true),
        )
    }
}

@Preview(
    name = "Forgot password · Sending",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun ForgotPasswordScreenSendingPreview() {
    OneRouteAndroidTheme {
        ForgotPasswordScreenContent(
            state = PreviewFilledState.copy(isLoading = true),
        )
    }
}

/** The exact value the mock ships with, handy for design review and previews. */
private val PreviewFilledState = ForgotPasswordUiState(
    email = "sofia.morales@ejemplo.com",
)
