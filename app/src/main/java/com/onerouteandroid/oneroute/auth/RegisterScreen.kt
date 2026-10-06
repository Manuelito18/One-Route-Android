package com.onerouteandroid.oneroute.auth

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onerouteandroid.oneroute.R
import com.onerouteandroid.oneroute.ui.components.FieldAnimationMillis
import com.onerouteandroid.oneroute.ui.components.FieldHelperLine
import com.onerouteandroid.oneroute.ui.components.FieldLeadingIcon
import com.onerouteandroid.oneroute.ui.components.FieldPlane
import com.onerouteandroid.oneroute.ui.icons.OneRouteIcons
import com.onerouteandroid.oneroute.ui.theme.OneRouteAndroidTheme
import com.onerouteandroid.oneroute.ui.theme.OneRouteElevation
import com.onerouteandroid.oneroute.ui.theme.OneRouteOnPrimaryFixed
import com.onerouteandroid.oneroute.ui.theme.OneRoutePrimaryFixed
import com.onerouteandroid.oneroute.ui.theme.OneRouteRadius
import com.onerouteandroid.oneroute.ui.theme.OneRouteSpacing
import com.onerouteandroid.oneroute.ui.theme.OneRouteType
import kotlinx.coroutines.delay

/*
 * =============================================================================
 *  Passenger registration — "Únete a RutaCompartida"
 * =============================================================================
 *  Compose port of `specs/ui/registro_screen.html`, built exclusively on the
 *  tokens declared in `specs/ui/DESIGN.md`.
 *
 *  Scope: presentation and local interaction only. No persistence, no network.
 *
 *  Layering:
 *   • [RegisterUiState]      – one immutable snapshot of everything needed to
 *                              draw a frame (text, flags, validation feedback
 *                              and button status).
 *   • [RegisterScreenContent] – stateless and fully hoisted: it takes that
 *                              state plus one callback per user intent, so it
 *                              renders from a @Preview, a test or any other
 *                              destination without extra wiring.
 *   • [RegisterScreen]       – stateful container that owns the in-memory
 *                              state, runs the field validation and drives the
 *                              "Validando… / ¡Cuenta Lista!" micro-interaction.
 * =============================================================================
 */

// ---------------------------------------------------------------------------
// State model
// ---------------------------------------------------------------------------

/** Identifies a field so focus can be reported as part of [RegisterUiState]. */
enum class RegisterField { FullName, Email, Phone, Password }

/** Lifecycle of the primary action, driving the button's three visual states. */
enum class RegisterSubmitState { Idle, Submitting, Success }

/**
 * Password strength, expressed as the number of segments to fill in the
 * four-segment meter rendered under the password field.
 */
enum class PasswordStrength(val filledSegments: Int) {
    Empty(0),
    Weak(1),
    Fair(2),
    Strong(3),
    VeryStrong(4),
}

private const val MinPasswordLength = 8
private const val StrongPasswordLength = 12
private const val MinPhoneDigits = 10
private const val StrengthSegmentCount = 4

/** Length in characters accepted by the e-mail field. */
private val EmailRegex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

/**
 * Everything [RegisterScreenContent] needs in order to render one frame.
 *
 * Free of `Context`, callbacks and coroutine scopes by design: a preview or a
 * unit test can build one from a literal and assert on it directly.
 */
