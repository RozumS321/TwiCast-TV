package app.twicast.tv.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.feature.settings.SettingsDestination
import kotlinx.serialization.Serializable

@Serializable data object SearchDestination

@Serializable data object HomeDestination

@Serializable data object CategoriesDestination

@Serializable data object FollowingDestination

@Serializable data object ProfileDestination

/** Sidebar sections in display order; Profile is pinned to the bottom of the sidebar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Search(SearchDestination, R.string.nav_search, TwiIcons.Search),
    Home(HomeDestination, R.string.nav_home, TwiIcons.Home),
    Categories(CategoriesDestination, R.string.nav_categories, TwiIcons.Categories),
    Following(FollowingDestination, R.string.nav_following, TwiIcons.Following),
    Settings(SettingsDestination, R.string.settings_title, TwiIcons.Settings),
    Profile(ProfileDestination, R.string.nav_profile, TwiIcons.Profile),
}
