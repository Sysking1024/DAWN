package world.accera.dawn.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import world.accera.dawn.DashedDivider
import world.accera.dawn.ticketBackground

@Composable
fun TicketContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    cornerRadius: Dp = 12.dp,
    notchRadius: Dp = 10.dp,
    showTopDivider: Boolean = false,
    showBottomDivider: Boolean = false,
    dividerColor: Color = Color.LightGray,
    dividerThickness: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .ticketBackground(
                backgroundColor = backgroundColor,
                cornerRadius = cornerRadius,
                notchRadius = notchRadius
            )
            .padding(16.dp)
    ) {
        if (showTopDivider) {
            DashedDivider(
                color = dividerColor,
                thickness = dividerThickness,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        content()
        if (showBottomDivider) {
            DashedDivider(
                color = dividerColor,
                thickness = dividerThickness,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}