@Immutable
data class RegisterUiState(
    // --- text fields ---
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val countryCode: String = "+52",
    val countryFlag: String = "🇲🇽",

    // --- flags ---
    val isPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val promosAccepted: Boolean = true,
    val focusedField: RegisterField? = null,

    // --- validation feedback, rendered in the helper line of each field ---
    val fullNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val termsError: String? = null,

    // --- primary action ---
    val submitState: RegisterSubmitState = RegisterSubmitState.Idle,
) {
    /** The trailing confirmation glyph appears once the address is well formed. */
    val isEmailValid: Boolean
        get() = email.isNotBlank() && EmailRegex.matches(email.trim())

    /** Phone digits, ignoring the spaces typed for legibility. */
    val phoneDigitCount: Int
        get() = phone.count(Char::isDigit)

    val passwordStrength: PasswordStrength
        get() = passwordStrengthOf(password)

    /** True once every field is valid and mandatory consent has been granted. */
    val isSubmitEnabled: Boolean
        get() = fullName.isNotBlank() &&
            isEmailValid &&
            phoneDigitCount >= MinPhoneDigits &&
            passwordStrength >= PasswordStrength.Strong &&
            termsAccepted

    /** True while the primary action must ignore further taps. */
    val isBusy: Boolean
        get() = submitState == RegisterSubmitState.Submitting
}

/** Pure scoring rule behind [RegisterUiState.passwordStrength]. */
private fun passwordStrengthOf(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.Empty

    var score = 0
    if (password.length >= MinPasswordLength) score++
    if (password.any(Char::isLetter) && password.any(Char::isDigit)) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    if (password.length >= StrongPasswordLength) score++

    return when (score) {
        0, 1 -> PasswordStrength.Weak
        2 -> PasswordStrength.Fair
        3 -> PasswordStrength.Strong
        else -> PasswordStrength.VeryStrong
    }
}

@StringRes
private fun passwordStrengthLabel(strength: PasswordStrength): Int = when (strength) {
    PasswordStrength.Empty, PasswordStrength.Weak -> R.string.register_password_strength_weak
    PasswordStrength.Fair -> R.string.register_password_strength_fair
    PasswordStrength.Strong -> R.string.register_password_strength_strong
    PasswordStrength.VeryStrong -> R.string.register_password_strength_very_strong
}

/** Resolved copy for the validation messages, so validation stays pure. */
@Immutable
private data class RegisterErrorCopy(
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val terms: String,
)

/**
 * Returns the state with per-field errors populated, plus whether any of them
 * fired. Pure: no `Context`, no coroutines, directly unit-testable.
 */
private fun validateRegister(
    state: RegisterUiState,
    copy: RegisterErrorCopy,
): Pair<RegisterUiState, Boolean> {
    val nameError = copy.fullName.takeIf { state.fullName.isBlank() }
    val emailError = copy.email.takeIf { !state.isEmailValid }
    val phoneError = copy.phone.takeIf { state.phoneDigitCount < MinPhoneDigits }
    val passwordError = copy.password.takeIf { state.passwordStrength < PasswordStrength.Strong }
    val termsError = copy.terms.takeIf { !state.termsAccepted }

    val hasErrors = listOf(
        nameError,
        emailError,
        phoneError,
        passwordError,
        termsError,
    ).any { it != null }

    return state.copy(
        fullNameError = nameError,
        emailError = emailError,
        phoneError = phoneError,
        passwordError = passwordError,
        termsError = termsError,
    ) to hasErrors
}

// ---------------------------------------------------------------------------
// Container
// ---------------------------------------------------------------------------

