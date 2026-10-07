package com.onerouteandroid.oneroute.auth

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onerouteandroid.oneroute.R
import com.onerouteandroid.oneroute.ui.components.FieldHelperLine
import com.onerouteandroid.oneroute.ui.components.FieldLeadingIcon
import com.onerouteandroid.oneroute.ui.components.FieldPlane
import com.onerouteandroid.oneroute.ui.icons.OneRouteIcons
import com.onerouteandroid.oneroute.ui.theme.OneRouteAndroidTheme
import com.onerouteandroid.oneroute.ui.theme.OneRouteElevation
import com.onerouteandroid.oneroute.ui.theme.OneRouteRadius
import com.onerouteandroid.oneroute.ui.theme.OneRouteSpacing
import com.onerouteandroid.oneroute.ui.theme.OneRouteType
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// State model
// ---------------------------------------------------------------------------

/** Identifies a field so focus can be reported as part of [SignInUiState]. */
enum class SignInField { Identifier, Password }

/** Lifecycle of the primary action. */
enum class SignInSubmitState { Idle, Submitting, Success }

private const val MinPhoneDigits = 10
private val EmailRegex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

/** Everything [SignInScreenContent] needs in order to render one frame. */
@Immutable
data class SignInUiState(
    // --- text fields ---
    /** The account identifier. The mock labels the field "Correo electrónico o teléfono", so [isIdentifierValid] accepts either form. */
    val email: String = "",
    val password: String = "",

    // --- flags ---
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val focusedField: SignInField? = null,

    // --- validation feedback, rendered under the field that failed ---
    val emailError: String? = null,
    val passwordError: String? = null,

    // --- primary action ---
    val submitState: SignInSubmitState = SignInSubmitState.Idle,
) {
    /** True when the identifier is either a well-formed e-mail address or a phone number with at least [MinPhoneDigits] digits. */
    val isIdentifierValid: Boolean
        get() {
            val trimmed = email.trim()
            if (trimmed.isEmpty()) return false
            return EmailRegex.matches(trimmed) || trimmed.count(Char::isDigit) >= MinPhoneDigits
        }

    /** The mock reveals the "clear" affordance only while the field has text. */
    val canClearIdentifier: Boolean
        get() = email.isNotEmpty()

    /** True once both fields are valid. */
    val canSubmit: Boolean
        get() = isIdentifierValid && password.isNotEmpty()

    /** True while the primary action must ignore further taps. */
    val isLoading: Boolean
        get() = submitState == SignInSubmitState.Submitting
}

/** Resolved copy for the validation messages, so validation stays pure. */
@Immutable
private data class LoginErrorCopy(
    val identifier: String,
    val password: String,
)

/** Returns the state with field errors populated, plus whether any of them fired. Pure: no `Context`, no coroutines, directly unit-testable. */
private fun validateLogin(
    state: SignInUiState,
    copy: LoginErrorCopy,
): Pair<SignInUiState, Boolean> {
    val emailError = copy.identifier.takeIf { !state.isIdentifierValid }
    val passwordError = copy.password.takeIf { state.password.isEmpty() }
    val hasErrors = emailError != null || passwordError != null

    return state.copy(
        emailError = emailError,
        passwordError = passwordError,
    ) to hasErrors
}

// ---------------------------------------------------------------------------
// Container
// ---------------------------------------------------------------------------

/** Stateful entry point for the login screen. */
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    onNavigateToRegister: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onBiometricSignIn: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
) {
    var state by remember { mutableStateOf(SignInUiState()) }

    val errorCopy = LoginErrorCopy(
        identifier = stringResource(R.string.login_error_identifier),
        password = stringResource(R.string.login_error_password),
    )

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.submitState) {
        if (state.submitState == SignInSubmitState.Submitting) {
            delay(SubmitIndicatorDurationMillis)
            state = state.copy(submitState = SignInSubmitState.Success)
        }
    }

    SignInScreenContent(
        state = state,
        modifier = modifier,
        onEmailChange = { value -> state = state.copy(email = value, emailError = null) },
        onPasswordChange = { value -> state = state.copy(password = value, passwordError = null) },
        onTogglePasswordVisibility = {
            state = state.copy(isPasswordVisible = !state.isPasswordVisible)
        },
        onClearEmail = { state = state.copy(email = "", emailError = null) },
        onRememberMeChange = { accepted -> state = state.copy(rememberMe = accepted) },
        onFieldFocusChange = { field, focused ->
            state = state.copy(focusedField = if (focused) field else null)
        },
        onMoveFocusDown = { focusManager.moveFocus(FocusDirection.Down) },
        onKeyboardDone = {
            keyboard?.hide()
            focusManager.clearFocus()
        },
        onLoginClick = {
            if (state.isLoading) return@SignInScreenContent
            val (validated, hasErrors) = validateLogin(state, errorCopy)
            state = if (hasErrors) {
                validated
            } else {
                state.copy(submitState = SignInSubmitState.Submitting)
            }
        },
        onForgotPasswordClick = onForgotPassword,
        onBiometricClick = onBiometricSignIn,
        onGoogleSignInClick = onGoogleSignIn,
        onNavigateToRegister = onNavigateToRegister,
        onNavigateBack = onNavigateBack,
    )
}

