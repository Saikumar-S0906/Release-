package com.pixijoy.studio.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixijoy.studio.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiText

/**
 * Inline numeric entry for [StudioValueSlider].
 * Commits on IME Done and when focus leaves. A decimal IME often has no minus key,
 * so ranges below zero get a sign toggle that does not take focus.
 */
@Composable
internal fun SliderValueField(
    initialText: String,
    allowNegative: Boolean,
    onCommit: (String) -> Unit
) {
    val signLabel = stringResource(R.string.a11y_slider_sign)
    var field by remember {
        mutableStateOf(TextFieldValue(initialText, TextRange(0, initialText.length)))
    }
    var armed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
        // The tap that opened the field can clear focus in this frame. Arm after that settles.
        withFrameNanos { }
        withFrameNanos { }
        withFrameNanos { }
        withFrameNanos { }
        kotlinx.coroutines.delay(120)
        focusRequester.requestFocus()
        keyboard?.show()
        armed = true
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (allowNegative) {
            SliderSignToggle(
                negative = field.text.startsWith("-"),
                label = signLabel,
                onFlip = {
                    val flipped = toggleSliderSign(field.text)
                    field = TextFieldValue(flipped, TextRange(flipped.length))
                }
            )
        }
        BasicTextField(
            value = field,
            onValueChange = { next ->
                if (isPartialSliderInput(next.text, allowNegative)) field = next
            },
            singleLine = true,
            textStyle = TextStyle(
                color = PixiText,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onCommit(field.text) }),
            cursorBrush = SolidColor(PixiAccent),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (armed && !state.isFocused) onCommit(field.text)
                }
                .testTag("session_slider_value_field")
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val y = size.height - stroke / 2f
                    drawLine(
                        color = PixiAccent,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = stroke
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SliderSignToggle(
    negative: Boolean,
    label: String,
    onFlip: () -> Unit
) {
    Box(
        modifier = Modifier
            .widthIn(min = 48.dp)
            .heightIn(min = 48.dp)
            .focusProperties { canFocus = false }
            .clickable(onClick = onFlip)
            .testTag("session_slider_value_sign")
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (negative) "+" else "−",
            color = PixiAccent,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