/**
 * Stateful entry point for the registration screen.
 *
 * Owns the in-memory [RegisterUiState] so the screen is interactive the moment
 * it is shown: fields accept text, the password toggle flips, both consent
 * boxes toggle, and the primary action validates before playing the
 * "Validando… / ¡Cuenta Lista!" feedback loop.
 *
 * @param onNavigateToLogin invoked when the user taps "Inicia sesión".
 * @param onNavigateBack invoked when the user taps the top app bar back button.
 * @param onOpenTerms invoked when the user taps the "Términos y Condiciones" link.
 * @param onOpenPrivacy invoked when the user taps the "Política de Privacidad" link.
 */
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
) {
    var state by remember { mutableStateOf(RegisterUiState()) }

    val errorCopy = RegisterErrorCopy(
        fullName = stringResource(R.string.register_error_name),
        email = stringResource(R.string.register_error_email),
        phone = stringResource(R.string.register_error_phone),
        password = stringResource(R.string.register_error_password),
        terms = stringResource(R.string.register_error_terms),
    )

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Mirrors the `setTimeout(..., 900)` submit micro-interaction of the mock.
    LaunchedEffect(state.submitState) {
        if (state.submitState == RegisterSubmitState.Submitting) {
            delay(SubmitIndicatorDurationMillis)
            state = state.copy(submitState = RegisterSubmitState.Success)
        }
    }

    RegisterScreenContent(
        state = state,
        modifier = modifier,
        onFullNameChange = { value -> state = state.copy(fullName = value, fullNameError = null) },
        onEmailChange = { value -> state = state.copy(email = value, emailError = null) },
        onPhoneChange = { value -> state = state.copy(phone = value, phoneError = null) },
        onPasswordChange = { value -> state = state.copy(password = value, passwordError = null) },
        onTogglePasswordVisibility = {
            state = state.copy(isPasswordVisible = !state.isPasswordVisible)
        },
        onTermsAcceptedChange = { accepted ->
            state = state.copy(termsAccepted = accepted, termsError = null)
        },
        onPromosAcceptedChange = { accepted -> state = state.copy(promosAccepted = accepted) },
        onFieldFocusChange = { field, focused ->
            state = state.copy(focusedField = if (focused) field else null)
        },
        onMoveFocusDown = { focusManager.moveFocus(FocusDirection.Down) },
        onKeyboardDone = {
            keyboard?.hide()
            focusManager.clearFocus()
        },
        onRegisterClick = {
            if (state.isBusy) return@RegisterScreenContent
            val (validated, hasErrors) = validateRegister(state, errorCopy)
            state = if (hasErrors) {
                validated
            } else {
                state.copy(submitState = RegisterSubmitState.Submitting, termsError = null)
            }
        },
        onNavigateToLogin = onNavigateToLogin,
        onNavigateBack = onNavigateBack,
        onOpenTerms = onOpenTerms,
        onOpenPrivacy = onOpenPrivacy,
    )
}

/** How long the "Validando…" state stays on screen before success is shown. */
private const val SubmitIndicatorDurationMillis = 900L

// ---------------------------------------------------------------------------
// Stateless content
// ---------------------------------------------------------------------------

/**
 * Stateless, fully hoisted registration screen.
 *
 * Renders [state] and reports every user intent through its callbacks. It owns
 * no state beyond transient interaction feedback (press scaling, animated
 * colours and the label float), so a [RegisterUiState] literal is all a preview
 * or a Compose test needs.
 */
@Composable
fun RegisterScreenContent(
    state: RegisterUiState,
    modifier: Modifier = Modifier,
    onFullNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPhoneChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onTermsAcceptedChange: (Boolean) -> Unit = {},
    onPromosAcceptedChange: (Boolean) -> Unit = {},
    onFieldFocusChange: (RegisterField, Boolean) -> Unit = { _, _ -> },
    onMoveFocusDown: () -> Unit = {},
    onKeyboardDone: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            RegisterTopBar(
                title = stringResource(R.string.register_top_bar_title),
                backContentDescription = stringResource(R.string.register_navigate_back),
                profileContentDescription = stringResource(R.string.register_profile_avatar),
                brandContentDescription = stringResource(R.string.register_brand_logo),
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OneRouteSpacing.Margin)
                // The mock's `pb-12` on <main> plus `pb-space-xl` on the content.
                .padding(bottom = 48.dp + OneRouteSpacing.SpaceXl),
        ) {
            CommunityBanner()

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg))

            Text(
                text = stringResource(R.string.register_title),
                style = OneRouteType.HeadlineLargeMobile,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(OneRouteSpacing.SpaceXs))
            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg))

            RegisterForm(
                state = state,
                onFullNameChange = onFullNameChange,
                onEmailChange = onEmailChange,
                onPhoneChange = onPhoneChange,
                onPasswordChange = onPasswordChange,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onTermsAcceptedChange = onTermsAcceptedChange,
                onPromosAcceptedChange = onPromosAcceptedChange,
                onFieldFocusChange = onFieldFocusChange,
                onMoveFocusDown = onMoveFocusDown,
                onKeyboardDone = onKeyboardDone,
                onRegisterClick = onRegisterClick,
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg))

            LoginRedirection(
                onNavigateToLogin = onNavigateToLogin,
                modifier = Modifier.padding(vertical = OneRouteSpacing.SpaceSm),
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceMd))

            TrustBadge()
        }
    }
}

