package fr.julie.mariosprint.ui

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.architecture.SprintApplication
import fr.julie.mariosprint.domain.game.Phase
import fr.julie.mariosprint.game.GameView
import fr.julie.mariosprint.ui.auth.GoogleSignInCoordinator
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.navigation.Destination
import fr.julie.mariosprint.ui.screens.*
import fr.julie.mariosprint.ui.theme.SprintTheme
import fr.julie.mariosprint.ui.viewmodel.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SprintApp() {
    val activity = checkNotNull(LocalActivity.current)
    val app = LocalContext.current.applicationContext as SprintApplication
    val container = remember(app) { app.container }
    val accountVm: AccountViewModel = viewModel(factory = remember(container) {
        factory { AccountViewModel(container.accounts, container.records, container.sync, container.progress) }
    })
    val profile by accountVm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val google = remember(container) { GoogleSignInCoordinator(app, container.webClientId) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var artReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { fr.julie.mariosprint.game.rendering.GameArt.heroFrames.size }
        artReady = true
    }
    LaunchedEffect(accountVm, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            accountVm.events.collect { event ->
                when (event) {
                    AccountEvent.RequestGoogleSignIn -> launch {
                        google.signIn(activity, { token, nonce -> accountVm.onAction(AccountAction.TokenReceived(token, nonce)) },
                            { accountVm.onAction(AccountAction.SignInFailed(it)) })
                    }
                    AccountEvent.ClearGoogleSession -> launch {
                        try { google.clearSession() }
                        catch (error: CancellationException) { throw error }
                        catch (_: Exception) { /* La session Supabase est déjà supprimée localement. */ }
                        finally { accountVm.onAction(AccountAction.SessionCleared) }
                    }
                    is AccountEvent.ShowMessage -> scope.launch {
                        snackbar.showSnackbar(app.getString(event.text.resource, *event.text.args.toTypedArray()))
                    }
                }
            }
        }
    }
    SprintTheme {
        val navigation = rememberNavController()
        Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) }) { insets ->
            if (!artReady) Box(Modifier.fillMaxSize().padding(insets), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            else NavHost(navigation, startDestination = Destination.Home,
                modifier = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                composable<Destination.Home> {
                    val vm: HomeViewModel = viewModel(factory = remember(container) { factory { HomeViewModel(container.records) } })
                    val state by vm.state.collectAsStateWithLifecycle()
                    HomeScreen(state.copy(profile = profile)) { action ->
                        when(action) {
                            is HomeAction.SelectLevel -> vm.selectLevel(action.level)
                            HomeAction.Play -> navigation.navigate(Destination.Game(state.selectedLevel.value))
                            HomeAction.Leaderboard -> navigation.navigate(Destination.Leaderboard(state.selectedLevel.value))
                            HomeAction.Collection -> navigation.navigate(Destination.Collection)
                            HomeAction.Profile -> navigation.navigate(Destination.Profile)
                            HomeAction.Help -> navigation.navigate(Destination.Help)
                            is HomeAction.Account -> accountVm.onAction(action.action)
                        }
                    }
                }
                composable<Destination.Profile> {
                    ProfileScreen(profile) { action ->
                        when (action) {
                            ProfileAction.Back -> navigation.popBackStack()
                            is ProfileAction.Account -> accountVm.onAction(action.action)
                        }
                    }
                }
                composable<Destination.Help> {
                    HelpScreen { action ->
                        if (action == HelpAction.Back) navigation.popBackStack()
                    }
                }
                composable<Destination.Collection> {
                    val vm: CollectionViewModel = viewModel(factory = remember(container) {
                        factory { CollectionViewModel(container.accounts, container.progress, container.sync) }
                    })
                    val state by vm.state.collectAsStateWithLifecycle()
                    CollectionScreen(state) { action ->
                        if (action == CollectionAction.Back) navigation.popBackStack() else vm.onAction(action)
                    }
                }
                composable<Destination.Leaderboard> { entry ->
                    val level = LevelId.from(entry.toRoute<Destination.Leaderboard>().levelId)
                    val vm: LeaderboardViewModel = viewModel(factory = remember(container, level) {
                        factory { LeaderboardViewModel(container.accounts, container.ranking, container.records, level) }
                    })
                    val state by vm.state.collectAsStateWithLifecycle()
                    LeaderboardScreen(state.copy(profile = profile)) { action ->
                        when(action) {
                            LeaderboardAction.Back -> navigation.popBackStack()
                            is LeaderboardAction.Account -> accountVm.onAction(action.action)
                            LeaderboardAction.Refresh -> vm.onAction(action)
                        }
                    }
                }
                composable<Destination.Game> { entry ->
                    val level = LevelId.from(entry.toRoute<Destination.Game>().levelId)
                    val vm: GameViewModel = viewModel(factory = remember(container, level) {
                        factory { GameViewModel(container.records, container.accounts, container.sync, level) }
                    })
                    val state by vm.state.collectAsStateWithLifecycle()
                    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.onAction(GameAction.Pause) }
                    DisposableEffect(activity) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                    }
                    BackHandler {
                        if (state.phase == Phase.RUNNING || state.phase == Phase.READY) vm.onAction(GameAction.Pause)
                        else { vm.onAction(GameAction.Quit); navigation.popBackStack() }
                    }
                    val sound = rememberUpdatedState(profile.sound)
                    GameScreen(state.copy(profile = profile), onAction = { action ->
                        vm.onAction(action)
                        when(action) {
                            GameAction.Quit -> navigation.popBackStack()
                            GameAction.Leaderboard -> navigation.navigate(Destination.Leaderboard(level.value)) {
                                popUpTo<Destination.Game> { inclusive = true }
                            }
                            else -> Unit
                        }
                    }) {
                        key(state.sessionId) {
                            AndroidView(factory = { GameView(it, vm.engine, soundEnabled = { sound.value }, skin = profile.skin,
                                onUpdate = { vm.onAction(GameAction.Frame) }) },
                                update = { it.setSkin(profile.skin); it.refreshFrameScheduling(state.phase) }, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

private fun <VM : ViewModel> factory(create: () -> VM): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
