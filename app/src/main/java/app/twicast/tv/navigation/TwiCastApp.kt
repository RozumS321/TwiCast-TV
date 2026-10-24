package app.twicast.tv.navigation

import app.twicast.tv.feature.update.UpdateViewModel
import app.twicast.tv.feature.changelog.ChangelogRoute
import app.twicast.tv.feature.changelog.ChangelogDestination
import app.twicast.tv.data.update.AppRelease
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build
import android.content.pm.PackageManager
import android.Manifest
import app.twicast.tv.data.repository.HistoryEntry
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.SidebarItem
import app.twicast.tv.core.designsystem.component.SidebarLayout
import app.twicast.tv.core.designsystem.theme.TwiMotion
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.User
import app.twicast.tv.feature.auth.LoginRoute
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.model.Stream
import app.twicast.tv.feature.categories.CategoriesRoute
import app.twicast.tv.feature.categories.CategoryStreamsDestination
import app.twicast.tv.feature.categories.CategoryStreamsRoute
import app.twicast.tv.feature.channel.ChannelDestination
import app.twicast.tv.feature.channel.ChannelRoute
import app.twicast.tv.feature.following.FollowingRoute
import app.twicast.tv.feature.home.HomeRoute
import app.twicast.tv.feature.player.PlayerDestination
import app.twicast.tv.feature.player.PlayerRoute
import app.twicast.tv.feature.profile.ProfileRoute
import app.twicast.tv.feature.search.SearchRoute
import app.twicast.tv.feature.settings.SettingsDestination
import app.twicast.tv.feature.settings.SettingsRoute
import app.twicast.tv.feature.update.UpdateRoute
import kotlinx.collections.immutable.toPersistentList

/** Root: on the first start a choice (sign in or go on as a guest), then the shell with the sidebar for both. */
@Composable
fun TwiCastApp(
    viewModel: AppViewModel = hiltViewModel(),
    /** A stream to open now (a tapped "в эфире" notification); [onStreamOpened] once it is. */
    openStream: PlayerDestination? = null,
    onStreamOpened: () -> Unit = {},
    /** "Сообщать о начале эфиров" is on: Android 13+ needs the notification permission. */
    askNotificationPermission: Boolean = false,
) {
    // One update offer for the whole app: the dialog at start, and «Обновить до …» in "Что нового".
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val liveFollowedCount by viewModel.liveFollowedCount.collectAsStateWithLifecycle()
    val guestChosen by viewModel.guestChosen.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        AuthGate(
            authState = authState,
            guestChosen = guestChosen,
            onContinueAsGuest = viewModel::onContinueAsGuest,
            liveFollowedCount = liveFollowedCount,
            openStream = openStream,
            onStreamOpened = onStreamOpened,
            askNotificationPermission = askNotificationPermission,
            onShowUpdate = updateViewModel::onShowUpdate,
        )
        // Self-update panel over everything, sign-in included.
        UpdateRoute(updateViewModel)
    }
}

@Composable
private fun AuthGate(
    authState: AuthState,
    guestChosen: Boolean?,
    onContinueAsGuest: () -> Unit,
    liveFollowedCount: Int,
    openStream: PlayerDestination?,
    onStreamOpened: () -> Unit,
    askNotificationPermission: Boolean,
    onShowUpdate: (AppRelease) -> Unit,
) {
    val gate = when {
        authState == AuthState.Loading || (authState == AuthState.SignedOut && guestChosen == null) -> Gate.Loading
        authState is AuthState.SignedIn -> Gate.Shell(authState.user)
        guestChosen == true -> Gate.Shell(user = null)
        else -> Gate.FirstStart
    }
    // Animate by gate type: the shell is not rebuilt when only the user's fields change.
    AnimatedContent(
        targetState = gate,
        contentKey = { (it as? Gate.Shell)?.user?.id ?: it::class },
        transitionSpec = { fadeIn(tween(TwiMotion.ScreenFadeMillis)) togetherWith fadeOut(tween(TwiMotion.ScreenFadeMillis)) },
        modifier = Modifier.fillMaxSize().background(TwiTheme.colors.background),
        label = "auth",
    ) { state ->
        when (state) {
            Gate.Loading -> Box(Modifier.fillMaxSize())
            // First start: sign in from the phone, or «Продолжить без входа».
            Gate.FirstStart -> LoginRoute(onContinueAsGuest = onContinueAsGuest)
            is Gate.Shell -> Shell(state.user, liveFollowedCount, openStream, onStreamOpened, askNotificationPermission, onShowUpdate)
        }
    }
}