// ---------------------------------------------------------------------------
// Top app bar
// ---------------------------------------------------------------------------

@Composable
private fun RegisterTopBar(
    title: String,
    backContentDescription: String,
    profileContentDescription: String,
    brandContentDescription: String,
    onNavigateBack: () -> Unit,
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
                    .padding(horizontal = OneRouteSpacing.SpaceSm) // px-space-sm
                    .consumeWindowInsets(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(44.dp), // w-11 h-11
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = OneRouteIcons.ArrowBack,
                        contentDescription = backContentDescription,
                        modifier = Modifier.size(24.dp), // text-2xl
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
// Community banner
// ---------------------------------------------------------------------------

@Composable
private fun CommunityBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(OneRouteRadius.Card), // rounded-lg
        shadowElevation = OneRouteElevation.Resting, // shadow-sm
    ) {
        Row(
            modifier = Modifier.padding(OneRouteSpacing.SpaceMd), // p-space-md
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(bottom = OneRouteSpacing.SpaceXs), // mb-1
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OneRouteSpacing.SpaceXs),
                ) {
                    Icon(
                        imageVector = OneRouteIcons.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.community_badge).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp, // tracking-wider
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(R.string.community_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.community_drivers_verified),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(OneRouteSpacing.SpaceMd))

            CommunityAvatarStack()
        }
    }
}

