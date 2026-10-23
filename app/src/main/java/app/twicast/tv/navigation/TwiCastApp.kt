package app.twicast.tv.navigation

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

/** Root: sign-in until a session exists (mandatory in the MVP), then the shell with the sidebar. */
@Composable
fun TwiCastApp(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        AuthGate(authState)
        // Self-update panel over everything, sign-in included.
        UpdateRoute()
    }
}

@Composable
private fun AuthGate(authState: AuthState) {
    // Animate by state type: the shell is not rebuilt when only the user's fields change.
    AnimatedContent(
        targetState = authState,
        contentKey = { it::class },
        transitionSpec = { fadeIn(tween(TwiMotion.ScreenFadeMillis)) togetherWith fadeOut(tween(TwiMotion.ScreenFadeMillis)) },
        modifier = Modifier.fillMaxSize().background(TwiTheme.colors.background),
        label = "auth",
    ) { state ->
        when (state) {
            AuthState.Loading -> Box(Modifier.fillMaxSize())
            AuthState.SignedOut -> LoginRoute()
            is AuthState.SignedIn -> SignedInShell(state.user)
        }
    }
}

@Composable
private fun SignedInShell(user: User) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // The section is the last top-level screen reached: a category, a channel or the player opened from
    // "Подписки" still belong to "Подписки". Followed from the current entry, since the whole back stack
    // is library-internal API.
    var currentDestination by rememberSaveable { mutableStateOf(TopLevelDestination.Home) }
    val entryDestination = backStackEntry?.destination
    LaunchedEffect(entryDestination) { entryDestination?.topLevel()?.let { currentDestination = it } }
    val isPlayer = backStackEntry?.destination?.hasRoute(PlayerDestination::class) == true

    val labels = TopLevelDestination.entries.map { destination ->
        if (destination == TopLevelDestination.Profile) user.displayName else stringResource(destination.labelRes)
    }
    val (mainItems, profileItem) = remember(labels) {
        val items = TopLevelDestination.entries.mapIndexed { index, destination ->
            SidebarItem(destination.name, labels[index], destination.icon)
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
            composable<HomeDestination> { HomeRoute(onStreamClick = navController::openStream) }
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
                    onStreamClick = navController::openStream,
                    onChannelClick = { channel -> navController.navigate(ChannelDestination(channel.login)) },
                )
            }
            composable<ProfileDestination> { ProfileRoute() }
            composable<SettingsDestination> { SettingsRoute() }
            composable<ChannelDestination> {
                ChannelRoute(
                    onWatchLive = { channel -> channel.stream?.let(navController::openStream) },
                    onVideoClick = { channel, video -> navController.navigate(channel.playerDestination(videoId = video.id)) },
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