private sealed interface Gate {
    data object Loading : Gate
    data object FirstStart : Gate

    /** [user] null: a guest, browsing without a Twitch account. */
    data class Shell(val user: User?) : Gate
}

@Composable
private fun Shell(
    user: User?,
    liveFollowedCount: Int,
    openStream: PlayerDestination?,
    onStreamOpened: () -> Unit,
    askNotificationPermission: Boolean,
    onShowUpdate: (AppRelease) -> Unit,
) {
    val navController = rememberNavController()
    LaunchedEffect(openStream) {
        if (openStream == null) return@LaunchedEffect
        navController.navigate(openStream)
        onStreamOpened()
    }
    NotificationPermission(askNotificationPermission)
    val backStackEntry by navController.currentBackStackEntryAsState()
    // The section is the last top-level screen reached: a category, a channel or the player opened from
    // "Подписки" still belong to "Подписки". Followed from the current entry, since the whole back stack
    // is library-internal API.
    var currentDestination by rememberSaveable { mutableStateOf(TopLevelDestination.Home) }
    val entryDestination = backStackEntry?.destination
    LaunchedEffect(entryDestination) { entryDestination?.topLevel()?.let { currentDestination = it } }
    val isPlayer = backStackEntry?.destination?.hasRoute(PlayerDestination::class) == true

    val labels = TopLevelDestination.entries.map { destination ->
        when {
            destination != TopLevelDestination.Profile -> stringResource(destination.labelRes)
            user != null -> user.displayName
            // A guest: the profile item is the way to sign in.
            else -> stringResource(R.string.nav_sign_in)
        }
    }
    val (mainItems, profileItem) = remember(labels, liveFollowedCount) {
        val items = TopLevelDestination.entries.mapIndexed { index, destination ->
            val badge = if (destination == TopLevelDestination.Following) liveFollowedCount else 0
            SidebarItem(destination.name, labels[index], destination.icon, badge)
        }
        items.filterNot { it.key == TopLevelDestination.Profile.name }.toPersistentList() to
            items.first { it.key == TopLevelDestination.Profile.name }
    }

    SidebarLayout(
        items = mainItems,
        bottomItem = profileItem,
        selectedKey = currentDestination.name,
        onItemClick = { item ->
            val destination = TopLevelDestination.valueOf(item.key)
            // The current section again: back to its root (e.g. from a channel page to the follow list).
            if (destination == currentDestination) {
                navController.popBackStack(destination.route, inclusive = false)
            } else {
                navController.navigateToTopLevel(destination)
            }
        },
        showSidebar = !isPlayer,
    ) {
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            enterTransition = { fadeIn(tween(TwiMotion.ScreenFadeMillis)) },
            exitTransition = { fadeOut(tween(TwiMotion.ScreenFadeMillis)) },
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<HomeDestination> {
                HomeRoute(
                    onSignIn = { navController.navigateToTopLevel(TopLevelDestination.Profile) },
                    onStreamClick = navController::openStream,
                    onRecordingClick = { entry -> navController.navigate(entry.playerDestination()) },
                    onChannelClick = { login -> navController.navigate(ChannelDestination(login)) },
                )
            }
            composable<SearchDestination> {
                SearchRoute(
                    onChannelClick = { channel ->
                        // Live: straight to the stream; offline: the channel page with its recordings.
                        if (channel.isLive) {
                            navController.navigate(
                                PlayerDestination(channel.login, channel.displayName, channel.categoryName, viewerCount = null, channel.avatarUrl),
                            )
                        } else {
                            navController.navigate(ChannelDestination(channel.login))
                        }
                    },
                    onCategoryClick = navController::openCategory,
                )
            }
            composable<CategoriesDestination> { CategoriesRoute(onCategoryClick = navController::openCategory) }
            composable<CategoryStreamsDestination> {
                CategoryStreamsRoute(onBack = navController::popBackStack, onStreamClick = navController::openStream)
            }
            composable<FollowingDestination> {
                FollowingRoute(
                    onSignIn = { navController.navigateToTopLevel(TopLevelDestination.Profile) },
                    onStreamClick = navController::openStream,
                    onChannelClick = { channel -> navController.navigate(ChannelDestination(channel.login)) },
                )
            }
            // A guest signs in here, the sidebar staying; once signed in the whole shell switches to the account.
            composable<ProfileDestination> { if (user == null) LoginRoute() else ProfileRoute() }
            composable<SettingsDestination> { SettingsRoute() }
            composable<ChangelogDestination> { ChangelogRoute(onUpdate = onShowUpdate) }
            composable<ChannelDestination> {
                ChannelRoute(
                    onWatchLive = { channel -> channel.stream?.let(navController::openStream) },
                    onVideoClick = { channel, video ->
                        navController.navigate(
                            channel.playerDestination(videoId = video.id).copy(title = video.title, thumbnailTemplate = video.thumbnailTemplate),
                        )
                    },
                    onClipClick = { channel, clip -> navController.navigate(channel.playerDestination(clipSlug = clip.id)) },
                )
            }
            composable<PlayerDestination> {
                PlayerRoute(onClose = navController::popBackStack, onOpenChannel = navController::openChannel)
            }
        }
    }
}