/** Three overlapping 40dp avatars plus a `+99` overflow bubble. */
@Composable
private fun CommunityAvatarStack(modifier: Modifier = Modifier) {
    val tones = listOf(
        MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.primaryFixed to
            MaterialTheme.colorScheme.onPrimaryFixed,
        MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val description = stringResource(R.string.community_avatar_description)

    Row(
        modifier = modifier.semantics { contentDescription = description },
        // Tailwind's `-space-x-3`.
        horizontalArrangement = Arrangement.spacedBy((-12).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tones.forEach { (background, foreground) ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = OneRouteIcons.Person,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OneRoutePrimaryFixed),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.community_avatar_overflow),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = OneRouteOnPrimaryFixed,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Form
// ---------------------------------------------------------------------------

@Composable
private fun RegisterForm(
    state: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onPromosAcceptedChange: (Boolean) -> Unit,
    onFieldFocusChange: (RegisterField, Boolean) -> Unit,
    onMoveFocusDown: () -> Unit,
    onKeyboardDone: () -> Unit,
    onRegisterClick: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OneRouteSpacing.SpaceMd), // gap-space-md
    ) {
        // -- 1. Full name ---------------------------------------------------
        RegisterTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = stringResource(R.string.register_name_label),
            helper = state.fullNameError ?: stringResource(R.string.register_name_helper),
            isError = state.fullNameError != null,
            focused = state.focusedField == RegisterField.FullName,
            onFocusChange = { onFieldFocusChange(RegisterField.FullName, it) },
            leadingIcon = { FieldLeadingIcon(OneRouteIcons.Person) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
        )

        // -- 2. E-mail -----------------------------------------------------
        RegisterTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.register_email_label),
            // The mock tints this particular helper with the primary role.
            helper = state.emailError
                ?: stringResource(R.string.register_email_helper),
            helperIsPrimary = state.emailError == null,
            isError = state.emailError != null,
            focused = state.focusedField == RegisterField.Email,
            onFocusChange = { onFieldFocusChange(RegisterField.Email, it) },
            leadingIcon = { FieldLeadingIcon(OneRouteIcons.Mail) },
            trailingIcon = if (state.isEmailValid && state.emailError == null) {
                {
                    FieldLeadingIcon(
                        imageVector = OneRouteIcons.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 20.dp,
                    )
                }
            } else {
                null
            },
            textEndPadding = 44.dp, // pr-11
            trailingEndPadding = 14.dp, // right-3.5
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),            keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
        )

        // -- 3. Phone ------------------------------------------------------
        RegisterTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = stringResource(R.string.register_phone_label),
            helper = state.phoneError ?: stringResource(R.string.register_phone_helper),
            isError = state.phoneError != null,
            focused = state.focusedField == RegisterField.Phone,
            onFocusChange = { onFieldFocusChange(RegisterField.Phone, it) },
            leadingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldLeadingIcon(OneRouteIcons.Call)
                    // The mock pins the country pill at `left-11`, i.e. 6dp
                    // past the 22dp glyph that starts at 16dp.
                    Spacer(Modifier.width(6.dp))
                    CountryCodePill(
                        flag = state.countryFlag,
                        code = state.countryCode,
                        contentDescription = stringResource(
                            R.string.register_country_code_description,
                        ),
                    )
                }
            },            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
        )

        // -- 4. Password ---------------------------------------------------
        Column {
            RegisterTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.register_password_label),
                helper = null,
                // No helper line in the design, so nothing is reserved for one.
                reserveHelperSpace = false,
                isError = state.passwordError != null,
                focused = state.focusedField == RegisterField.Password,
                onFocusChange = { onFieldFocusChange(RegisterField.Password, it) },
                leadingIcon = { FieldLeadingIcon(OneRouteIcons.Lock) },
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isVisible = state.isPasswordVisible,
                        onToggle = onTogglePasswordVisibility,
                    )
                },
                textEndPadding = 48.dp, // pr-12
                trailingEndPadding = 8.dp, // right-2
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onKeyboardDone()
                        onRegisterClick()
                    },
                ),
            )

            PasswordStrengthMeter(
                strength = state.passwordStrength,
                modifier = Modifier.padding(top = OneRouteSpacing.SpaceSm),
            )

            // The error replaces the rule bullet so the line never doubles up.
            if (state.passwordError != null) {
                FieldHelperLine(
                    text = state.passwordError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
                )
            }
        }

        // -- 5. Terms & Privacy (mandatory) --------------------------------
        Spacer(Modifier.height(OneRouteSpacing.SpaceXs)) // mt-space-xs
        ConsentRow(
            checked = state.termsAccepted,
            onCheckedChange = onTermsAcceptedChange,
            label = { TermsLabel(onOpenTerms = onOpenTerms, onOpenPrivacy = onOpenPrivacy) },
        )

        // -- 6. Promotions (optional) --------------------------------------
        ConsentRow(
            checked = state.promosAccepted,
            onCheckedChange = onPromosAcceptedChange,
            label = {
                Text(
                    text = stringResource(R.string.register_promos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        // -- 7. Primary action ---------------------------------------------
        RegisterSubmitButton(
            submitState = state.submitState,
            enabled = !state.isBusy,
            onClick = onRegisterClick,
            modifier = Modifier.padding(top = OneRouteSpacing.SpaceSm), // mt-space-sm
        )
    }
}

// ---------------------------------------------------------------------------
// Form primitives
// ---------------------------------------------------------------------------

/**
 * One 56dp white plane carrying a floating label, an optional leading and
 * trailing slot, and a helper line underneath.
 *
 * Built on [BasicTextField] rather than Material's `TextField` on purpose: the
 * mock asks for a borderless plane whose shadow steps from Level 1 to Level 2 on
 * focus (`focus-within:shadow-md`), a label that floats *over* the top edge, and
 * a helper line inset 12dp from the field edge rather than from the text column.
 * None of that is reachable through Material's slots, and a `TextField` cannot
 * cast the tinted shadow because it owns its own background.
 */
@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String?,
    isError: Boolean,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    helperIsPrimary: Boolean = false,
    reserveHelperSpace: Boolean = true,
    // `pr-4` on the input; the mock widens it to `pr-11` / `pr-12` on the two
    // fields that carry a trailing control.
    textEndPadding: Dp = OneRouteSpacing.SpaceMd,
    trailingEndPadding: Dp = textEndPadding,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val shape = RoundedCornerShape(OneRouteRadius.Field) // rounded-[12px]
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    // The label floats as soon as the field holds a value or has focus, exactly
    // like the mock's `peer-placeholder-shown` / `peer-focus` peer selectors.
    val labelFloated = focused || value.isNotEmpty()

    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> error
            focused -> primary
            else -> onSurfaceVariant
        },
        label = "fieldLabelColor",
    )
    val labelOffset by animateDpAsState(
        targetValue = if (labelFloated) (-14).dp else 0.dp, // -translate-y-3.5
        animationSpec = tween(FieldAnimationMillis),
        label = "fieldLabelOffset",
    )
    val labelScale by animateFloatAsState(
        targetValue = if (labelFloated) 0.8f else 1f, // scale-[0.8]
        animationSpec = tween(FieldAnimationMillis),
        label = "fieldLabelScale",
    )

    // The leading slot is measured so the floating label can clear it no matter
    // how wide the slot is: the standard fields need 48dp (left-4 + 22dp glyph +
    // 10dp), while the phone field has to clear the country-code pill as well.
    // The measurement is seeded with the plain glyph width so the three regular
    // fields land on 48dp without a one-frame correction.
    val density = LocalDensity.current
    var leadingWidthPx by remember {
        mutableStateOf(with(density) { LeadingIconSize.roundToPx() })
    }
    val labelInset = if (leadingIcon != null) {
        with(density) { LeadingStartInset + leadingWidthPx.toDp() + LeadingGap }
    } else {
        LeadingStartInset
    }

    Column(modifier = modifier.fillMaxWidth()) {
        FieldPlane(
            focused = focused,
            isError = isError,
            shape = shape,
            height = FieldHeight, // h-14
            restContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onFocusChange = onFocusChange,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = LeadingStartInset) // left-4
                            .onSizeChanged { leadingWidthPx = it.width },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        leadingIcon()
                    }
                    Spacer(Modifier.width(LeadingGap))
                }

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
                            .padding(end = textEndPadding)
                            // pt-4 pb-1
                            .padding(top = 16.dp, bottom = 4.dp),
                        enabled = true,
                        readOnly = false,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isError) error else primary),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                    )

                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = labelColor,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = labelInset)
                            .offset(y = labelOffset)
                            .graphicsLayer {
                                scaleX = labelScale
                                scaleY = labelScale
                                // The mock scales from the label's own origin.
                                transformOrigin = TransformOrigin(0f, 0f)
                            },
                    )
                }

                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.padding(end = trailingEndPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        trailingIcon()
                    }
                }
            }
        }

        when {
            helper != null -> FieldHelperLine(
                text = helper,
                color = when {
                    isError -> error
                    helperIsPrimary -> primary
                    else -> onSurfaceVariant
                },
                modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
                leadingContent = if (helperIsPrimary && !isError) {
                    { FieldLeadingIcon(OneRouteIcons.Info, size = 14.dp, tint = primary) }
                } else {
                    null
                },
            )

            // Reserve the helper line so the fields below never shift as soon
            // as an error appears.
            reserveHelperSpace -> Spacer(Modifier.height(6.dp + 16.dp))
        }
    }
}

