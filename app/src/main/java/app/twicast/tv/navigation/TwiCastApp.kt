package app.twicast.tv.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.SidebarItem
import app.twicast.tv.core.designsystem.component.SidebarLayout
import app.twicast.tv.core.designsystem.theme.TwiMotion
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.User
import app.twicast.tv.feature.auth.LoginRoute
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.Stream
import app.twicast.tv.feature.categories.CategoriesRoute
import app.twicast.tv.feature.categories.CategoryStreamsDestination
import app.twicast.tv.feature.categories.CategoryStreamsRoute
import app.twicast.tv.feature.home.HomeRoute
import app.twicast.tv.feature.player.PlayerDestination
import app.twicast.tv.feature.player.PlayerRoute
import app.twicast.tv.feature.search.SearchRoute
import kotlinx.collections.immutable.toPersistentList

/** Root: sign-in until a session exists (mandatory in the MVP), then the shell with the sidebar. */
@Composable
fun TwiCastApp(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
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
            is AuthState.SignedIn -> SignedInShell(state.user, onSignOut = viewModel::onSignOut)
        }
    }
}

@Composable
private fun SignedInShell(user: User, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = when {
        // Streams of a category belong to the Categories section.
        backStackEntry?.destination?.hasRoute(CategoryStreamsDestination::class) == true -> TopLevelDestination.Categories
        else -> TopLevelDestination.entries.firstOrNull { destination ->
            backStackEntry?.destination?.hasRoute(destination.route::class) == true
        } ?: TopLevelDestination.Home
    }
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
        onItemClick = { item -> navController.navigateToTopLevel(TopLevelDestination.valueOf(item.key)) },
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
                        // Offline channels open too: the player itself says the channel is not live.
                        navController.navigate(
                            PlayerDestination(channel.login, channel.displayName, channel.categoryName, viewerCount = null, channel.avatarUrl),
                        )
                    },
                    onCategoryClick = navController::openCategory,
                )
            }
            composable<CategoriesDestination> { CategoriesRoute(onCategoryClick = navController::openCategory) }
            composable<CategoryStreamsDestination> {
                CategoryStreamsRoute(onBack = navController::popBackStack, onStreamClick = navController::openStream)
            }
            composable<FollowingDestination> { Placeholder(R.string.nav_following, R.string.placeholder_stage_authorized, navController) }
            composable<ProfileDestination> {
                PlaceholderScreen(
                    titleRes = R.string.nav_profile,
                    messageRes = R.string.placeholder_stage_authorized,
                    actionRes = R.string.action_sign_out,
                    onAction = onSignOut,
                )
            }
            composable<PlayerDestination> { PlayerRoute(onClose = navController::popBackStack) }
        }
    }
}

private fun NavHostController.openStream(stream: Stream) {
    navigate(PlayerDestination(stream.userLogin, stream.channelName, stream.categoryName, stream.viewerCount, stream.avatarUrl))
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

@Composable
private fun Placeholder(@StringRes titleRes: Int, @StringRes messageRes: Int, navController: NavHostController) {
    PlaceholderScreen(
        titleRes = titleRes,
        messageRes = messageRes,
        actionRes = R.string.placeholder_go_home,
        onAction = { navController.navigateToTopLevel(TopLevelDestination.Home) },
    )
}