/** How long the "Ingresando…" state stays on screen before success is shown. */
private const val SubmitIndicatorDurationMillis = 900L

// ---------------------------------------------------------------------------
// Stateless content
// ---------------------------------------------------------------------------

/** Stateless, fully hoisted login screen. */
@Composable
fun SignInScreenContent(
    state: SignInUiState,
    modifier: Modifier = Modifier,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onClearEmail: () -> Unit = {},
    onRememberMeChange: (Boolean) -> Unit = {},
    onFieldFocusChange: (SignInField, Boolean) -> Unit = { _, _ -> },
    onMoveFocusDown: () -> Unit = {},
    onKeyboardDone: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onBiometricClick: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LoginTopBar(
                title = stringResource(R.string.login_top_bar_title),
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
                .padding(bottom = 48.dp + OneRouteSpacing.SpaceXl),
        ) {
            CommunityBadge(
                modifier = Modifier.padding(
                    top = OneRouteSpacing.SpaceSm,
                    bottom = OneRouteSpacing.SpaceMd,
                ),
            )
            Text(
                text = stringResource(R.string.login_welcome_title),
                style = OneRouteType.HeadlineLargeMobile.copy(
                    letterSpacing = (-0.7).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(OneRouteSpacing.SpaceXs))
            Text(
                text = stringResource(R.string.login_welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceLg))
            LoginIdentifierField(
                value = state.email,
                onValueChange = onEmailChange,
                onClear = onClearEmail,
                canClear = state.canClearIdentifier,
                isError = state.emailError != null,
                errorText = state.emailError,
                focused = state.focusedField == SignInField.Identifier,
                onFocusChange = { onFieldFocusChange(SignInField.Identifier, it) },
                onMoveFocusDown = onMoveFocusDown,
            )

            Spacer(Modifier.height(OneRouteSpacing.SpaceMd))

            LoginPasswordField(
                value = state.password,
                onValueChange = onPasswordChange,
                isPasswordVisible = state.isPasswordVisible,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                isError = state.passwordError != null,
                errorText = state.passwordError,
                focused = state.focusedField == SignInField.Password,
                onFocusChange = { onFieldFocusChange(SignInField.Password, it) },
                onKeyboardDone = onKeyboardDone,
                onLoginClick = onLoginClick,
            )

            // The controls sit inside the form, so they clear the last field by

            Spacer(Modifier.height(OneRouteSpacing.SpaceMd + OneRouteSpacing.SpaceXs))

            // -- "Recordarme" + "¿Olvidaste tu contraseña?" -------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RememberMeCheckbox(
                    checked = state.rememberMe,
                    onCheckedChange = onRememberMeChange,
                )
                ForgotPasswordButton(onClick = onForgotPasswordClick)
            }
            LoginSubmitButton(
                submitState = state.submitState,
                enabled = !state.isLoading,
                onClick = onLoginClick,
                modifier = Modifier.padding(
                    top = OneRouteSpacing.SpaceMd + OneRouteSpacing.SpaceSm,
                ),
            )
            BiometricButton(
                onClick = onBiometricClick,
                modifier = Modifier.padding(top = OneRouteSpacing.SpaceLg),
            )
            OrSeparator(
                modifier = Modifier.padding(vertical = OneRouteSpacing.SpaceLg),
            )

            GoogleSignInButton(onClick = onGoogleSignInClick)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = OneRouteSpacing.SpaceXl),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.login_register_question),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                val actionLabel = stringResource(R.string.login_register_action)
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = actionLabel,
                        onClick = onNavigateToRegister,
                    ),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top app bar