private val FieldHeight = 56.dp

/** Inset of the leading glyph from the field's left edge (`left-4`). */
private val LeadingStartInset = 16.dp

/** Width of the plain 22dp leading glyph. */
private val LeadingIconSize = 22.dp

/** Gap between the leading glyph and the text column. */
private val LeadingGap = 10.dp

/** `bg-surface-container py-1 px-2 rounded-full` country-selector trigger. */
@Composable
private fun CountryCodePill(
    flag: String,
    code: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp) // px-2 py-1
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
    ) {
        Text(text = flag, style = MaterialTheme.typography.labelSmall)
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = OneRouteIcons.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** 40dp circular trailing control that flips password visibility. */
@Composable
private fun PasswordVisibilityToggle(
    isVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(40.dp), // w-10 h-10
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            imageVector = if (isVisible) {
                OneRouteIcons.Visibility
            } else {
                OneRouteIcons.VisibilityOff
            },
            contentDescription = stringResource(
                if (isVisible) R.string.register_hide_password
                else R.string.register_show_password,
            ),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Four-segment strength meter plus the satisfied-requirement bullet. */
@Composable
private fun PasswordStrengthMeter(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp), // px-3
        verticalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
        ) {
            repeat(StrengthSegmentCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp) // h-1
                        .clip(CircleShape)
                        .background(
                            if (index < strength.filledSegments) {
                                primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                )
            }
            if (strength != PasswordStrength.Empty) {
                Spacer(Modifier.width(4.dp)) // ml-1
                Text(
                    text = stringResource(passwordStrengthLabel(strength)),
                    style = MaterialTheme.typography.labelSmall,
                    color = primary,
                )
            }
        }

        if (strength >= PasswordStrength.Strong) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
            ) {
                Icon(
                    imageVector = OneRouteIcons.Done,
                    contentDescription = stringResource(R.string.register_password_rule_met),
                    tint = primary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stringResource(R.string.register_password_helper),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Consent checkboxes
// ---------------------------------------------------------------------------

/** 24dp rounded checkbox plus its label, tappable as a whole row. */
@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.Top, // items-start
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp) // mt-0.5
                .size(24.dp) // w-6 h-6
                .clip(RoundedCornerShape(OneRouteRadius.Checkbox)) // rounded-[6px]
                .background(
                    if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = OneRouteIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp)) // gap-3

        Box(modifier = Modifier.weight(1f)) {
            label()
        }
    }
}

