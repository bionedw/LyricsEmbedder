package com.lyricsembedder.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lyricsembedder.app.ui.components.AboutDialog
import com.lyricsembedder.app.ui.components.AppHeader
import com.lyricsembedder.app.ui.lyrics.LyricsEditorScreen
import com.lyricsembedder.app.ui.lyrics.LyricsTabScreen
import com.lyricsembedder.app.ui.lyrics.LyricsViewModel
import com.lyricsembedder.app.ui.metadata.MetadataTabScreen
import com.lyricsembedder.app.ui.metadata.MetadataViewModel
import com.lyricsembedder.app.ui.navigation.NavRoutes
import com.lyricsembedder.app.ui.settings.FilterSettingsScreen
import com.lyricsembedder.app.ui.settings.LyricsEditorSettingsScreen
import com.lyricsembedder.app.ui.settings.LyricsProviderSettingsScreen
import com.lyricsembedder.app.ui.settings.LyricsRequestSettingsScreen
import com.lyricsembedder.app.ui.settings.LyricsStorageSettingsScreen
import com.lyricsembedder.app.ui.settings.SettingsScreen
import com.lyricsembedder.app.ui.settings.SettingsViewModel
import com.lyricsembedder.app.ui.settings.ThemeSettingsScreen
import com.lyricsembedder.app.ui.theme.LyricsEmbedderTheme
import com.lyricsembedder.app.util.LocaleHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled gracefully via SAF & runtime checks */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()

        val container = (application as LyricsEmbedderApp).container

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(container))
            val metadataViewModel: MetadataViewModel = viewModel(factory = MetadataViewModel.Factory(container))
            val lyricsViewModel: LyricsViewModel = viewModel(factory = LyricsViewModel.Factory(container))

            val settings by settingsViewModel.settings.collectAsState()
            val baseContext = LocalContext.current
            val localizedContext = remember(baseContext, settings.appLanguage) {
                LocaleHelper.createLocalizedContext(baseContext, settings.appLanguage)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                LyricsEmbedderTheme(
                    themeMode = settings.themeMode,
                    dynamicColor = settings.dynamicColor
                ) {
                    LyricsEmbedderRoot(
                        isExpandedScreen = isExpandedScreen,
                        settingsViewModel = settingsViewModel,
                        metadataViewModel = metadataViewModel,
                        lyricsViewModel = lyricsViewModel
                    )
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }
}

@Composable
private fun LyricsEmbedderRoot(
    isExpandedScreen: Boolean,
    settingsViewModel: SettingsViewModel,
    metadataViewModel: MetadataViewModel,
    lyricsViewModel: LyricsViewModel
) {
    val navController = rememberNavController()
    val settings by settingsViewModel.settings.collectAsState()
    val metadataState by metadataViewModel.uiState.collectAsState()
    val lyricsState by lyricsViewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    BackHandler(enabled = pagerState.currentPage != 0 && navController.currentDestination?.route == NavRoutes.HOME) {
        coroutineScope.launch { pagerState.animateScrollToPage(0) }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260))
        }
    ) {
        composable(NavRoutes.HOME) {
            Scaffold(
                topBar = {
                    AppHeader(
                        onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                        onOpenAbout = { showAboutDialog = true }
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = pagerState.currentPage == 0,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                },
                                icon = { Icon(Icons.Filled.FolderCopy, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_metadata)) }
                            )
                            NavigationBarItem(
                                selected = pagerState.currentPage == 1,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                },
                                icon = { Icon(Icons.Filled.Lyrics, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_lyrics)) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail {
                            NavigationRailItem(
                                selected = pagerState.currentPage == 0,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                },
                                icon = { Icon(Icons.Filled.FolderCopy, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_metadata)) }
                            )
                            NavigationRailItem(
                                selected = pagerState.currentPage == 1,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                },
                                icon = { Icon(Icons.Filled.Lyrics, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_lyrics)) }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> MetadataTabScreen(
                                state = metadataState,
                                isExpandedScreen = isExpandedScreen,
                                viewModel = metadataViewModel
                            )
                            else -> LyricsTabScreen(
                                state = lyricsState,
                                viewModel = lyricsViewModel,
                                onOpenEditor = { track ->
                                    lyricsViewModel.openLyricsEditor(track)
                                    navController.navigate(NavRoutes.LYRICS_EDITOR)
                                }
                            )
                        }
                    }
                }
            }
        }

        composable(NavRoutes.LYRICS_EDITOR) {
            LyricsEditorScreen(
                state = lyricsState,
                timestampStepMs = settings.timestampStepMs,
                viewModel = lyricsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateTheme = { navController.navigate(NavRoutes.SETTINGS_THEME) },
                onNavigateLyricsRequest = { navController.navigate(NavRoutes.SETTINGS_LYRICS_REQUEST) },
                onNavigateLyricsStorage = { navController.navigate(NavRoutes.SETTINGS_LYRICS_STORAGE) },
                onNavigateLyricsProvider = { navController.navigate(NavRoutes.SETTINGS_LYRICS_PROVIDER) },
                onNavigateLyricsEditor = { navController.navigate(NavRoutes.SETTINGS_LYRICS_EDITOR) },
                onNavigateFilters = { navController.navigate(NavRoutes.SETTINGS_FILTERS) }
            )
        }

        composable(NavRoutes.SETTINGS_THEME) {
            ThemeSettingsScreen(
                settings = settings,
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS_LYRICS_REQUEST) {
            LyricsRequestSettingsScreen(
                settings = settings,
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS_LYRICS_STORAGE) {
            LyricsStorageSettingsScreen(
                settings = settings,
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS_LYRICS_PROVIDER) {
            LyricsProviderSettingsScreen(
                settings = settings,
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS_LYRICS_EDITOR) {
            LyricsEditorSettingsScreen(
                settings = settings,
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS_FILTERS) {
            FilterSettingsScreen(
                settings = settings,
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