private fun NavDestination.topLevel(): TopLevelDestination? = TopLevelDestination.entries.firstOrNull { hasRoute(it.route::class) }

private fun NavHostController.openStream(stream: Stream) {
    navigate(PlayerDestination(stream.userLogin, stream.channelName, stream.categoryName, stream.viewerCount, stream.avatarUrl))
}

/** A recording from "Недавно смотрели": opened as it was last time (the player resumes where it stopped). */
private fun HistoryEntry.playerDestination() = PlayerDestination(
    channelLogin = channelLogin,
    channelName = channelName,
    categoryName = categoryName,
    viewerCount = null,
    avatarUrl = avatarUrl,
    videoId = videoId,
    title = title,
    thumbnailTemplate = thumbnailTemplate,
)

/** A recording or clip of [this] channel. */
private fun ChannelPage.playerDestination(videoId: String? = null, clipSlug: String? = null) = PlayerDestination(
    channelLogin = login,
    channelName = displayName,
    categoryName = stream?.categoryName.orEmpty(),
    viewerCount = null,
    avatarUrl = avatarUrl,
    videoId = videoId,
    clipSlug = clipSlug,
)

/** From the player: back to the channel page if the stream was opened from it, so Back does not loop. */
private fun NavHostController.openChannel(login: String) {
    val previous = previousBackStackEntry
    if (previous?.destination?.hasRoute(ChannelDestination::class) == true && previous.toRoute<ChannelDestination>().channelLogin == login) {
        popBackStack()
    } else {
        navigate(ChannelDestination(login))
    }
}

private fun NavHostController.openCategory(category: Category) {
    navigate(CategoryStreamsDestination(category.id, category.name, category.boxArtTemplate))
}

/** Switches sections without growing the back stack; each section keeps its state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Android 13+ asks once whether TwiCast may notify; the system stops asking after the viewer declines twice. */
@Composable
private fun NotificationPermission(wanted: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(wanted) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (wanted && !granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
