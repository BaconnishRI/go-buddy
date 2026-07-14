package com.baconnish.gobuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baconnish.gobuddy.service.OverlayScanService
import com.baconnish.gobuddy.ui.detail.DetailScreen
import com.baconnish.gobuddy.ui.edit.EditScreen
import com.baconnish.gobuddy.ui.home.HomeScreen
import com.baconnish.gobuddy.ui.imports.ImportScreen
import com.baconnish.gobuddy.ui.profile.ProfileScreen
import com.baconnish.gobuddy.ui.theme.GoBuddyTheme

class MainActivity : ComponentActivity() {

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            OverlayScanService.start(this, result.resultCode, data)
            Toast.makeText(
                this,
                "Overlay on; switch to Pokémon GO and tap the bubble to scan.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        launchProjectionRequest()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedImages = extractSharedImages(intent)
        setContent {
            GoBuddyTheme {
                GoBuddyNavHost(
                    sharedImages = sharedImages,
                    onSharedImportDone = { finish() },
                    onOverlayToggle = { toggleOverlay() },
                )
            }
        }
    }

    private fun toggleOverlay() {
        if (OverlayScanService.isRunning) {
            OverlayScanService.stop(this)
            Toast.makeText(this, "Overlay stopped.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Allow \"Display over other apps\" for Go Buddy, then tap the overlay button again.",
                Toast.LENGTH_LONG,
            ).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri(),
                ),
            )
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        launchProjectionRequest()
    }

    private fun launchProjectionRequest() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun extractSharedImages(intent: Intent?): List<Uri> = when (intent?.action) {
        Intent.ACTION_SEND ->
            listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                .orEmpty()
        else -> emptyList()
    }
}

@Composable
fun GoBuddyNavHost(
    modifier: Modifier = Modifier,
    sharedImages: List<Uri> = emptyList(),
    onSharedImportDone: () -> Unit = {},
    onOverlayToggle: () -> Unit = {},
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = if (sharedImages.isEmpty()) "home" else "import",
        modifier = modifier,
    ) {
        composable("home") {
            HomeScreen(
                onAddClick = { navController.navigate("edit/-1") },
                onPokemonClick = { id -> navController.navigate("detail/$id") },
                onOverlayClick = onOverlayToggle,
                onProfileClick = { navController.navigate("profile") },
            )
        }
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "edit/{pokemonId}",
            arguments = listOf(navArgument("pokemonId") { type = NavType.LongType }),
        ) {
            EditScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = "detail/{pokemonId}",
            arguments = listOf(navArgument("pokemonId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("pokemonId") ?: -1L
            DetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("edit/$id") },
            )
        }
        composable("import") {
            ImportScreen(
                uris = sharedImages,
                onDone = onSharedImportDone,
            )
        }
    }
}