// ---------------------------------------------------------------------------

@Composable
private fun LoginTopBar(
    title: String,
    backContentDescription: String,
    profileContentDescription: String,
    brandContentDescription: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        // `bg-surface/85 backdrop-blur-xl`
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = OneRouteElevation.Resting,
    ) {
        Column {
            // Native status-bar strip drawn by the platform.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = OneRouteSpacing.SpaceSm)
                    .consumeWindowInsets(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(44.dp),
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

/** Primary disc with a route glyph, standing in for the remote brand mark. */
@Composable
private fun BrandMark(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
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
            .size(32.dp)
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
// Ambient badge
// ---------------------------------------------------------------------------

/** `Comunidad de Viajeros` pill that opens the screen. */
@Composable
private fun CommunityBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(
                horizontal = OneRouteSpacing.SpaceMd,
                vertical = OneRouteSpacing.SpaceXs,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OneRouteSpacing.SpaceXs),
    ) {
        Icon(
            imageVector = OneRouteIcons.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.login_badge).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.35.sp,
            ),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// ---------------------------------------------------------------------------
// Fields
// ---------------------------------------------------------------------------

/** Height implied by the mock's `py-3.5` plus a `body-lg` line box. */
private val SignInFieldHeight = 52.dp

/** `rounded-lg` on the mock's field containers. */
private val SignInFieldShape = RoundedCornerShape(OneRouteRadius.Card)

/** External caption shared by both login fields: `label-sm`, 12dp in from the field edge, set in `on-surface-variant`. */
@Composable
private fun SignInFieldCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp),
    )
}

@Composable
private fun LoginIdentifierField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    canClear: Boolean,
    isError: Boolean,
    errorText: String?,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onMoveFocusDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = SignInFieldShape
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    Column(modifier = modifier.fillMaxWidth()) {
        SignInFieldCaption(stringResource(R.string.login_identifier_label))
        Spacer(Modifier.height(OneRouteSpacing.SpaceXs))

        FieldPlane(
            focused = focused,
            isError = isError,
            shape = shape,
            height = SignInFieldHeight,
            // `bg-surface-container-low` → `focus-within:bg-surface-container-lowest`
            restContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onFocusChange = onFocusChange,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldLeadingIcon(
                    imageVector = OneRouteIcons.Mail,
                    tint = outline, // text-outline
                    modifier = Modifier.padding(start = 16.dp),
                )

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    enabled = true,
                    readOnly = false,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text, // the mock uses type="text"
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.login_identifier_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = outline,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                // The mock collapses this button when the field is empty, but
                // reserving the slot keeps the text from resizing as it appears.
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(26.dp), // p-1 + 18px glyph
                    contentAlignment = Alignment.Center,
                ) {
                    if (canClear) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(26.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = outline,
                            ),
                        ) {
                            Icon(
                                imageVector = OneRouteIcons.Cancel,
                                contentDescription = stringResource(
                                    R.string.login_clear_identifier,
                                ),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        if (errorText != null) {
            FieldHelperLine(
                text = errorText,
                color = error,
                modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
            )
        }
    }
}

@Composable
private fun LoginPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isError: Boolean,
    errorText: String?,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onKeyboardDone: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = SignInFieldShape
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    Column(modifier = modifier.fillMaxWidth()) {
        SignInFieldCaption(stringResource(R.string.login_password_label))
        Spacer(Modifier.height(OneRouteSpacing.SpaceXs))

        FieldPlane(
            focused = focused,
            isError = isError,
            shape = shape,
            height = SignInFieldHeight,
            restContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onFocusChange = onFocusChange,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldLeadingIcon(
                    imageVector = OneRouteIcons.Lock,
                    tint = outline, // text-outline
                    modifier = Modifier.padding(start = 16.dp),
                )

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .padding(end = 8.dp),
                    enabled = true,
                    readOnly = false,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(primary),
                    visualTransformation = if (isPasswordVisible) {
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
                            onLoginClick()
                        },
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.login_password_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = outline,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Box(
                    modifier = Modifier.padding(end = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        modifier = Modifier.size(38.dp), // p-2 + 22px glyph
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = outline,
                        ),
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                OneRouteIcons.Visibility
                            } else {
                                OneRouteIcons.VisibilityOff
                            },
                            contentDescription = stringResource(
                                if (isPasswordVisible) {
                                    R.string.register_hide_password
                                } else {
                                    R.string.register_show_password
                                },
                            ),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        if (errorText != null) {
            FieldHelperLine(
                text = errorText,
                color = error,
                modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

/** 20dp rounded "Recordarme" checkbox. */
@Composable
private fun RememberMeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.login_remember_me)

    Row(
        modifier = modifier
            .clip(CircleShape)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = OneRouteSpacing.SpaceXs)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OneRouteSpacing.SpaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(OneRouteRadius.Medium))
                .background(
                    if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = OneRouteIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** `label-lg` "primary" pill link: "¿Olvidaste tu contraseña?". */
@Composable
private fun ForgotPasswordButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.login_forgot_password)

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            )
            .padding(
                horizontal = OneRouteSpacing.SpaceSm,
                vertical = OneRouteSpacing.SpaceXs,
            ),
    )
}