/**
 * "Acepto los Términos y Condiciones y la Política de Privacidad." with both
 * documents as real links: focusable, and announced as links by TalkBack.
 */
@Composable
private fun TermsLabel(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val titleMedium = MaterialTheme.typography.titleMedium

    // The mock renders both documents in `font-title-md font-semibold` +
    // underline, even though the surrounding copy is `body-md`.
    val linkStyle = SpanStyle(
        color = primary,
        fontSize = titleMedium.fontSize,
        letterSpacing = titleMedium.letterSpacing,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
    )

    val prefix = stringResource(R.string.register_terms_prefix)
    val termsLink = stringResource(R.string.register_terms_link)
    val middle = stringResource(R.string.register_terms_middle)
    val privacyLink = stringResource(R.string.register_privacy_link)
    val suffix = stringResource(R.string.register_terms_suffix)

    val text = buildAnnotatedString {
        append(prefix)
        withLink(
            LinkAnnotation.Clickable(
                tag = TermsLinkTag,
                styles = TextLinkStyles(linkStyle),
                linkInteractionListener = LinkInteractionListener { onOpenTerms() },
            ),
        ) {
            withStyle(linkStyle) { append(termsLink) }
        }
        append(middle)
        withLink(
            LinkAnnotation.Clickable(
                tag = PrivacyLinkTag,
                styles = TextLinkStyles(linkStyle),
                linkInteractionListener = LinkInteractionListener { onOpenPrivacy() },
            ),
        ) {
            withStyle(linkStyle) { append(privacyLink) }
        }
        append(suffix)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private const val TermsLinkTag = "register.terms"
private const val PrivacyLinkTag = "register.privacy"

// ---------------------------------------------------------------------------
// Primary action
// ---------------------------------------------------------------------------

/**
 * Full-width 56dp full-pill button carrying the three states of the mock's
 * `submit-signup` micro-interaction: "Crear cuenta" → "Validando…" → "¡Cuenta
 * Lista!".
 */
@Composable
private fun RegisterSubmitButton(
    submitState: RegisterSubmitState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSubmitting = submitState == RegisterSubmitState.Submitting

    val containerColor by animateColorAsState(
        targetValue = if (isSubmitting) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "submitContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSubmitting) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        label = "submitContentColor",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.99f else 1f, // active:scale-[0.99]
        label = "submitScale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // h-14
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
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
        when (submitState) {
            RegisterSubmitState.Idle -> SubmitButtonLabel(
                icon = OneRouteIcons.PersonAdd,
                text = stringResource(R.string.register_submit),
            )

            RegisterSubmitState.Submitting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(OneRouteSpacing.SpaceXs)) // gap-space-xs
                Text(
                    text = stringResource(R.string.register_submitting).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            RegisterSubmitState.Success -> SubmitButtonLabel(
                icon = OneRouteIcons.Check,
                text = stringResource(R.string.register_success),
            )
        }
    }
}

@Composable
private fun SubmitButtonLabel(icon: ImageVector, text: String) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(OneRouteSpacing.SpaceXs)) // gap-space-xs
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

// ---------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------

/** "¿Ya tienes una cuenta?  Inicia sesión" */
@Composable
private fun LoginRedirection(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = stringResource(R.string.register_login_action)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center, // justify-center
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.register_login_question),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp)) // gap-1.5
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(
                role = Role.Button,
                onClickLabel = actionLabel,
                onClick = onNavigateToLogin,
            ),
        )
    }
}

