package co.loopr.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.loopr.player.ui.theme.*

@Composable
fun PlayerScreen(vm: PlayerViewModel = viewModel()) {
    val identity by vm.identity.collectAsState(initial = null)
    val name = identity?.screenName ?: "this TV"
    val workspace = identity?.workspaceId?.let { "workspace #$it" } ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(LooprPanelLight, LooprBlack))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓", color = LooprEmerald, fontSize = 88.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Connected", color = LooprText, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                name,
                color = LooprTextDim,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            if (workspace.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(workspace, color = LooprTextMuted, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier
                    .background(LooprPanel, RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 18.dp)
            ) {
                Text(
                    "Waiting for content…",
                    color = LooprTextDim,
                    fontSize = 18.sp,
                )
            }
        }

        Text(
            "Loopr",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(40.dp),
            color = LooprTextMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
    }
}
