package ly.img.editor.guides

import CustomPanelSolution
import ForceTrimVideoSolution
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class GuidesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuidesApp()
        }
    }
}

@Composable
private fun GuidesApp() {
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                GuidesHome(
                    onOpenDockDemo = { navController.navigate("dock-demo") },
                    onOpenForceTrimDemo = { navController.navigate("force-trim") },
                )
            }
            composable("dock-demo") {
                CustomPanelSolution(navController = navController)
            }
            composable("force-trim") {
                ForceTrimVideoSolution(navController = navController)
            }
        }
    }
}

@Composable
private fun GuidesHome(
    onOpenDockDemo: () -> Unit,
    onOpenForceTrimDemo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "CE.SDK Guides",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Pick a demo to preview the guide examples.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenDockDemo,
        ) {
            Text("Dock Button Demo")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenForceTrimDemo,
        ) {
            Text("Force Trim Demo")
        }
    }
}
