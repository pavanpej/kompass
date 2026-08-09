package com.pavanpej.kompass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavanpej.kompass.ui.theme.KompassColors

/**
 * A minimal multi-option toggle: text labels only, no filled "pill" background. The selected
 * option is marked by accent color + a thin underline rather than a solid fill, so it reads as
 * subtle chrome indicating current state, not a prominent call-to-action button.
 */
@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            SegmentedToggleOption(
                text = label(option),
                isSelected = option == selected,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun SegmentedToggleOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    // width(IntrinsicSize.Min) forces a two-pass measurement so this Column's width is fixed to
    // its widest child (the Text) BEFORE children are placed. Without it, the underline Box's
    // fillMaxWidth() below measures against the Row's full incoming width (since Column doesn't
    // constrain children to its own resolved size), stretching one segment's underline across
    // nearly the whole toggle and squeezing the other segment's text into almost no space.
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) KompassColors.PrincetonOrange else KompassColors.OnSurfaceMuted,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isSelected) KompassColors.PrincetonOrange else Color.Transparent)
        )
    }
}
