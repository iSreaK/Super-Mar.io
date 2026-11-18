package fr.julie.mariosprint.ui

import android.view.WindowManager
import android.content.ClipboardManager
import android.content.ClipData
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
import fr.julie.mariosprint.domain.model.FriendCode
import fr.julie.mariosprint.domain.model.FriendCommand
import fr.julie.mariosprint.BuildConfig
import fr.julie.mariosprint.R
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val friendsVm: FriendsViewModel = viewModel(factory = remember(container) {
        factory { FriendsViewModel(container.accounts, container.friends) }
    })
    val observedFriends by friendsVm.state.collectAsStateWithLifecycle()
    val friends = observedFriends.takeIf { it.viewerUid == profile.player?.uid }
        ?: FriendsState(viewerUid = profile.player?.uid)
    val snackbar = remember { SnackbarHostState() }
    val google = remember(container) { GoogleSignInCoordinator(app, container.webClientId) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(friendsVm, lifecycleOwner, profile.player?.uid) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                friendsVm.events.collect { event ->
                    when (event) {
                        is FriendsEvent.IncomingRequests -> if (event.uid == profile.player?.uid) {
                            snackbar.showSnackbar(app.resources.getQuantityString(R.plurals.friends_received_alert, event.count, event.count))
                        }
                    }
                }
            }
            friendsVm.onAction(FriendsAction.VisibilityChanged(true))
            try { awaitCancellation() } finally { friendsVm.onAction(FriendsAction.VisibilityChanged(false)) }
        }
    }
    var artReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { fr.julie.mariosprint.game.rendering.GameArt.heroFrames.size }
        artReady = true
    }
    LaunchedEffect(accountVm, lifecycleOwner, google) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                accountVm.state.map { it.googleSessionClearPending }.distinctUntilChanged().collect { pending ->
                    if (pending) {
                        try { google.clearSession() }
                        catch (error: CancellationException) { throw error }
                        catch (_: Exception) { /* La session Supabase est déjà supprimée localement. */ }
                        // Une annulation lifecycle laisse la demande en attente pour le retour au premier plan.
                        accountVm.onAction(AccountAction.SessionCleared)
                    }
                }
            }
            accountVm.events.collect { event ->
                when (event) {
                    AccountEvent.RequestGoogleSignIn -> launch {
                        google.signIn(activity, { token, nonce -> accountVm.onAction(AccountAction.TokenReceived(token, nonce)) },
                            { accountVm.onAction(AccountAction.SignInFailed(it)) })
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
                    HomeScreen(state.copy(profile = profile, friendRequests = friends.incoming.size)) { action ->
                        when(action) {
                            is HomeAction.SelectLevel -> vm.selectLevel(action.level)
                            HomeAction.Play -> navigation.navigate(Destination.Game(state.selectedLevel.value))
                            HomeAction.Leaderboard -> navigation.navigate(Destination.Leaderboard(state.selectedLevel.value))
                            HomeAction.Collection -> navigation.navigate(Destination.Collection)
                            HomeAction.Profile -> navigation.navigate(Destination.Profile)
                            HomeAction.Help -> navigation.navigate(Destination.Help)
                            HomeAction.Friends -> navigation.navigate(Destination.Friends)
                            is HomeAction.Account -> accountVm.onAction(action.action)
                        }
                    }
                }
                composable<Destination.Profile> {
                    ProfileScreen(profile) { action ->
                        when (action) {
                            ProfileAction.Back -> navigation.popBackStack()
                            ProfileAction.Friends -> navigation.navigate(Destination.Friends)
                            ProfileAction.PublicProfile -> profile.player?.uid?.let { navigation.navigate(Destination.UserProfile(it)) }
                            ProfileAction.About -> navigation.navigate(Destination.About)
                            is ProfileAction.Account -> accountVm.onAction(action.action)
                        }
                    }
                }
                composable<Destination.Help> {
                    HelpScreen { action ->
                        if (action == HelpAction.Back) navigation.popBackStack()
                    }
                }
                composable<Destination.About> {
                    AboutScreen(BuildConfig.VERSION_NAME) { navigation.popBackStack() }
                }
                composable<Destination.Friends> {
                    FriendsScreen(friends) { action ->
                        when (action) {
                            FriendsAction.Back -> navigation.popBackStack()
                            FriendsAction.SignIn -> navigation.navigate(Destination.Profile)
                            FriendsAction.CopyCode -> if (friends.friendCode.isNotBlank()) {
                                app.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                                    ClipData.newPlainText(app.getString(R.string.friends_my_code), FriendCode.format(friends.friendCode)))
                            }
                            is FriendsAction.OpenProfile -> navigation.navigate(Destination.UserProfile(action.uid))
                            else -> friendsVm.onAction(action)
                        }
                    }
                }
                composable<Destination.UserProfile> { entry ->
                    val uid = entry.toRoute<Destination.UserProfile>().uid
                    val vm: UserProfileViewModel = viewModel(factory = remember(container, uid) {
                        factory { UserProfileViewModel(container.accounts, container.friends, uid) }
                    })
                    val observed by vm.state.collectAsStateWithLifecycle()
                    val state = observed.takeIf { it.viewerUid == profile.player?.uid }
                        ?: UserProfileState(viewerUid = profile.player?.uid, playerUid = uid)
                    UserProfileScreen(state) { action ->
                        when (action) {
                            UserProfileAction.Back -> navigation.popBackStack()
                            UserProfileAction.SignIn -> navigation.navigate(Destination.Profile)
                            else -> vm.onAction(action)
                        }
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
                    LeaderboardScreen(state.copy(profile = profile,
                        friendships = friends.relations.associate { it.uid to it.status },
                        friendActionBusy = friends.busy, friendError = friends.error)) { action ->
                        when(action) {
                            LeaderboardAction.Back -> navigation.popBackStack()
                            is LeaderboardAction.Account -> accountVm.onAction(action.action)
                            LeaderboardAction.Refresh -> vm.onAction(action)
                            is LeaderboardAction.OpenProfile -> navigation.navigate(Destination.UserProfile(action.uid))
                            is LeaderboardAction.AddFriend -> friendsVm.onAction(FriendsAction.Execute(FriendCommand.SendToPlayer(action.uid)))
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