/** Full-width 48dp full-pill primary action. */
@Composable
private fun LoginSubmitButton(
    submitState: SignInSubmitState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = when (submitState) {
            SignInSubmitState.Idle -> MaterialTheme.colorScheme.primaryContainer
            SignInSubmitState.Submitting,
            SignInSubmitState.Success,
            -> MaterialTheme.colorScheme.primary
        },
        label = "loginSubmitContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onPrimary,
        label = "loginSubmitContent",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        label = "loginSubmitScale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = OneRouteElevation.Active,
            pressedElevation = OneRouteElevation.Resting,
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = OneRouteSpacing.SpaceMd),
    ) {
        when (submitState) {
            SignInSubmitState.Idle -> {
                Text(
                    text = stringResource(R.string.login_submit),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.width(OneRouteSpacing.SpaceXs))
                Icon(
                    imageVector = OneRouteIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }

            SignInSubmitState.Submitting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(OneRouteSpacing.SpaceXs))
                Text(
                    text = stringResource(R.string.login_submitting).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            SignInSubmitState.Success -> {
                Icon(
                    imageVector = OneRouteIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(OneRouteSpacing.SpaceXs))
                Text(
                    text = stringResource(R.string.login_success).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

/** "Ingreso biométrico" chip. The mock squashes it to 95% for 180ms on tap, which is reproduced here with a press-driven scale. */
@Composable
private fun BiometricButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.login_biometric)
    val description = stringResource(R.string.login_biometric_description)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f, // scale-95
        animationSpec = tween(180),
        label = "biometricScale",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        label = "biometricContainer",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .padding(
                horizontal = OneRouteSpacing.SpaceLg,
                vertical = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OneRouteSpacing.SpaceXs),
    ) {
        Icon(
            imageVector = OneRouteIcons.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "— O ingresa con —" hairline separator. */
@Composable
private fun OrSeparator(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OneRouteSpacing.SpaceMd),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = stringResource(R.string.login_separator),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.35.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/** 48dp full-pill "Continuar con Google" button. */
@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.login_google)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "googleContainer",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = OneRouteIcons.GoogleLogo,
            contentDescription = null,
            // Unspecified keeps the mark's own four brand colours: a tint would
            // flatten them into a single colour filter.
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Login · Empty", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun SignInScreenEmptyPreview() {
    OneRouteAndroidTheme {
        SignInScreen()
    }
}

@Preview(
    name = "Login · Filled (mock)",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun SignInScreenFilledPreview() {
    OneRouteAndroidTheme {
        SignInScreenContent(state = SignInPreviewFilledState)
    }
}

@Preview(
    name = "Login · Password revealed",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun SignInScreenPasswordRevealedPreview() {
    OneRouteAndroidTheme {
        SignInScreenContent(
            state = SignInPreviewFilledState.copy(isPasswordVisible = true),
        )
    }
}

@Preview(
    name = "Login · Validation errors",
    showBackground = true,
    widthDp = 412,
    heightDp = 917,
)
@Composable
private fun SignInScreenErrorsPreview() {
    OneRouteAndroidTheme {
        SignInScreenContent(
            state = SignInUiState(
                email = "sofia.morales",
                passwordError = "Ingresa tu contraseña",
                emailError = "Ingresa un correo electrónico o un número de teléfono válido",
            ),
        )
    }
}

/** The exact values the mock ships with, handy for design review and previews. */
private val SignInPreviewFilledState = SignInUiState(
    email = "sofia.morales@ejemplo.com",
    password = "ViajeSeguro2025",
    rememberMe = true,
)