/** "Validación de identidad" assurance strip under the form. */
@Composable
private fun TrustBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneRouteRadius.Sheet)) // rounded-xl
            // bg-secondary-container/40
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            .padding(OneRouteSpacing.SpaceMd), // p-space-md
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp) // w-10 h-10
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = OneRouteIcons.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(OneRouteSpacing.SpaceMd)) // gap-space-md

        Column {
            Text(
                text = stringResource(R.string.register_trust_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.register_trust_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Register · Empty", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RegisterScreenEmptyPreview() {
    OneRouteAndroidTheme {
        RegisterScreen()
    }
}

@Preview(
    name = "Register · Filled (mock)",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun RegisterScreenFilledPreview() {
    OneRouteAndroidTheme {
        RegisterScreenContent(state = PreviewFilledState)
    }
}

@Preview(
    name = "Register · Validation errors",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun RegisterScreenErrorsPreview() {
    OneRouteAndroidTheme {
        RegisterScreenContent(
            state = PreviewFilledState.copy(
                email = "sofia.morales",
                password = "corta",
                termsAccepted = false,
                emailError = "Ingresa un correo electrónico válido",
                passwordError = "Mínimo 8 caracteres, incluyendo letras y números",
                termsError = "Debes aceptar los Términos y la Política de Privacidad",
            ),
        )
    }
}

@Preview(
    name = "Register · Password revealed",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun RegisterScreenPasswordRevealedPreview() {
    OneRouteAndroidTheme {
        RegisterScreenContent(
            state = PreviewFilledState.copy(isPasswordVisible = true),
        )
    }
}

/** The exact values the mock ships with, handy for design review and previews. */
private val PreviewFilledState = RegisterUiState(
    fullName = "Sofía Morales Valdez",
    email = "sofia.morales@ejemplo.com",
    phone = "55 4892 1047",
    password = "ViajeSeguro2025",
    termsAccepted = true,
)
